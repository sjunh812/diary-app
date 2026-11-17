package org.sjhstudio.diary.feature.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.list_group_length
import org.sjhstudio.diary.feature.settings.resources.list_length_five

// 목록 보기(스펙 008 US2·US5). 언어와 상관없이 역할로 찾음
@OptIn(ExperimentalTestApi::class)
class ListStyleSettingsUiTest {
    private val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    private val switch = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private val lengths = mutableListOf<PreviewLength>()

    private fun run(list: ListPreferences, canFormat: Boolean, check: ComposeUiTest.(MutableList<Boolean>) -> Unit) = runComposeUiTest {
        val formatting = mutableListOf<Boolean>()
        setContent {
            DiaryTheme(darkTheme = false) {
                ListStyleSettingsScreen(
                    list = list,
                    canFormat = canFormat,
                    previewStyle = DiaryTheme.typography.body,
                    today = LocalDate(2026, 11, 17),
                    onStyle = {},
                    onLength = { lengths += it },
                    onFormatting = { formatting += it },
                    onPlus = {},
                    onBack = {}
                )
            }
        }
        waitForIdle()
        check(formatting)
    }

    @Test
    fun feed_shows_style_choices_length_row_and_the_formatting_switch() = run(ListPreferences(), canFormat = true) {
        // 종류는 고르기 줄 셋, 본문 길이는 값 줄 하나
        assertEquals(3, onAllNodes(radio).fetchSemanticsNodes().size)
        assertEquals(1, onAllNodes(switch).fetchSemanticsNodes().size)
    }

    @Test
    fun photo_grid_hides_length_and_formatting() = run(ListPreferences(listStyle = ListStyle.PHOTO_GRID), canFormat = true) {
        assertEquals(3, onAllNodes(radio).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(switch).fetchSemanticsNodes().size)
    }

    @Test
    fun plus_turns_formatting_on() = run(ListPreferences(), canFormat = true) { formatting ->
        onNode(switch).performClick()
        waitForIdle()
        assertEquals(listOf(true), formatting)
    }

    @Test
    fun free_user_does_not_turn_formatting_on() = run(ListPreferences(showFormatting = true), canFormat = false) { formatting ->
        // 저장 값이 켜져 있어도 무료면 꺼진 채 보임
        onNode(switch and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off)).performClick()
        waitForIdle()
        assertEquals(emptyList(), formatting)
    }

    @Test
    fun length_row_opens_a_sheet_with_three_choices() = run(ListPreferences(), canFormat = true) {
        // 값 줄을 누르면 시트에 3줄·5줄·전체, 고르면 닫힘
        onNodeWithText(runBlocking { getString(Res.string.list_group_length) }).performClick()
        waitForIdle()
        assertEquals(6, onAllNodes(radio).fetchSemanticsNodes().size)
        onNodeWithText(runBlocking { getString(Res.string.list_length_five) }).performClick()
        waitForIdle()
        assertEquals(listOf(PreviewLength.FIVE_LINES), lengths)
        assertEquals(3, onAllNodes(radio).fetchSemanticsNodes().size)
    }
}
