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

// 기분 칸 이름이 잘리지 않는지 봄
// 글자 폭은 칸 68에서 고른 바탕 여백 2씩을 뺀 64, 태블릿 쓰기 옆 칸은 칸이 줄어 약 63. 좁은 쪽보다 조금 더 좁게 봄
@OptIn(ExperimentalTestApi::class)
class MoodNameTest {
    private fun overflows(name: String, font: DiaryFont, width: Int = PANEL_CELL, selected: Boolean = false): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        runComposeUiTest {
            setContent {
                DiaryTheme(darkTheme = false, fontFamily = diaryBodyFontFamily(font) { null }) {
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

    /** 한 낱말 이름은 줄여도 한 줄에 다 들어가야 함. 프랑스어 Enthousiaste가 끝 글자가 잘렸음 */
    @Test
    fun one_word_mood_names_fit_in_every_language() {
        val names =
            listOf(
                "Excited", "Laughing", "Annoyed", "Anxious",
                "Emballé", "Agacé", "Anxieux", "Ennuyé", "Fatigué",
                "Begeistert", "Entspannt", "Gelangweilt", "Unruhig", "Fröhlich", "Schlecht",
                "Ilusionado", "Orgulloso", "Aburrido", "Enfermo", "Cansado",
                "Empolgado", "Orgulhoso", "Entediado", "Tranquilo", "Irritado",
                "ワクワク", "誇らしい", "イライラ", "不舒服"
            )
        for (font in listOf(DiaryFont.PRETENDARD, DiaryFont.CHANGWON_ROUND, DiaryFont.SYSTEM)) {
            for (name in names) assertFalse(overflows(name, font, selected = true), "$font $name")
        }
    }

    private companion object {
        const val PANEL_CELL = 61
    }
}
