package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

@OptIn(ExperimentalTestApi::class)
class DiaryMonthPickerTest {
    @Test
    fun title_swaps_the_days_for_the_wheel_and_back() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) { DiaryMonthPicker(YearMonth(2026, 7), {}) { BasicText("days") } }
        }
        onNodeWithText("days").assertExists()
        onAllNodes(hasClickAction()).assertCountEquals(3)

        // 화살표 둘 사이의 가운데가 제목
        onAllNodes(hasClickAction())[1].performClick()
        onAllNodesWithText("days").assertCountEquals(0)
        onAllNodes(hasClickAction()).assertCountEquals(1)

        onAllNodes(hasClickAction())[0].performClick()
        onNodeWithText("days").assertExists()
    }
}
