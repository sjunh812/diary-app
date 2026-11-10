package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 두 버튼 글이 잘리지 않음. 짧으면 나란히, 길면 주 버튼을 위로 쌓음
@OptIn(ExperimentalTestApi::class)
class DiaryButtonPairTest {
    @Test
    fun short_labels_stay_side_by_side() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(320.dp)) { DiaryButtonPair("Close", {}, "Done", {}) }
            }
        }
        val close = onNodeWithText("Close").getBoundsInRoot()
        val done = onNodeWithText("Done").getBoundsInRoot()
        assertEquals(close.top, done.top)
        assertTrue(close.right < done.left)
    }

    @Test
    fun long_label_stacks_with_primary_on_top() = runComposeUiTest {
        val long = "A label far too long to share half of a small phone screen"
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(320.dp)) { DiaryButtonPair("Not now", {}, long, {}) }
            }
        }
        val later = onNodeWithText("Not now").getBoundsInRoot()
        val primary = onNodeWithText(long).getBoundsInRoot()
        assertTrue(primary.bottom <= later.top)
    }
}
