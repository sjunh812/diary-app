package org.sjhstudio.diary.core.data.diary

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
 * 목록 미리보기에 넣는 최대 글자.
 * 3줄을 넘지 않고, 긴 일기를 메모리에 다 올리지 않는다.
 */
private const val PREVIEW_CHARS = 240

/** 찾은 글자가 뒤에 있을 때 앞에 남기는 글자. */
private const val PREVIEW_LEAD = 12

/** `IN` 조건 한 번에 넘길 일기 수. */
private const val MEDIA_CHUNK = 500

/** 사진 상태가 바뀐 뒤 목록을 다시 읽기까지 묶는 시간. */
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

/** 찾은 글자가 잘리지 않게 그 앞에서부터 보여 준다(FR-025). */
internal fun preview(source: String, query: String): String {
    val found = if (query.isEmpty()) -1 else source.indexOf(query, ignoreCase = true)
    // 긴 일기 전체를 다듬지 않게 보일 곳 근처만 잘라 낸다.
    // 1만 편 찾기에서 이 비용이 가장 컸다.
    val from = (found - PREVIEW_LEAD - SLICE_MARGIN).coerceAtLeast(0)
    val slice = source.substring(from, (from + EntryDao.LIST_TEXT_CHARS).coerceAtMost(source.length))
    // 빈 줄이 미리보기 세 줄을 차지하지 않게 줄바꿈을 한 칸으로 줄인다.
    val text = slice.replace(WHITESPACE, " ").trim()
    val at = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (from == 0 && at < PREVIEW_CHARS - query.length - PREVIEW_LEAD && at <= 60) return text.safeTake(PREVIEW_CHARS)
    return "…" + text.substring((at - PREVIEW_LEAD).coerceAtLeast(0)).safeTake(PREVIEW_CHARS)
}

/** 이모지를 반으로 자르지 않는다. */
private fun String.safeTake(count: Int): String {
    if (length <= count) return this
    return if (this[count - 1].isHighSurrogate()) take(count - 1) else take(count)
}

internal fun MediaEntity.pathIn(mediaDir: Path): String? =
    if (status == MediaStatus.NORMAL.id) fileName?.let { (mediaDir / it).toString() } else null

/** 일기 묶음의 사진을 한 번에 읽는다. */
internal suspend fun MediaDao.forRows(entryIds: List<String>): Map<String, List<MediaEntity>> =
    entryIds.chunked(MEDIA_CHUNK).flatMap { forEntries(it) }.groupBy { it.entryId }

/**
 * 사진 상태가 바뀌었음을 알린다.
 * 복원은 사진을 한 장씩 받아서, 장마다 알리면 수천 번 목록을 다시 읽게 된다.
 * 그래서 [every]에 한 번으로 묶고, 묶는 사이에 온 변화는 마지막 한 번으로 남겨 놓치지 않는다.
 * 일정한 시계를 돌리지 않으니 바뀌는 게 없으면 아무 일도 하지 않는다.
 */
internal fun MediaDao.photoStatusChanges(every: Duration): Flow<Unit> =
    observeUnsettled().distinctUntilChanged().drop(1).conflate().onEach { delay(every) }.map { }
