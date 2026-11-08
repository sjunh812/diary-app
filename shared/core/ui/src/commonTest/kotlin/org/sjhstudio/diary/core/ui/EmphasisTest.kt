package org.sjhstudio.diary.core.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class EmphasisTest {
    private val bold = SpanStyle(fontWeight = FontWeight.SemiBold)

    @Test
    fun marked_values_are_styled_and_marks_removed() {
        val text = "이번 달엔 ${emphasis("보통, 피곤")}이 가장 많았어요".withEmphasis(bold)

        assertEquals("이번 달엔 보통, 피곤이 가장 많았어요", text.text)
        assertEquals(listOf("보통, 피곤"), text.spanStyles.map { text.text.substring(it.start, it.end) })
    }

    @Test
    fun several_values_keep_their_own_order_in_any_language() {
        val text = "${emphasis(12)}% → ${emphasis(30)}%".withEmphasis(bold)

        assertEquals("12% → 30%", text.text)
        assertEquals(listOf("12", "30"), text.spanStyles.map { text.text.substring(it.start, it.end) })
    }

    @Test
    fun a_broken_mark_never_shows_up_on_screen() {
        val text = "앞 뒤".withEmphasis(bold)

        assertEquals("앞 뒤", text.text)
    }
}
