@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * iOS 한글 키보드가 실제로 보내는 순서를 시뮬레이터에서 찍어 그대로 옮김
 * 공백 뒤의 `ㄴ`을 `나`로 바꿀 때: 공백과 글자 고르기, 지우기, 공백 넣기, 글자 넣기
 */
class EditorInputTest {
    private class Keyboard(val state: RichTextState, iosFix: Boolean = true) {
        private val input = EditorInput(state, iosFix)

        /** 수정 전 경로: 키보드가 고른 범위를 그대로 받음 */
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

        /** 시뮬레이터 로그와 같은 순서로 글자를 바꿈 */
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
    fun aNumberedLineIsPlainTextAndTheNextLineHasNoNumber() {
        val state = RichTextState()
        state.toggleOrderedList()
        val keyboard = Keyboard(state)
        keyboard.compose("ㄱ", "가")
        keyboard.type("\n")
        keyboard.compose("ㄴ", "나")
        // 번호 목록은 이 앱 형식에 없어 글자 줄로 풀리고, 줄을 바꿔도 다음 번호가 붙지 않음
        assertEquals("<p>1. 가</p><p>나</p>", state.toHtml().replace("\n", ""))
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

    // 선택 핸들을 끄는 중에는 손가락이 가리킨 범위를 그대로 씀
    // 라이브러리 보정을 거치면 끝이 붙잡혀 핸들이 가다가 멈춤
    @Test
    fun handleDragKeepsTheExactRange() {
        val state = RichTextState()
        state.setText("alpha beta gamma delta epsilon zeta eta")
        val input = EditorInput(state, fixKeyboard = false)
        state.selection = TextRange(17, 22)
        input.apply(state.textFieldValue.copy(selection = TextRange(17, 35)))
        assertEquals(TextRange(17, 35), state.selection)
    }

    // 줄 맨 앞에 "1. "을 쳐도 번호 목록이 되지 않고 글자로 남음. 줄을 바꿔도 "2. "가 붙지 않음(사용자 피드백)
    @Test
    fun typedNumberStaysText() {
        val state = RichTextState()
        val input = EditorInput(state, fixKeyboard = false)
        for (typed in listOf("1", "1.", "1. ", "1. a", "1. a\n")) input.apply(TextFieldValue(typed, TextRange(typed.length)))

        assertEquals(false, state.isOrderedList)
        assertEquals("1. a\n", state.toText())
    }

    // 글 중간 줄에서 쳐도 같음. 다른 줄의 글과 커서는 그대로
    @Test
    fun typedNumberInTheMiddleStaysText() {
        val state = RichTextState()
        state.setText("첫 줄\n")
        val input = EditorInput(state, fixKeyboard = false)
        for (typed in listOf("첫 줄\n3", "첫 줄\n3.", "첫 줄\n3. ")) input.apply(TextFieldValue(typed, TextRange(typed.length)))

        assertEquals(false, state.isOrderedList)
        assertEquals("첫 줄\n3. ", state.toText())
        assertEquals(TextRange(7), state.selection)
    }
}
