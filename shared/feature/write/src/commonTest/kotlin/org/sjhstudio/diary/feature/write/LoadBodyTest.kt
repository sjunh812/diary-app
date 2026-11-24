package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

// 쓰기 화면이 본문을 넣을 때 커서 자리
class LoadBodyTest {
    @Test
    fun editing_an_entry_puts_the_caret_at_the_start() {
        val editor = RichTextState()
        editor.loadBody("<p>첫 줄</p><p>둘째 줄</p>", caret = -1, writing = false)
        assertEquals(TextRange(0), editor.selection)
    }

    @Test
    fun a_saved_caret_comes_back() {
        val editor = RichTextState()
        editor.loadBody("<p>첫 줄</p><p>둘째 줄</p>", caret = 3, writing = false)
        assertEquals(TextRange(3), editor.selection)
    }

    @Test
    fun a_saved_caret_past_the_end_stays_inside() {
        val editor = RichTextState()
        editor.loadBody("<p>짧은 글</p>", caret = 99, writing = true)
        assertEquals(TextRange(editor.annotatedString.text.length), editor.selection)
    }

    @Test
    fun continuing_a_draft_writes_at_the_end() {
        // [이어 쓰기]는 키보드가 바로 떠 친 글이 글 끝에 붙어야 함
        val editor = RichTextState()
        editor.loadBody("<p>첫 줄</p><p>둘째 줄</p>", caret = -1, writing = true)
        assertEquals(TextRange(editor.annotatedString.text.length), editor.selection)
    }
}
