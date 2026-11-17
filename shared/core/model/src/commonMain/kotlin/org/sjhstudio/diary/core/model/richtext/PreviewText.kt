package org.sjhstudio.diary.core.model.richtext

import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.RichSpan
import org.sjhstudio.diary.core.model.RichText

/**
 * 목록 미리보기 글 만들기(스펙 008 contracts/preview-text.md)
 * 쓴 줄바꿈은 지키고, 빈 줄은 3줄에서 없애고 전체에서는 연달아 있으면 하나로 줄임
 * 서식 구간은 지운 글자만큼 옮김
 */
object PreviewText {
    const val THREE_LINES = 3

    /** 전체 보기 상한. 측정으로 정하기 전 출발점(research R4) */
    const val FULL_LINES = 8

    /** 3줄 미리보기 글자 수. 넓은 화면 세 줄을 채우는 양 */
    const val THREE_LINES_CHARS = 240

    /** 전체 보기에서 읽는 글자 수. 넓은 화면에서 상한 줄을 채우는 양 */
    const val FULL_CHARS = 1600

    fun lines(length: PreviewLength): Int = if (length == PreviewLength.FULL) FULL_LINES else THREE_LINES

    fun chars(length: PreviewLength): Int = if (length == PreviewLength.FULL) FULL_CHARS else THREE_LINES_CHARS

    /** 잘렸을 때 글 끝에 붙임. 줄 상한 안에 다 들어가는 넓은 화면에서도 더 있다는 것이 보이게 */
    const val ELLIPSIS = "\u2026"

    fun shape(text: String, length: PreviewLength): String = shape(RichText(text, emptyList()), length).text

    /**
     * [truncated]는 원문을 앞부분만 읽었는지
     * 잘렸으면 글 끝에 [ELLIPSIS]를 붙이고 [Shaped.cut]이 참
     */
    fun shape(source: RichText, length: PreviewLength, truncated: Boolean = false): Shaped {
        val text = source.text
        val limit = chars(length)
        val (lines, stopped) = splitLines(text, limit)
        val tidied = lines.map { tidy(text, it) }
        val kept = keepLines(tidied.map { it to it.isEmpty() }, length)

        val out = StringBuilder()
        val origin = ArrayList<Int>()
        kept.forEachIndexed { index, indices ->
            if (index > 0) {
                out.append('\n')
                // 앞 줄 끝 바로 뒤. 다음 줄 서식이 줄바꿈까지 넓어지지 않음
                origin += (origin.lastOrNull() ?: -1) + 1
            }
            for (at in indices) {
                out.append(if (isSpace(text[at])) ' ' else text[at])
                origin += at
            }
        }
        val end = cut(out, limit)
        val shaped = out.substring(0, end)
        val spans = moveSpans(source.spans, origin.subList(0, end), shaped)
        val cut = shaped.isNotEmpty() && (stopped || truncated || end < out.length)
        return Shaped(if (cut) shaped + ELLIPSIS else shaped, spans, cut)
    }

    /** 목록 미리보기 글. [cut]이면 원문이 더 있음 */
    data class Shaped(val text: String, val spans: List<RichSpan>, val cut: Boolean)

    private fun keepLines(lines: List<Pair<List<Int>, Boolean>>, length: PreviewLength): List<List<Int>> {
        val kept = ArrayList<List<Int>>()
        var lastBlank = true
        for ((indices, blank) in lines) {
            // 3줄은 빈 줄을 모두 빼고, 전체는 연달은 빈 줄 가운데 첫 줄만 남김
            if (!blank || (length == PreviewLength.FULL && !lastBlank)) {
                kept += indices
                lastBlank = blank
            }
        }
        while (kept.isNotEmpty() && kept.last().isEmpty()) kept.removeAt(kept.lastIndex)
        return kept
    }

    /** 이모지를 반으로 자르지 않음 */
    private fun cut(text: CharSequence, count: Int): Int {
        if (text.length <= count) return text.length
        return if (text[count - 1].isHighSurrogate()) count - 1 else count
    }

    private fun moveSpans(spans: List<RichSpan>, origin: List<Int>, text: String): List<RichSpan> {
        if (spans.isEmpty() || text.isEmpty()) return emptyList()
        return spans.mapNotNull { span ->
            val start = firstAtOrAfter(origin, span.start)
            val end = firstAtOrAfter(origin, span.end)
            // 지운 빈 줄에만 걸린 서식은 남은 줄바꿈 하나에 남지 않게 버림
            if (start < end && (start until end).any { text[it] != '\n' }) RichSpan(start, end, span.style) else null
        }
    }

    // origin은 오름차순
    private fun firstAtOrAfter(origin: List<Int>, position: Int): Int {
        var low = 0
        var high = origin.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (origin[mid] < position) low = mid + 1 else high = mid
        }
        return low
    }
}
