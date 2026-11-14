package org.sjhstudio.diary.core.data.diary

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import okio.Path
import org.sjhstudio.diary.core.database.EntryDao
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.EntryListRow
import org.sjhstudio.diary.core.database.MediaDao
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.asListRow
import org.sjhstudio.diary.core.database.localDate
import org.sjhstudio.diary.core.database.localTime
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

/**
 * 목록 미리보기에 넣는 최대 글자
 * 3줄을 넘지 않고, 긴 일기를 메모리에 다 올리지 않음
 */
private const val PREVIEW_CHARS = 240

/** 찾은 글자가 뒤에 있을 때 앞에 남기는 글자 */
private const val PREVIEW_LEAD = 12

/** `IN` 조건 한 번에 넘길 일기 수 */
private const val MEDIA_CHUNK = 500

/** 사진 상태가 바뀐 뒤 목록을 다시 읽기까지 묶는 시간 */
internal val PHOTO_REFRESH_EVERY: Duration = 3.seconds

internal fun EntryEntity.toRow(media: List<MediaEntity>, mediaDir: Path, query: String = ""): EntryRow =
    asListRow().toRow(media, mediaDir, query)

internal fun EntryListRow.toRow(media: List<MediaEntity>, mediaDir: Path, query: String = ""): EntryRow = EntryRow(
    id = id,
    date = localDate,
    time = localTime,
    preview = Checklist.forPreview(preview(plainText, query)),
    mood = Mood.fromId(mood),
    weather = Weather.fromId(weather),
    placeName = placeName,
    isFavorite = isFavorite,
    photos = media.map { if (it.status == MediaStatus.PENDING.id) EntryRow.PHOTO_PENDING else it.pathIn(mediaDir) }
)

private val WHITESPACE = Regex("\\s+")
private const val SLICE_MARGIN = 40

/** 찾은 글자가 잘리지 않게 그 앞에서부터 보여 줌(FR-025) */
internal fun preview(source: String, query: String): String {
    val found = if (query.isEmpty()) -1 else source.indexOf(query, ignoreCase = true)
    // 긴 일기 전체를 다듬지 않게 보일 곳 근처만 잘라 냄
    // 1만 편 찾기에서 이 비용이 가장 컸음
    val from = (found - PREVIEW_LEAD - SLICE_MARGIN).coerceAtLeast(0)
    val slice = source.substring(from, (from + EntryDao.LIST_TEXT_CHARS).coerceAtMost(source.length))
    // 빈 줄이 미리보기 세 줄을 차지하지 않게 줄바꿈을 한 칸으로 줄임
    val text = slice.replace(WHITESPACE, " ").trim()
    val at = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (from == 0 && at < PREVIEW_CHARS - query.length - PREVIEW_LEAD && at <= 60) return text.safeTake(PREVIEW_CHARS)
    return "…" + text.substring((at - PREVIEW_LEAD).coerceAtLeast(0)).safeTake(PREVIEW_CHARS)
}

/** 이모지를 반으로 자르지 않음 */
private fun String.safeTake(count: Int): String {
    if (length <= count) return this
    return if (this[count - 1].isHighSurrogate()) take(count - 1) else take(count)
}

internal fun MediaEntity.pathIn(mediaDir: Path): String? =
    if (status == MediaStatus.NORMAL.id) fileName?.let { (mediaDir / it).toString() } else null

/** 일기 묶음의 사진을 한 번에 읽음 */
internal suspend fun MediaDao.forRows(entryIds: List<String>): Map<String, List<MediaEntity>> =
    entryIds.chunked(MEDIA_CHUNK).flatMap { forEntries(it) }.groupBy { it.entryId }

/**
 * 사진 상태가 바뀌었음을 알림
 * 복원은 사진을 한 장씩 받아, 장마다 알리면 목록을 수천 번 다시 읽음
 * 그래서 [every]에 한 번으로 묶고, 묶는 사이에 온 변화는 마지막 한 번으로 남겨 놓치지 않음
 * 일정한 시계를 돌리지 않아 바뀌는 게 없으면 아무 일도 하지 않음
 */
internal fun MediaDao.photoStatusChanges(every: Duration, onBaseline: () -> Unit = {}): Flow<Unit> =
    observeUnsettled().changesSinceFirst(onBaseline).conflate().onEach { delay(every) }.map { }

/**
 * 첫 값 뒤에 바뀐 값만. 첫 값은 보는 쪽이 이미 아는 기준
 * 첫 값을 받기 전에 읽기가 실패했으면 기준이 없으니 다시 붙은 뒤의 첫 값도 냄
 * 실패하면 잠시 뒤 다시 붙고([rejoining]), 다시 붙어도 같은 값은 내지 않음
 */
internal fun <T> Flow<T>.changesSinceFirst(onBaseline: () -> Unit = {}): Flow<T> = flow {
    var failedEarly = false
    var seen = false
    rejoining {
        if (!seen) {
            failedEarly = true
            onBaseline()
        }
    }.distinctUntilChanged().collect { value ->
        if (seen || failedEarly) emit(value)
        if (!seen) {
            seen = true
            onBaseline()
        }
    }
}

/**
 * 감시를 띄우고, 기준을 잡으면(첫 값이나 첫 실패) 끝나는 값을 돌려줌. 그 뒤 바뀔 때마다 [onChange]
 * 목록은 기준을 다 잡은 뒤에 시작해야 시작하는 사이의 변화를 놓치지 않음
 */
internal fun CoroutineScope.watchFromBaseline(changes: (onBaseline: () -> Unit) -> Flow<*>, onChange: () -> Unit): Deferred<Unit> {
    val baseline = CompletableDeferred<Unit>()
    launch { changes { baseline.complete(Unit) }.collect { onChange() } }
    return baseline
}

/**
 * 읽기에 실패하면 잠시 뒤 다시 붙음. 한 번 실패로 감시가 끝나면 그 목록을 다시 열 때까지 바뀐 것을 모름
 * 쿼리 바로 뒤에 붙여, 다시 붙어도 뒤의 중복 거르기가 같은 값을 거름
 * 목록은 화면 스레드에서 모아 예외가 새면 앱이 꺼짐
 */
internal fun <T> Flow<T>.rejoining(onFailure: () -> Unit = {}): Flow<T> = retryWhen { cause, _ ->
    (cause is Exception).also { retry ->
        if (retry) {
            onFailure()
            delay(REJOIN_AFTER)
        }
    }
}

/**
 * 첫 값까지의 실패는 그대로 내 화면이 [다시 시도]를 보이고, 그 뒤의 실패는 잠시 뒤 조용히 다시 붙음
 * 보던 화면이 한 번 실패로 멈춰 그 뒤 고친 것이 안 보이는 일이 없게
 */
internal fun <T> Flow<T>.rejoiningAfterFirst(): Flow<T> = flow {
    var seen = false
    emitAll(
        onEach { seen = true }.retryWhen { cause, _ ->
            (seen && cause is Exception).also { retry -> if (retry) delay(REJOIN_AFTER) }
        }
    )
}

private val REJOIN_AFTER = 5.seconds
