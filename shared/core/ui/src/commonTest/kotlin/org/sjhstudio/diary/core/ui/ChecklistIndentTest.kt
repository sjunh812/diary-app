package org.sjhstudio.diary.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.Checklist

class ChecklistIndentTest {
    private val indent = ChecklistIndent(width = 20.sp, doneSpacing = (-4).sp)
    private val open = Checklist.OPEN
    private val done = Checklist.DONE

    /** 읽기 글은 체크리스트 줄에만 둘째 줄 들여쓰기를 둠. 글은 그대로 */
    @Test
    fun reading_text_indents_only_checklist_lines() {
        val text = AnnotatedString("오늘\n$open 우유\n$done 달걀")

        val shown = text.withChecklistIndent(indent)

        // 글자 수와 자리는 그대로. 문단 끝 줄바꿈만 공백으로 바뀌어 빈 줄이 생기지 않음
        assertEquals(text.text.replace('\n', ' '), shown.text)
        assertEquals(
            listOf(null, TextIndent(restLine = 20.sp), TextIndent(restLine = 20.sp)),
            shown.paragraphStyles.map { it.item.textIndent }
        )
        // 끝낸 표시만 자간을 줄여 빈 표시와 폭을 맞춤
        val doneAt = text.text.indexOf(done)
        assertEquals(listOf(doneAt to (-4).sp), shown.spanStyles.map { it.start to it.item.letterSpacing })
        assertEquals(listOf("오늘\n", "$open 우유\n", "$done 달걀"), shown.paragraphStyles.map { text.text.substring(it.start, it.end) })
    }

    /** 체크리스트가 없는 글은 손대지 않음 */
    @Test
    fun text_without_checklist_is_unchanged() {
        val text = AnnotatedString("오늘\n\n내일")

        assertEquals(text, text.withChecklistIndent(indent))
    }

    /** 편집기 글은 문단 스타일을 바꾸지 않고 들여쓰기만 더함. 목록 문단은 그대로 */
    @Test
    fun editor_paragraphs_keep_their_own_style() {
        val list = ParagraphStyle(textIndent = TextIndent(firstLine = 8.sp, restLine = 16.sp))
        val text =
            buildAnnotatedString {
                withStyle(list) { append("• 하나 ") }
                withStyle(ParagraphStyle(lineHeight = 28.sp)) { append("$open 우유") }
            }

        val styles = text.withChecklistIndent(indent).paragraphStyles.map { it.item }

        assertEquals(list, styles[0])
        assertEquals(TextIndent(firstLine = 0.sp, restLine = 20.sp), styles[1].textIndent)
        assertEquals(28.sp, styles[1].lineHeight)
    }

    /** 첫 줄 들여쓰기가 있는 문단은 둘째 줄도 그만큼 더 들여 씀 */
    @Test
    fun indented_paragraphs_keep_the_second_line_under_the_text() {
        val text = buildAnnotatedString { withStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 8.sp))) { append("$open 우유") } }

        assertEquals(
            TextIndent(firstLine = 8.sp, restLine = 28.sp),
            text.withChecklistIndent(indent).paragraphStyles.single().item.textIndent
        )
    }

    /** 여러 줄 형광펜과 밑줄은 문단 끝 공백에서 끊겨 줄 끝까지 그려지지 않음. 굵게 같은 꾸밈은 그대로 */
    @Test
    fun marks_skip_the_paragraph_ends() {
        val text =
            buildAnnotatedString {
                append("$open 우유\n달걀")
                addStyle(SpanStyle(background = Color.Yellow), 0, length)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, length)
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), 0, length)
            }
        val end = text.text.indexOf('\n')

        val styles = text.withChecklistIndent(indent).spanStyles

        assertEquals(
            listOf(0 to end, end + 1 to text.length),
            styles.filter {
                it.item.background == Color.Yellow
            }.map { it.start to it.end }
        )
        assertEquals(listOf(0 to end, end + 1 to text.length), styles.filter { it.item.textDecoration != null }.map { it.start to it.end })
        assertEquals(listOf(0 to text.length), styles.filter { it.item.fontWeight == FontWeight.Bold }.map { it.start to it.end })
    }
}
