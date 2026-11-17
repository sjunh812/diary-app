package org.sjhstudio.diary.core.data.diary

import org.sjhstudio.diary.core.database.EntryDao
import org.sjhstudio.diary.core.database.EntryTextRow
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.richtext.PreviewText

// 목록 미리보기 글 읽기와 찾기 미리보기(스펙 002 FR-025, 스펙 008)

/** 찾기 미리보기에 넣는 최대 글자. 3줄을 넘지 않음 */
private const val PREVIEW_CHARS = PreviewText.THREE_LINES_CHARS

/**
 * 서식 보기에서 읽는 본문 HTML 앞부분
 * 태그가 글보다 길어 미리보기 글자 수보다 넉넉히 읽음
 */
internal const val THREE_LINES_HTML_CHARS = 4000
internal const val FULL_HTML_CHARS = 12000

/** 찾은 글자가 뒤에 있을 때 앞에 남기는 글자 */
private const val PREVIEW_LEAD = 12

/** 미리보기를 만들 때 다듬는 앞부분 길이. 3줄은 목록 쿼리와 같음 */
internal fun previewInputChars(length: PreviewLength): Int =
    if (length == PreviewLength.FULL) PreviewText.FULL_CHARS else EntryDao.LIST_TEXT_CHARS

/** 이모지를 반으로 자르지 않는 앞부분 */
internal fun String.safePrefix(count: Int): String = if (length <= count) this else safeTake(count)

/** 서식 보기에서 읽는 본문 HTML 길이 */
internal fun previewHtmlChars(length: PreviewLength): Int = if (length == PreviewLength.FULL) FULL_HTML_CHARS else THREE_LINES_HTML_CHARS

/** 앞부분만 읽어 끝에 잘린 태그(`<…`)나 문자 참조(`&am`)가 남으면 버림 */
internal fun String.withoutCutTag(): String {
    val open = lastIndexOf('<')
    val tagless = if (open > lastIndexOf('>')) substring(0, open) else this
    return CUT_ENTITY.find(tagless)?.let { tagless.substring(0, it.range.first) } ?: tagless
}

private val CUT_ENTITY = Regex("&#?[A-Za-z0-9]{0,10}$")

/** 일기 묶음의 미리보기 글을 한 번에 읽음 */
internal suspend fun EntryDao.previewTextsOf(
    ids: List<String>,
    read: suspend EntryDao.(List<String>) -> List<EntryTextRow>
): Map<String, String> = ids.chunked(MEDIA_CHUNK).flatMap { read(it) }.associate { it.id to it.text }

// 줄 구분, 전각 띄어쓰기 같은 글자도 찾기 미리보기에서는 한 칸으로
private val WHITESPACE = Regex("[\\s\u00A0\u3000\u2028\u2029\u0085]+")
private const val SLICE_MARGIN = 40

/** 찾은 글자가 잘리지 않게 그 앞에서부터 보여 줌(FR-025) */
internal fun preview(source: String, query: String): String {
    val found = if (query.isEmpty()) -1 else source.indexOf(query, ignoreCase = true)
    // 긴 일기 전체를 다듬지 않게 보일 곳 근처만 잘라 냄
    // 1만 편 찾기에서 이 비용이 가장 컸음
    val from = source.charStart((found - PREVIEW_LEAD - SLICE_MARGIN).coerceAtLeast(0))
    val slice = source.substring(from, source.charStart((from + EntryDao.LIST_TEXT_CHARS).coerceAtMost(source.length)))
    // 찾은 글자가 첫 줄 가까이 오게 줄바꿈을 한 칸으로 줄임
    val text = slice.replace(WHITESPACE, " ").trim()
    val at = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (from == 0 && at < PREVIEW_CHARS - query.length - PREVIEW_LEAD && at <= 60) return text.safeTake(PREVIEW_CHARS)
    return "…" + text.substring(text.charStart((at - PREVIEW_LEAD).coerceAtLeast(0))).safeTake(PREVIEW_CHARS)
}

/** 이모지를 반으로 자르지 않음 */
private fun String.safeTake(count: Int): String {
    if (length <= count) return this
    return take(charStart(count))
}

/** 이모지 한가운데면 그 글자 앞으로 */
private fun String.charStart(index: Int): Int =
    if (index in 1 until length && this[index].isLowSurrogate() && this[index - 1].isHighSurrogate()) index - 1 else index
