package org.sjhstudio.diary.app.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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
import org.sjhstudio.diary.app.navigation.AppLaunch
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.detail_pick_hint
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.DefaultWelcomeRepository
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoAdPlayer
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.data.WelcomeRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.feature.backup.backupFeatureModule
import org.sjhstudio.diary.feature.calendar.CalendarViewModel
import org.sjhstudio.diary.feature.home.EntryDetailViewModel
import org.sjhstudio.diary.feature.home.HomeViewModel
import org.sjhstudio.diary.feature.settings.FontReplacedNoticeViewModel
import platform.UIKit.UIViewController

private object NoAdaptiveFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

/** 창 폭이 바뀌어도 고른 탭이 그대로이고, 840dp부터 두 칸(FR-037) */
@OptIn(ExperimentalTestApi::class)
class AdaptiveLayoutUiTest {
    private val day = LocalDate(2026, 10, 31)
    private var moods = mapOf(day to DayMood(day, null, 1))
    private var rows: List<EntryRow> = emptyList()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                backupFeatureModule,
                appShellModule,
                module {
                    single { AppLaunch() }
                    single<PlanRepository> { FreePlanRepository() }
                    single { AdRepository(get(), AdStore(InMemoryDataStore(emptyPreferences())), NoAdPlayer) }
                    single<BackupRepository> { FakeBackupRepository() }
                    single<RestoreRepository> { FakeRestoreRepository() }
                    single<WelcomeRepository> { DefaultWelcomeRepository(WelcomeStore(InMemoryDataStore(emptyPreferences()))) { null } }
                    single { Telemetry(NoTelemetrySink, strict = true) }
                    viewModel { FontReplacedNoticeViewModel(FakeSettingsRepository()) }
                    single<EntryRepository> { FakeEntryRepository(moods, rows) }
                    viewModel { (entryId: String) -> EntryDetailViewModel(entryId, get(), FakeSettingsRepository(), NoAdaptiveFonts) }
                    viewModel { HomeViewModel(get(), FakeSettingsRepository(), NoAdaptiveFonts) }
                    viewModel { CalendarViewModel(get(), FakeSettingsRepository(), NoAdaptiveFonts, saved = SavedStateHandle()) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() = stopKoin()

    private val tabs = isSelectable() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private val hint get() = runBlocking { getString(Res.string.detail_pick_hint) }

    @Test
    fun two_panes_start_at_840() = runComposeUiTest {
        var width by mutableStateOf<Dp>(839.dp)
        setContent { DiaryTheme(darkTheme = false) { Box(Modifier.size(width, 700.dp)) { AppScaffold() } } }
        waitForIdle()
        assertEquals(0, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)

        width = 840.dp
        waitForIdle()

        assertEquals(1, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
    }

    /** 일기가 아직 없어도 오른쪽 칸이 통째로 비지 않음 */
    @Test
    fun hint_stays_when_there_are_no_entries() = runComposeUiTest {
        moods = emptyMap()
        setContent { DiaryTheme(darkTheme = false) { Box(Modifier.size(840.dp, 700.dp)) { AppScaffold() } } }
        waitForIdle()

        assertEquals(1, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
    }

    @Test
    fun selected_tab_survives_width_changes() = runComposeUiTest {
        var width by mutableStateOf<Dp>(1280.dp)
        setContent { DiaryTheme(darkTheme = false) { Box(Modifier.size(width, 700.dp)) { AppScaffold() } } }
        waitForIdle()
        onAllNodes(tabs)[1].performClick()
        waitForIdle()

        for (next in listOf(840.dp, 600.dp, 360.dp)) {
            width = next
            waitForIdle()
            onAllNodes(tabs)[1].assertIsSelected()
        }
    }

    /** 달력에서 연 상세는 넓은 화면에서도 한 칸이라 돌아갈 길이 있어야 함 */
    @Test
    fun a_detail_opened_from_the_calendar_has_a_back_button() = runComposeUiTest {
        rows = listOf(fakeEntryRow("a", currentDate()))
        setContent { TwoPanes() }
        waitForIdle()
        onAllNodes(tabs)[1].performClick()
        waitForIdle()

        openRowA()
        waitForIdle()

        assertEquals(1, onAllNodes(backButton).fetchSemanticsNodes().size)
        onNode(backButton).performClick()
        waitForIdle()
        waitUntil(timeoutMillis = ROW_WAIT_MILLIS) { onAllNodes(hasText("글 a", substring = true)).fetchSemanticsNodes().size == 1 }
    }

    /** 두 칸에서는 왼쪽 목록이 그대로 보여 뒤로 버튼이 필요 없음 */
    @Test
    fun a_detail_beside_the_list_has_no_back_button() = runComposeUiTest {
        rows = listOf(fakeEntryRow("a", day))
        setContent { TwoPanes() }
        waitForIdle()

        openRowA()
        waitForIdle()

        assertEquals(0, onAllNodes(backButton).fetchSemanticsNodes().size)
    }

    private val backButton =
        SemanticsMatcher("뒤로 가기 단추") { node ->
            val key = SemanticsProperties.ContentDescription
            key in node.config && node.config[key].any { it in BACK_LABELS }
        }

    /**
     * 목록의 "글 a" 줄을 엶
     * 목록은 백그라운드에서 읽어 waitForIdle이 끝나도 줄이 아직 없을 수 있음
     * 줄이 처음 나타나는 동안 합친 의미 트리에 글이 늦게 붙기도 해 느린 CI에서 가끔 못 찾음
     * 그래서 합치기 전 트리에서 글을 찾고, 보일 때까지 기다린 뒤 누름
     */
    private fun ComposeUiTest.openRowA() {
        val row = hasText("글 a", substring = true)
        waitUntil(timeoutMillis = ROW_WAIT_MILLIS) {
            onAllNodes(row, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodes(row, useUnmergedTree = true)[0].performClick()
    }

    /** 상세의 공유가 화면 컨트롤러를 찾음 */
    @Composable
    private fun TwoPanes() {
        CompositionLocalProvider(LocalUIViewController provides UIViewController(null, null)) {
            DiaryTheme(darkTheme = false) { Box(Modifier.size(1000.dp, 700.dp)) { AppScaffold() } }
        }
    }

    /** 앱이 죽었다 살아나도 고른 탭과 쌓인 화면이 돌아옴 */
    @Test
    fun tab_and_open_entry_come_back_after_the_app_dies() = runComposeUiTest {
        rows = listOf(fakeEntryRow("a", day))
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var running by mutableStateOf(true)
        setContent {
            if (running) CompositionLocalProvider(LocalSaveableStateRegistry provides registry) { TwoPanes() }
        }
        waitForIdle()
        openRowA()
        waitForIdle()
        assertEquals(0, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
        onAllNodes(tabs)[1].performClick()
        waitForIdle()

        val saved = registry.performSave()
        running = false
        waitForIdle()
        registry = SaveableStateRegistry(saved) { true }
        running = true
        waitForIdle()

        onAllNodes(tabs)[1].assertIsSelected()
        onAllNodes(tabs)[0].performClick()
        waitForIdle()
        assertEquals(0, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
    }

    /** 두 칸에서 보던 일기는 다른 탭에 다녀와도 오른쪽에 그대로 있음 */
    @Test
    fun two_panes_keep_the_open_entry_across_tabs() = runComposeUiTest {
        rows = listOf(fakeEntryRow("a", day))
        setContent { TwoPanes() }
        waitForIdle()
        openRowA()
        waitForIdle()

        onAllNodes(tabs)[1].performClick()
        waitForIdle()
        onAllNodes(tabs)[0].performClick()
        waitForIdle()

        assertEquals(0, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
        waitUntil(timeoutMillis = ROW_WAIT_MILLIS) { onAllNodes(hasText("글 a", substring = true)).fetchSemanticsNodes().size == 1 }
    }
}

private val BACK_LABELS = setOf("뒤로 가기", "Back", "戻る")

private const val ROW_WAIT_MILLIS = 5_000L
