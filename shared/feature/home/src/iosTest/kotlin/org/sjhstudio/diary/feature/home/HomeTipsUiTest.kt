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

    private fun ComposeUiTest.home(open: Set<ContextTip>, length: PreviewLength): FakeSettingsRepository {
        val settings = FakeSettingsRepository(AppSettings(list = ListPreferences(previewLength = length)))
        val tips = FakeContextTips(open)
        val rows = List(6) { fakeEntryRow("e$it", LocalDate(2026, 11, 10 - it), "일기 $it", false) }
        startKoin {
            modules(
                module {
                    single<ContextTips> { tips }
                    viewModel { HomeViewModel(FakeEntryRepository(rows = rows), settings, NoTipFonts, FakePlanRepository(), tips) }
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

    @Test
    fun full_preview_tip_is_only_for_three_lines() = runComposeUiTest {
        home(setOf(ContextTip.HOME_FULL_PREVIEW), PreviewLength.FULL)

        onAllNodes(hasText(text(Res.string.tip_home_full_preview))).assertCountEquals(0)
    }
}
