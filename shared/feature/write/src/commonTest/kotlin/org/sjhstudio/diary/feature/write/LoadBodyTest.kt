package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

// 쓰기 화면이 본문을 넣을 때 커서 자리
class LoadBodyTest {
    @Test
    fun opening_an_entry_puts_the_caret_at_the_start() {
        val editor = RichTextState()
        editor.loadBody("<p>첫 줄</p><p>둘째 줄</p>", caret = -1)
        assertEquals(TextRange(0), editor.selection)
    }

    @Test
    fun a_saved_caret_comes_back() {
        val editor = RichTextState()
        editor.loadBody("<p>첫 줄</p><p>둘째 줄</p>", caret = 3)
        assertEquals(TextRange(3), editor.selection)
    }

    @Test
    fun a_saved_caret_past_the_end_stays_inside() {
        val editor = RichTextState()
        editor.loadBody("<p>짧은 글</p>", caret = 99)
        assertEquals(TextRange(editor.annotatedString.text.length), editor.selection)
    }
}
