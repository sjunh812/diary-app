package org.sjhstudio.diary.feature.write

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.Checklist

// 여러 줄을 고른 채 체크리스트 도구를 눌러도 고른 글이 그대로 골라져 있음
@OptIn(ExperimentalTestApi::class)
class ChecklistSelectionUiTest {
    private val open = Checklist.OPEN

    @Test
    fun adding_and_removing_marks_keeps_the_picked_lines() = runComposeUiTest {
        val state = RichTextState()
        state.setHtml("<p>오늘<br>우유<br>달걀</p>")
        val text = state.toText()
        state.selection = TextRange(text.indexOf("우유"), text.length)

        state.changeChecklistLines(Checklist::toggleLines)
        val added = state.toText()
        assertEquals("우유\n$open 달걀", added.substring(state.selection.min, state.selection.max))

        state.changeChecklistLines(Checklist::toggleLines)
        val removed = state.toText()
        assertEquals("우유\n달걀", removed.substring(state.selection.min, state.selection.max))
    }

    /** 체크리스트와 일반 줄을 함께 골라 글머리 목록으로 바꾸면 표시만 빠지고 일반 줄에 표시가 생기지 않음 */
    @Test
    fun a_mixed_selection_becomes_a_clean_list() = runComposeUiTest {
        val state = RichTextState()
        state.setHtml("<p>$open 우유<br>달걀</p>")
        state.selection = TextRange(0, state.toText().length)

        state.toggleList()

        assertTrue(state.isUnorderedList)
        assertFalse(state.toText().contains(open), state.toText())
    }

    /** 앞 줄이 이미 목록이어도 체크리스트 줄을 고르면 둘 다 목록으로 */
    @Test
    fun a_list_and_a_checklist_become_one_list() = runComposeUiTest {
        val state = RichTextState()
        state.setHtml("<ul><li>우유</li></ul><p>$open 달걀</p>")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.toggleList()

        assertFalse(state.toText().contains(open), state.toText())
        assertEquals(listOf("우유", "달걀"), state.toHtml().split("<li>").drop(1).map { it.substringBefore("</li>") })
    }
}
