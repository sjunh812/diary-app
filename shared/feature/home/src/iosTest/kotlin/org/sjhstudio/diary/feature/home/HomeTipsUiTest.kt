package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeContextTips
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview_kept
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview_kept_action
import org.sjhstudio.diary.feature.home.resources.tip_home_list_style
import platform.UIKit.UIViewController

private object NoTipFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

// 홈 목록 위 팁은 열린 첫 팁 하나만(스펙 008 FR-011)
@OptIn(ExperimentalTestApi::class)
class HomeTipsUiTest {
    private fun text(resource: org.jetbrains.compose.resources.StringResource) = runBlocking { getString(resource) }

    @AfterTest
    fun tearDown() = stopKoin()

    private fun ComposeUiTest.home(
        open: Set<ContextTip>,
        length: PreviewLength,
        style: ListStyle = ListStyle.FEED,
        chosen: Boolean = false
    ): FakeSettingsRepository {
        val settings =
            FakeSettingsRepository(AppSettings(list = ListPreferences(listStyle = style, previewLength = length, previewChosen = chosen)))
        val tips = FakeContextTips(open)
        val rows = List(6) { fakeEntryRow("e$it", LocalDate(2026, 11, 10 - it), "일기 $it", false) }
        startKoin {
            modules(
                module {
                    single<ContextTips> { tips }
                    viewModel {
                        val entries = FakeEntryRepository(rows = rows)
                        HomeViewModel(entries, settings, NoTipFonts, FakePlanRepository(), tips, HomeMonths(entries, MainScope()))
                    }
                }
            )
        }
        setContent {
            CompositionLocalProvider(LocalUIViewController provides UIViewController(null, null)) {
                DiaryTheme(darkTheme = false) {
                    Box(Modifier.size(400.dp, 800.dp)) { HomeRoute({}, {}, {}, {}, {}, selectedId = null, onListStyle = {}) }
                }
            }
        }
        waitForIdle()
        return settings
    }

    @Test
    fun only_the_first_open_tip_shows() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW_KEPT, ContextTip.HOME_LIST_STYLE), PreviewLength.FULL)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview_kept))).assertCountEquals(1)
        onAllNodes(hasText(text(Res.string.tip_home_list_style))).assertCountEquals(0)
    }

    @Test
    fun kept_tip_can_go_back_to_three_lines() = runComposeUiTest {
        val settings = home(setOf(ContextTip.HOME_FULL_PREVIEW_KEPT), PreviewLength.FULL)

        onNode(hasText(text(Res.string.tip_home_full_preview_kept_action))).performClick()
        waitForIdle()

        assertEquals(PreviewLength.THREE_LINES, settings.settings.value.list.previewLength)
    }

    /** 팁이 미뤄진 사이 길이를 바꿨으면 "예전처럼 전체를 보여드려요"는 사실이 아니라 띄우지 않음 */
    @Test
    fun kept_tip_needs_the_full_preview() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW_KEPT), PreviewLength.FIVE_LINES, chosen = true)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview_kept))).assertCountEquals(0)
    }

    /** 직접 다시 전체로 골랐으면 이어 온 것이 아니라 띄우지 않음 */
    @Test
    fun kept_tip_skips_a_full_preview_the_user_picked() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW_KEPT), PreviewLength.FULL, chosen = true)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview_kept))).assertCountEquals(0)
    }

    @Test
    fun full_preview_tip_is_only_for_three_lines() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW), PreviewLength.FULL)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview))).assertCountEquals(0)
    }

    @Test
    fun photo_grid_shows_no_length_tip() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW, ContextTip.HOME_FULL_PREVIEW_KEPT), PreviewLength.THREE_LINES, ListStyle.PHOTO_GRID)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview))).assertCountEquals(0)
        onAllNodes(hasText(text(Res.string.tip_home_full_preview_kept))).assertCountEquals(0)
    }
}
