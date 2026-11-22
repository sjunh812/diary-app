package org.sjhstudio.diary.feature.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.theme_color_lavender
import org.sjhstudio.diary.feature.settings.resources.theme_color_sea
import org.sjhstudio.diary.feature.settings.resources.theme_custom
import org.sjhstudio.diary.feature.settings.resources.theme_custom_apply
import org.sjhstudio.diary.feature.settings.resources.theme_light
import org.sjhstudio.diary.feature.settings.resources.theme_plus_title
import org.sjhstudio.diary.feature.settings.resources.theme_swatch_plus

// 테마 화면(스펙 010 US1~US4, US6). 이름은 리소스에서 읽어 언어와 상관없이 찾음
@OptIn(ExperimentalTestApi::class)
class ThemeSettingsUiTest {
    private val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    private class Calls {
        val modes = mutableListOf<ThemeMode>()
        val colors = mutableListOf<ThemeColor>()
        val customs = mutableListOf<Int>()
    }

    private fun run(theme: ThemeChoice, canCustom: Boolean?, check: ComposeUiTest.(Calls) -> Unit) = runComposeUiTest {
        val calls = Calls()
        setContent {
            DiaryTheme(darkTheme = false) {
                ThemeSettingsScreen(
                    mode = ThemeMode.SYSTEM,
                    theme = theme,
                    listStyle = ListStyle.FEED,
                    canCustom = canCustom,
                    onMode = { calls.modes += it },
                    onColor = { calls.colors += it },
                    onCustom = { calls.customs += it },
                    onPlus = {},
                    onBack = {}
                )
            }
        }
        waitForIdle()
        check(calls)
    }

    private fun text(resource: org.jetbrains.compose.resources.StringResource) = runBlocking { getString(resource) }

    @Test
    fun two_pictures_and_eight_swatches_are_choices() = run(ThemeChoice(), canCustom = true) {
        // 그림 둘 + 견본 여덟
        assertEquals(10, onAllNodes(radio).fetchSemanticsNodes().size)
        onNodeWithContentDescription(text(Res.string.theme_color_lavender)).assertIsSelected()
    }

    @Test
    fun picking_a_swatch_and_a_picture_saves_right_away() = run(ThemeChoice(), canCustom = true) { calls ->
        onNodeWithContentDescription(text(Res.string.theme_color_sea)).performClick()
        onNodeWithContentDescription(text(Res.string.theme_light)).performClick()

        assertEquals(listOf(ThemeColor.SEA), calls.colors)
        assertEquals(listOf(ThemeMode.LIGHT), calls.modes)
    }

    @Test
    fun free_custom_has_a_plus_chip_and_asks_for_plus() = run(ThemeChoice(), canCustom = false) { calls ->
        val custom = text(Res.string.theme_custom)
        onNodeWithContentDescription(runBlocking { getString(Res.string.theme_swatch_plus, custom) }).performClick()
        waitForIdle()

        onNodeWithText(text(Res.string.theme_plus_title)).assertExists()
        assertEquals(emptyList(), calls.customs)
    }

    @Test
    fun plus_custom_opens_the_picker_and_applies_only_on_confirm() = run(ThemeChoice(), canCustom = true) { calls ->
        onNodeWithContentDescription(text(Res.string.theme_custom)).performClick()
        waitForIdle()
        assertEquals(emptyList(), calls.customs)

        onNodeWithText(text(Res.string.theme_custom_apply)).performClick()
        waitForIdle()
        // 아직 고른 색이 없으면 지금 테마 색(라벤더)에서 시작
        assertEquals(listOf(0x6E56CF), calls.customs)
    }

    /** 색 코드를 덜 넣으면 적용할 수 없음. 칸에 보이는 값과 다른 앞 색이 저장되지 않게 */
    @Test
    fun an_unfinished_color_code_cannot_be_applied() = run(ThemeChoice(), canCustom = true) { calls ->
        onNodeWithContentDescription(text(Res.string.theme_custom)).performClick()
        waitForIdle()
        onNodeWithText("6E56CF").performTextReplacement("2A9D8")
        waitForIdle()

        onNodeWithText(text(Res.string.theme_custom_apply)).assertIsNotEnabled().performClick()
        waitForIdle()
        assertEquals(emptyList(), calls.customs)
    }

    /** Plus가 끝난 직후 직접 고른 색이 지워지기 전에도 라벤더로 보임 */
    @Test
    fun custom_without_plus_shows_lavender() = run(ThemeChoice(colorId = "custom", customHex = "#C2185B"), canCustom = false) {
        onNodeWithContentDescription(text(Res.string.theme_color_lavender)).assertIsSelected()
        onNodeWithContentDescription(runBlocking { getString(Res.string.theme_swatch_plus, text(Res.string.theme_custom)) })
            .assertExists()
    }

    @Test
    fun unknown_plan_disables_custom() = run(ThemeChoice(), canCustom = null) {
        onNodeWithContentDescription(text(Res.string.theme_custom)).assertIsNotEnabled()
    }
}
