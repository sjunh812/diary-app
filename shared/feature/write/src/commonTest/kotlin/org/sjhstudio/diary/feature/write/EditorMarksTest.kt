package org.sjhstudio.diary.feature.write

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// 형광펜은 글 뒤에 따로 그려, 고르거나 칠해도 그릴 글이 바뀌지 않음
class EditorMarksTest {
    private val yellow = Color(0xFFFFF3B0)
    private val green = Color(0xFFB7F5C9)

    private fun highlighted(): RichTextState = RichTextState().apply {
        setText("alpha beta gamma")
        selection = TextRange(0, 10)
        addSpanStyle(SpanStyle(background = yellow))
        selection = TextRange(16)
    }

    @Test
    fun selecting_over_a_mark_keeps_the_text_and_the_mark() {
        val state = highlighted()
        val text = state.annotatedString.withoutBackgrounds()
        val marks = state.annotatedString.marks()
        assertEquals(listOf(Mark(0, 10, yellow)), marks)

        for (picked in listOf(TextRange(2, 8), TextRange(4, 14), TextRange(0, 16), TextRange(9, 3))) {
            state.selection = picked
            // 라이브러리는 고른 글 아래 형광펜을 지운 글을 새로 만듦
            assertNotEquals(text, state.annotatedString)
            assertEquals(text, state.annotatedString.withoutBackgrounds(), "$picked")
            assertEquals(marks, state.annotatedString.marks(), "$picked")
        }
    }

    @Test
    fun painting_adds_a_mark_and_no_background_on_the_text() {
        val state = highlighted()
        state.selection = TextRange(11, 16)

        state.addSpanStyle(SpanStyle(background = green))

        assertTrue(state.annotatedString.withoutBackgrounds().spanStyles.none { it.item.background.isSpecified })
        assertTrue(Mark(11, 16, green) in state.annotatedString.marks())
    }

    @Test
    fun other_styles_stay_on_the_text() {
        val text = AnnotatedString(
            "abcdef",
            listOf(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold, background = yellow), 0, 4))
        )

        val plain = text.withoutBackgrounds()

        assertEquals(listOf(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 4)), plain.spanStyles)
        assertEquals(listOf(Mark(0, 4, yellow)), text.marks())
    }

    @Test
    fun the_picked_part_of_a_mark_is_left_for_the_selection() {
        val mark = Mark(2, 10, yellow)

        assertEquals(listOf(2 to 10), mark.outside(TextRange(5)))
        assertEquals(listOf(2 to 10), mark.outside(TextRange(10, 14)))
        assertEquals(listOf(2 to 4, 7 to 10), mark.outside(TextRange(4, 7)))
        assertEquals(listOf(2 to 4, 7 to 10), mark.outside(TextRange(7, 4)))
        assertEquals(listOf(6 to 10), mark.outside(TextRange(0, 6)))
        assertEquals(emptyList(), mark.outside(TextRange(0, 12)))
    }
}
