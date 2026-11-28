package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 설정 줄 값이 화살표 옆에 들어가지 않으면 이름 아래로 내려가 말줄임하지 않음
@OptIn(ExperimentalTestApi::class)
class DiarySettingsRowTest {
    private val width = 360.dp

    @Test
    fun long_value_moves_under_the_title_at_double_text_size() = runComposeUiTest {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DiaryTheme(darkTheme = false) {
                    Column(Modifier.width(width)) {
                        DiarySettingsRow("Theme", onClick = {}, value = "Lavender · System default")
                        DiarySettingsValueRow("Version", "2.2.1", valueNote = "Downloading 42%")
                    }
                }
            }
        }
        for (text in listOf("Lavender · System default", "2.2.1 · Downloading 42%")) {
            val value = onNodeWithText(text, useUnmergedTree = true)
            assertFalse(value.layout().hasVisualOverflow, text)
            assertTrue(value.getUnclippedBoundsInRoot().right <= width + 0.5.dp, text)
        }
        val title = onNodeWithText("Theme", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = onNodeWithText("Lavender · System default", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(value.top >= title.bottom, "값이 이름 아래에 있어야 함")
        assertTrue(value.left == title.left, "값이 이름과 같은 선에서 시작해야 함")
    }

    @Test
    fun short_value_stays_beside_the_chevron() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Column(Modifier.width(width)) { DiarySettingsRow("Theme", onClick = {}, value = "Lavender · System default") }
            }
        }
        val title = onNodeWithText("Theme", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = onNodeWithText("Lavender · System default", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(value.left > title.right, "값이 이름 오른쪽에 있어야 함")
        assertTrue(value.top < title.bottom, "값이 이름과 같은 줄에 있어야 함")
    }

    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }
}
