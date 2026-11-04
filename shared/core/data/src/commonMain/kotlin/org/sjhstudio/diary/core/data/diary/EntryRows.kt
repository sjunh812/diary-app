package org.sjhstudio.diary.core.data.diary

import okio.Path
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaDao
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus

/** 목록 미리보기에 넣는 최대 글자. 3줄을 넘지 않고, 긴 일기를 메모리에 다 올리지 않는다. */
private const val PREVIEW_CHARS = 240

/** 찾은 글자가 뒤에 있을 때 앞에 남기는 글자. */
private const val PREVIEW_LEAD = 12

/** `IN` 조건 한 번에 넘길 일기 수. */
private const val MEDIA_CHUNK = 500

internal fun EntryEntity.toRow(media: List<MediaEntity>, mediaDir: Path, query: String = ""): EntryRow {
    val model = asModel()
    return EntryRow(
        id = id,
        date = model.date,
        time = model.time,
        preview = preview(plainText, query),
        mood = model.mood,
        weather = model.weather,
        placeName = placeName,
        isFavorite = isFavorite,
        photos = media.map { it.pathIn(mediaDir) }
    )
}

private val WHITESPACE = Regex("\\s+")

/** 찾은 글자가 잘리지 않게 그 앞에서부터 보여 준다(FR-025). */
internal fun preview(source: String, query: String): String {
    // 빈 줄이 미리보기 세 줄을 차지하지 않게 줄바꿈을 한 칸으로 줄인다.
    val text = source.replace(WHITESPACE, " ").trim()
    val at = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (at < PREVIEW_CHARS - query.length - PREVIEW_LEAD && at <= 60) return text.safeTake(PREVIEW_CHARS)
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
internal suspend fun MediaDao.forRows(entries: List<EntryEntity>): Map<String, List<MediaEntity>> =
    entries.map { it.id }.chunked(MEDIA_CHUNK).flatMap { forEntries(it) }.groupBy { it.entryId }
