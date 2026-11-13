@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// Gboard 한글은 낱말 전체를 조합 중으로 잡음. 서식 버튼은 조합을 먼저 확정해야 누른 뒤 글자부터 바뀜
class CommitCompositionTest {
    private fun composing(word: String) = RichTextState().apply {
        onTextFieldValueChange(TextFieldValue(word, TextRange(word.length), composition = TextRange(0, word.length)))
    }

    @Test
    fun committing_keeps_the_text_and_clears_the_composition() {
        val state = composing("밑줄")

        state.commitComposition()

        assertEquals("밑줄", state.textFieldValue.text)
        assertNull(state.composition)
    }

    /** 밑줄을 켜고 조합 중에 끈 뒤 이어 쓰면 뒤 글자에는 밑줄이 없음 */
    @Test
    fun underline_turned_off_mid_word_stays_off_for_the_next_letters() {
        val state = composing("")
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.onTextFieldValueChange(TextFieldValue("밑줄", TextRange(2), composition = TextRange(0, 2)))

        state.commitComposition()
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.onTextFieldValueChange(TextFieldValue("밑줄 끝", TextRange(4), composition = TextRange(3, 4)))

        assertFalse(state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true)
    }
}
