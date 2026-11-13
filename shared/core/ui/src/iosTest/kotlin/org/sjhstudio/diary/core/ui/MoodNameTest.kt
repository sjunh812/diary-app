package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DiaryFont

// 기분 칸 이름이 말줄임(…)으로 잘리지 않는지 봄
// 글자가 놓이는 폭은 태블릿 쓰기 옆 칸이 약 55(360 - 여백 48을 다섯 칸으로), 폰 시트가 약 59
@OptIn(ExperimentalTestApi::class)
class MoodNameTest {
    private fun overflows(name: String, font: DiaryFont, width: Int = PANEL_CELL, selected: Boolean = false): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        runComposeUiTest {
            setContent {
                DiaryTheme(darkTheme = false, fontFamily = diaryBodyFontFamily(font)) {
                    Box(Modifier.width(width.dp)) { MoodName(name, selected) }
                }
            }
            onNodeWithText(name).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        }
        return results.first().hasVisualOverflow
    }

    @Test
    fun english_laughing_fits_in_every_bundled_font() {
        for (font in listOf(DiaryFont.PRETENDARD, DiaryFont.CHANGWON_ROUND, DiaryFont.SYSTEM)) {
            assertFalse(overflows("Laughing", font), "$font")
            assertFalse(overflows("Laughing", font, selected = true), "$font 고름")
        }
    }

    @Test
    fun japanese_names_fit_with_the_system_font() {
        for (name in listOf("具合が悪い", "悲しい", "疲れた", "クール")) {
            assertFalse(overflows(name, DiaryFont.SYSTEM), name)
        }
    }

    private companion object {
        const val PANEL_CELL = 55
    }
}
