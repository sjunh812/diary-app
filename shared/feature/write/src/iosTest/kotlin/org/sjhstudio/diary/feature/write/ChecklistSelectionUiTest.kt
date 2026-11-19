package org.sjhstudio.diary.feature.write

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
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

        state.toggleChecklistLines()
        val added = state.toText()
        assertEquals("우유\n$open 달걀", added.substring(state.selection.min, state.selection.max))

        state.toggleChecklistLines()
        val removed = state.toText()
        assertEquals("우유\n달걀", removed.substring(state.selection.min, state.selection.max))
    }
}
