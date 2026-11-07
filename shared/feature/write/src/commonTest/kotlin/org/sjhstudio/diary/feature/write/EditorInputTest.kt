@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * iOS 한글 키보드가 실제로 보내는 순서를 시뮬레이터에서 찍어 그대로 옮겼다.
 * 공백 뒤의 `ㄴ`을 `나`로 바꿀 때: 공백과 글자 고르기, 지우기, 공백 넣기, 글자 넣기.
 */
class EditorInputTest {
    private class Keyboard(val state: RichTextState, iosFix: Boolean = true) {
        private val input = EditorInput(state, iosFix)

        /** 수정 전 경로: 키보드가 고른 범위를 그대로 받는다. */
        fun type(text: String) {
            val value = state.textFieldValue
            val caret = value.selection.max
            val start = value.selection.min
            input.apply(TextFieldValue(value.text.substring(0, start) + text + value.text.substring(caret), TextRange(start + text.length)))
        }

        private fun backspace() {
            val value = state.textFieldValue
            val start = if (value.selection.collapsed) value.selection.min - 1 else value.selection.min
            input.apply(TextFieldValue(value.text.removeRange(start, value.selection.max), TextRange(start)))
        }

        /** 시뮬레이터 로그와 같은 순서로 글자를 바꾼다. */
        fun compose(jamo: String, syllable: String) {
            type(jamo)
            val value = state.textFieldValue
            val caret = value.selection.min
            val spaceBefore = caret >= 2 && value.text[caret - 2] == ' '
            input.apply(value.copy(selection = TextRange(caret - if (spaceBefore) 2 else 1, caret)))
            backspace()
            if (spaceBefore) type(" ")
            type(syllable)
        }

        fun lines() = state.toText().split("\n")
    }

    @Test
    fun theOldOrderJoinsTheLines() {
        val keyboard = Keyboard(RichTextState(), iosFix = false)
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        assertEquals(listOf("가 나"), keyboard.lines())
    }

    @Test
    fun aSyllableOnANewLineStaysOnThatLine() {
        val keyboard = Keyboard(RichTextState())
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        keyboard.compose("ㅁ", "남")
        assertEquals(listOf("가", "나남"), keyboard.lines())
    }

    @Test
    fun twoReturnsLeaveOneEmptyLine() {
        val keyboard = Keyboard(RichTextState())
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        assertEquals(listOf("가", "", "나"), keyboard.lines())
    }

    @Test
    fun aRealSpaceStays() {
        val keyboard = Keyboard(RichTextState())
        keyboard.compose("ㄱ", "가")
        keyboard.type(" ")
        keyboard.compose("ㄴ", "나")
        assertEquals(listOf("가 나"), keyboard.lines())
    }

    @Test
    fun aListKeepsItsMarkerAndLines() {
        val state = RichTextState()
        state.toggleUnorderedList()
        val keyboard = Keyboard(state)
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        assertEquals("<ul><li>가</li><li>나</li></ul>", state.toHtml().replace("\n", ""))
    }

    @Test
    fun anOrderedListKeepsItsNumbers() {
        val state = RichTextState()
        state.toggleOrderedList()
        val keyboard = Keyboard(state)
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        assertEquals("<ol><li>가</li><li>나</li></ol>", state.toHtml().replace("\n", ""))
    }

    @Test
    fun aDraggedSelectionOfLatinTextIsLeftAlone() {
        val state = RichTextState()
        val keyboard = Keyboard(state)
        keyboard.type("ab c")
        val value = state.textFieldValue
        EditorInput(state).apply(value.copy(selection = TextRange(2, 4)))
        assertEquals(TextRange(2, 4), state.selection)
    }

    @Test
    fun androidPassesEverythingThrough() {
        val state = RichTextState()
        val input = EditorInput(state, fixKeyboard = false)
        input.apply(TextFieldValue("가 나", TextRange(3)))
        input.apply(state.textFieldValue.copy(selection = TextRange(1, 3)))
        assertEquals(TextRange(1, 3), state.selection)
    }
}
