@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 맥(iPad 앱) 하드웨어 키보드 한글이 실제로 부른 순서를 맥 앱 로그에서 옮김
 * 입력칸은 BasicTextField처럼 마지막으로 그린 값과 같은 변경을 넘기지 않음
 * 글 넣기는 바로 다시 그리고, 선택 바꾸기는 다시 그리지 않음(Compose iOS TextInputConnection)
 */
class MacHangulInputTest {
    private class Field(val state: RichTextState, fix: Boolean) {
        private val input = EditorInput(state, fix)
        private val processor = EditProcessor()
        private var drawn = TextFieldValue()

        init {
            redraw()
        }

        private fun redraw() {
            drawn = input.shown(state.textFieldValue)
            processor.reset(drawn, null)
        }

        private fun edit(command: EditCommand, redraws: Boolean) {
            val value = processor.apply(listOf(command))
            if (drawn != value) input.apply(value)
            if (redraws) redraw()
        }

        fun select(start: Int, end: Int) = edit(SetSelectionCommand(start, end), redraws = false)

        fun insert(text: String) = edit(CommitTextCommand(text, 1), redraws = true)
    }

    /** "안"까지 친 뒤 ㄴ: 음절 선택, 같은 음절로 확정, 새 자모 */
    private fun Field.typeAnN() {
        insert("ㅇ")
        select(0, 1)
        insert("아")
        select(0, 1)
        insert("안")
        select(0, 1)
        insert("안")
        insert("ㄴ")
    }

    @Test
    fun the_previous_syllable_stays_when_the_next_one_starts() {
        val state = RichTextState()
        Field(state, fix = true).typeAnN()
        assertEquals("안ㄴ", state.annotatedString.text)
    }

    @Test
    fun without_the_fix_the_commit_is_dropped_and_overwritten() {
        // 고치기 전 동작을 남겨 시험이 실제 원인을 짚는지 확인
        val state = RichTextState()
        Field(state, fix = false).typeAnN()
        assertEquals("ㄴ", state.annotatedString.text)
    }
}
