package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AppSettings

// 글자 크기를 200%로 키운 작은 폰에서도 설정 줄 글자가 잘리거나 화면 밖으로 나가지 않음
@OptIn(ExperimentalTestApi::class)
class SettingsLargeTextUiTest {
    private val width = 360.dp

    @Test
    fun every_row_stays_readable_at_double_text_size() {
        for (dark in listOf(false, true)) {
            runComposeUiTest {
                setContent {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                        DiaryTheme(darkTheme = dark) {
                            Box(Modifier.size(width, 4000.dp)) {
                                SettingsScreen(
                                    SettingsUiState(settings = AppSettings(reminderEnabled = true), loaded = true),
                                    onFont = {},
                                    onTextSize = {},
                                    onTheme = {},
                                    onReminder = {},
                                    onLock = {},
                                    onMigrationReport = {},
                                    onLicenses = {},
                                    trashCount = 128,
                                    backupValue = "오늘 오전 12:52",
                                    appVersion = "2.0.0"
                                )
                            }
                        }
                    }
                }
                waitForIdle()

                val texts = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                val count = texts.fetchSemanticsNodes().size
                assertTrue(count > 10, "설정 줄이 그려지지 않았다: $count")
                val cut = mutableListOf<String>()
                for (index in 0 until count) {
                    val node = texts[index]
                    val bounds = node.getUnclippedBoundsInRoot()
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.fetchSemanticsNode().config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) {
                        null
                    }?.action?.invoke(layouts)
                    val layout = layouts.singleOrNull() ?: continue
                    // 줄 수를 막아 위아래가 잘렸거나 화면 오른쪽 밖으로 나간 글자를 모음
                    if (layout.didOverflowHeight || bounds.right > width + 0.5.dp) cut += layout.layoutInput.text.text
                }
                assertEquals(emptyList(), cut, "dark=$dark")
            }
        }
    }
}
