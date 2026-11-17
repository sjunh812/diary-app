package org.sjhstudio.diary.core.model.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.PreviewLength.FIVE_LINES
import org.sjhstudio.diary.core.model.PreviewLength.FULL
import org.sjhstudio.diary.core.model.PreviewLength.THREE_LINES
import org.sjhstudio.diary.core.model.RichSpan
import org.sjhstudio.diary.core.model.RichStyle
import org.sjhstudio.diary.core.model.RichText
import org.sjhstudio.diary.core.model.parseRichText

class PreviewTextTest {
    @Test
    fun keepsLineBreaks() {
        assertEquals("a\nb\nc\nd", PreviewText.shape("a\nb\nc\nd", THREE_LINES))
        assertEquals("a\nb\nc\nd", PreviewText.shape("a\nb\nc\nd", FULL))
    }

    @Test
    fun threeLinesDropsBlankLinesAndFullKeepsOne() {
        assertEquals("a\nb", PreviewText.shape("a\n\n\nb", THREE_LINES))
        assertEquals("a\n\nb", PreviewText.shape("a\n\n\nb", FULL))
        assertEquals("a\nb", PreviewText.shape("a\n   \n\tb", THREE_LINES))
    }

    @Test
    fun specialLineBreaksBecomeLineBreaks() {
        for (separator in listOf("\u2028", "\u2029", "\u0085", "\u000B", "\u000C", "\r\n", "\r")) {
            assertEquals("a\nb", PreviewText.shape("a${separator}b", THREE_LINES), "U+${separator.map { it.code.toString(16) }}")
        }
    }

    @Test
    fun tidiesSpacesAndEdges() {
        assertEquals("a b", PreviewText.shape("  a  b \n\n", THREE_LINES))
        // 전각·줄 바꾸지 않는 띄어쓰기만 있는 줄도 빈 줄
        assertEquals("첫째\n둘째\n셋째", PreviewText.shape("첫째\n\u3000\n둘째\n\u00A0\n셋째", THREE_LINES))
        assertEquals("a b", PreviewText.shape("a\u3000\u3000b", THREE_LINES))
        assertEquals("a b", PreviewText.shape("\n\n  a \t b", FULL))
        assertEquals("", PreviewText.shape("\n\n", FULL))
        assertEquals("", PreviewText.shape("", THREE_LINES))
    }

    @Test
    fun scheduleKeepsEveryLine() {
        val schedule = "09:00 병원 예약\n11:00 은행 들르기\n14:00 팀 회의\n16:30 택배 보내기"
        assertEquals(schedule, PreviewText.shape(schedule, THREE_LINES))
    }

    @Test
    fun cutsAtLimitWithoutSplittingEmoji() {
        val long = "가".repeat(PreviewText.THREE_LINES_CHARS - 1) + "😀"
        val shaped = PreviewText.shape(long, THREE_LINES)
        assertEquals("가".repeat(PreviewText.THREE_LINES_CHARS - 1) + PreviewText.ELLIPSIS, shaped)
        assertEquals(PreviewText.FIVE_LINES_CHARS + 1, PreviewText.shape("가".repeat(PreviewText.FIVE_LINES_CHARS + 10), FIVE_LINES).length)
    }

    /** 전체는 끝까지. 아주 긴 일기도 자르지 않고 줄임표도 없음 */
    @Test
    fun fullShowsEverything() {
        val long = (1..400).joinToString("\n") { "줄 $it 일정" } + "가".repeat(20_000)
        val shaped = PreviewText.shape(RichText(long, emptyList()), FULL)
        assertEquals(long, shaped.text)
        kotlin.test.assertFalse(shaped.cut)
        assertEquals(PreviewText.UNLIMITED, PreviewText.lines(FULL))
    }

    /** 5줄은 3줄처럼 빈 줄을 없앰 */
    @Test
    fun fiveLinesDropsBlankLines() {
        assertEquals("a\nb", PreviewText.shape("a\n\n\nb", FIVE_LINES))
        assertEquals(5, PreviewText.lines(FIVE_LINES))
    }

    /** 앞부분만 읽은 원문은 다 보여도 더 있다고 알림 */
    @Test
    fun truncatedSourceIsCut() {
        val shaped = PreviewText.shape(RichText("짧은 앞부분", emptyList()), THREE_LINES, truncated = true)
        assertEquals("짧은 앞부분" + PreviewText.ELLIPSIS, shaped.text)
        kotlin.test.assertTrue(shaped.cut)
        kotlin.test.assertFalse(PreviewText.shape(RichText("짧은 글", emptyList()), THREE_LINES).cut)
    }

    @Test
    fun movesSpansPastRemovedText() {
        // "  굵게\n\n\n보통" → "굵게\n보통"
        val source = RichText("  굵게\n\n\n보통", listOf(RichSpan(2, 4, RichStyle.Bold), RichSpan(7, 9, RichStyle.Underline)))
        val shaped = PreviewText.shape(source, THREE_LINES)
        assertEquals("굵게\n보통", shaped.text)
        assertEquals(listOf(RichSpan(0, 2, RichStyle.Bold), RichSpan(3, 5, RichStyle.Underline)), shaped.spans)
    }

    @Test
    fun dropsSpansThatBecomeEmpty() {
        val source = RichText("a\n\n\nb", listOf(RichSpan(1, 3, RichStyle.Bold)))
        assertEquals(emptyList(), PreviewText.shape(source, THREE_LINES).spans)
    }

    @Test
    fun clampsSpansAtCut() {
        val text = "가".repeat(PreviewText.THREE_LINES_CHARS + 20)
        val source = RichText(text, listOf(RichSpan(PreviewText.THREE_LINES_CHARS - 5, PreviewText.THREE_LINES_CHARS + 10, RichStyle.Bold)))
        val span = PreviewText.shape(source, THREE_LINES).spans.single()
        assertEquals(PreviewText.THREE_LINES_CHARS - 5, span.start)
        assertEquals(PreviewText.THREE_LINES_CHARS, span.end)
    }

    /** 앞부분만 읽은 HTML은 닫는 태그가 없어도 서식이 글 끝까지 이어짐 */
    @Test
    fun cut_html_keeps_open_spans() {
        val parsed = parseRichText("<p><b>" + "가".repeat(10))
        assertEquals(listOf(RichSpan(0, 10, RichStyle.Bold)), parsed.spans)
    }
}
