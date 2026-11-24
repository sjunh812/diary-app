package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 대화상자가 뜨면 뒤 화면 입력 칸의 초점을 풀고, 닫아도 돌려주지 않음
@OptIn(ExperimentalTestApi::class)
class DismissBehindKeyboardTest {
    @Test
    fun a_dialog_takes_the_focus_away_and_does_not_give_it_back() = runComposeUiTest {
        var open by mutableStateOf(false)
        setContent {
            DiaryTheme(darkTheme = false) {
                BasicTextField(value = "글", onValueChange = {}, modifier = Modifier.testTag("field"))
                if (open) DiaryDialog(title = "제목", confirmLabel = "확인", onConfirm = { open = false }, onDismiss = { open = false })
            }
        }
        onNodeWithTag("field").performClick()
        onNodeWithTag("field").assertIsFocused()

        open = true
        waitForIdle()
        onNodeWithTag("field", useUnmergedTree = true).assertIsNotFocused()

        open = false
        waitForIdle()
        onNodeWithTag("field").assertIsNotFocused()
    }
}
