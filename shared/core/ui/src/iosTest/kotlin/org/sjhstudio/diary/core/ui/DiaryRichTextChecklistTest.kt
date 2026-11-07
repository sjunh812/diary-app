package org.sjhstudio.diary.core.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Checklist

// 칸은 그림이라 화면 낭독기는 동작으로 체크를 바꾼다.
@OptIn(ExperimentalTestApi::class)
class DiaryRichTextChecklistTest {
    private val html = "<p>${Checklist.OPEN} 우유<br>${Checklist.DONE} 달걀</p>"
    private val hasActions = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

    @Test
    fun each_item_has_a_toggle_action() = runComposeUiTest {
        var checked = -1
        setContent {
            DiaryTheme(darkTheme = false) { DiaryRichText(html, TextStyle.Default, onCheck = { checked = it }) }
        }

        val node = onNode(hasActions).fetchSemanticsNode()
        val actions = node.config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        assertEquals(2, actions.size)
        assertTrue(actions[1].action())
        assertEquals(1, checked)
    }

    @Test
    fun plain_reading_has_no_actions() = runComposeUiTest {
        setContent { DiaryTheme(darkTheme = false) { DiaryRichText("<p>우유</p>", TextStyle.Default) } }

        val node = onNode(hasActions).fetchSemanticsNode()
        assertTrue(androidx.compose.ui.semantics.SemanticsActions.CustomActions !in node.config)
    }
}
