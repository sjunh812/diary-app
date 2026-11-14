package org.sjhstudio.diary.feature.write

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 화면 읽기가 서식 도구의 켜짐 상태를 알 수 있어야 함
@OptIn(ExperimentalTestApi::class)
class ToolButtonUiTest {
    @Test
    fun toggles_report_their_state_and_plain_buttons_do_not() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                ToolButton(DiaryIcons.Bold, "on", Color.Black, onClick = {}, active = true)
                ToolButton(DiaryIcons.Bold, "off", Color.Black, onClick = {}, active = false)
                ToolButton(DiaryIcons.Image, "plain", Color.Black, onClick = {})
            }
        }
        onNodeWithContentDescription("on").assertIsSelected()
        onNodeWithContentDescription("off").assertIsNotSelected()
        onNodeWithContentDescription("plain").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }
}
