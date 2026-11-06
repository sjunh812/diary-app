package org.sjhstudio.diary.app.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
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
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.feature.backup.backupFeatureModule
import org.sjhstudio.diary.feature.calendar.CalendarViewModel
import org.sjhstudio.diary.feature.home.HomeViewModel
import org.sjhstudio.diary.feature.settings.FontReplacedNoticeViewModel

// 앱 틀(스펙 001 T084, contracts/app-shell.md, docs/design.md 넓은 화면).
// 폭 600dp 미만은 하단 탭, 이상은 레일이고, 글자 배율 2.0에서도 탭 칸끼리 겹치거나 글자가 칸 밖으로 나가지 않는다.
private object NoFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

@OptIn(ExperimentalTestApi::class)
class AppScaffoldUiTest {
    private val sink = RecordingSink()
    private var rows: List<EntryRow> = emptyList()
    private val homeModels = mutableListOf<HomeViewModel>()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                backupFeatureModule,
                module {
                    single<PlanRepository> { FreePlanRepository() }
                    single { AdRepository(get(), AdStore(InMemoryDataStore(emptyPreferences())), NoAdPlayer) }
                    single<BackupRepository> { FakeBackupRepository() }
                    single<RestoreRepository> { FakeRestoreRepository() }
                    single<WelcomeRepository> { DefaultWelcomeRepository(WelcomeStore(InMemoryDataStore(emptyPreferences()))) { null } }
                    single { Telemetry(sink, strict = true) }
                    viewModel { FontReplacedNoticeViewModel(FakeSettingsRepository()) }
                    single<EntryRepository> { FakeEntryRepository(rows = rows) }
                    viewModel { HomeViewModel(get(), FakeSettingsRepository(), NoFonts).also { homeModels += it } }
                    viewModel { CalendarViewModel(get(), FakeSettingsRepository(), NoFonts) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun phone_width_shows_bottom_tabs() = shell(width = 400.dp) {
        val tabs = tabBounds()
        assertEquals(4, tabs.size)
        // 한 줄로 나란하고 화면 아래에 붙어 있다.
        assertTrue(tabs.all { it.top == tabs.first().top }, "$tabs")
        assertTrue(tabs.zipWithNext().all { (a, b) -> a.right <= b.left }, "$tabs")
        assertTrue(tabs.first().bottom > HEIGHT - 72.dp, "${tabs.first()}")
    }

    @Test
    fun just_below_600_is_still_bottom_tabs() = shell(width = 599.dp) {
        val tabs = tabBounds()
        assertTrue(tabs.all { it.top == tabs.first().top }, "$tabs")
    }

    @Test
    fun from_600_the_tabs_move_to_a_rail() {
        for (width in listOf(600.dp, 700.dp, 1000.dp)) {
            shell(width = width) {
                val tabs = tabBounds()
                assertEquals(4, tabs.size)
                // 왼쪽 80 안에 위에서 아래로 쌓인다.
                assertTrue(tabs.all { it.left == tabs.first().left && it.right <= 80.dp }, "$width $tabs")
                assertTrue(tabs.zipWithNext().all { (a, b) -> a.bottom <= b.top }, "$width $tabs")
            }
        }
    }

    @Test
    fun large_text_keeps_tabs_apart_in_light_and_dark() {
        for (dark in listOf(false, true)) {
            for (width in listOf(400.dp, 700.dp)) {
                shell(width = width, fontScale = 2f, dark = dark) {
                    val tabs = tabBounds()
                    assertTrue(tabs.zipWithNext().all { (a, b) -> !a.overlaps(b) }, "$width dark=$dark $tabs")
                    // 이름 글자가 제 칸 밖으로 나가지 않는다.
                    val labels = onAllNodes(hasAnyAncestor(isSelectable()) and hasAnyText(), useUnmergedTree = true).bounds()
                    labels.forEach { label ->
                        assertTrue(tabs.any { it.contains(label) }, "$width dark=$dark label=$label tabs=$tabs")
                    }
                    // 칸에 맞춰 잘린 글자도 없다.
                    // 잘리면 칸 안에 있어도 글자가 다 보이지 않는다.
                    val clipped = labelLayouts().filter { it.isClipped() }.map { it.layoutInput.text.text }
                    assertEquals(emptyList(), clipped, "$width dark=$dark")
                }
            }
        }
    }

    @Test
    fun choosing_a_tab_selects_it_and_logs_only_the_screen_key() = shell(width = 400.dp) {
        // 달력을 열면 날짜 칸도 고를 수 있는 항목이라 탭만 골라낸다.
        val tabs = isSelectable() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        onAllNodes(tabs)[1].performClick()
        waitForIdle()
        onAllNodes(tabs)[1].assertIsSelected()
        assertEquals(
            listOf("HomeKey", "CalendarKey"),
            sink.events.filter { it.first == AnalyticsEvents.SCREEN_VIEW }.map { it.second.getValue("screen") }
        )
    }

    /** 탭을 옮겼다 와도 화면 모델이 살아 있어 필터가 그대로다. */
    @Test
    fun a_tab_keeps_its_screen_model_while_you_are_away() = shell(width = 400.dp) {
        onNode(favoritesSwitch).performClick()
        waitForIdle()
        assertTrue(homeModels.single().favoritesOnly.value)

        tab(1)
        waitForIdle()
        tab(0)
        waitForIdle()

        assertEquals(1, homeModels.size)
        assertTrue(homeModels.single().favoritesOnly.value)
    }

    /** 탭을 옮길 때 두 화면이 겹쳐 보이지 않는다. */
    @Test
    fun changing_tabs_swaps_screens_at_once() = shell(width = 400.dp) {
        onAllNodes(favoritesSwitch).assertCountEquals(1)

        mainClock.autoAdvance = false
        tab(1)
        var frames = 0
        while (onAllNodes(favoritesSwitch).fetchSemanticsNodes().isNotEmpty() && frames < CROSS_FADE_FRAMES) {
            mainClock.advanceTimeByFrame()
            frames++
        }
        mainClock.autoAdvance = true

        assertTrue(frames <= FRAMES_TO_SETTLE, "탭을 바꾸는 데 $frames 프레임")
    }

    /** 홈을 내렸다가 달력에 다녀와도 보던 자리고, 뼈대를 거치지 않는다. */
    @Test
    fun home_comes_back_where_you_left_it() {
        rows = (0 until ROW_COUNT).map { row(it) }
        shell(width = 400.dp) {
            onNode(hasScrollToIndexAction()).performScrollToIndex(ROW_COUNT - 1)
            waitForIdle()
            onNode(hasText(preview(ROW_COUNT - 1), substring = true)).assertExists()

            tab(1)
            waitForIdle()
            mainClock.autoAdvance = false
            tab(0)
            repeat(FRAMES_TO_SETTLE) { mainClock.advanceTimeByFrame() }

            onNode(hasText(preview(ROW_COUNT - 1), substring = true)).assertExists()
            onAllNodes(hasText(preview(0), substring = true)).assertCountEquals(0)
            mainClock.autoAdvance = true
        }
    }

    private fun row(index: Int) = fakeEntryRow("e$index", LocalDate(2026, 10, 1), preview(index))

    private fun preview(index: Int) = "줄-$index-끝"

    private val favoritesSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    private fun ComposeUiTest.tab(index: Int) {
        // 달력을 열면 날짜 칸도 고를 수 있는 항목이라 탭만 골라낸다.
        onAllNodes(isSelectable() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[index].performClick()
    }

    private fun shell(width: Dp, fontScale: Float = 1f, dark: Boolean = false, block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DiaryTheme(darkTheme = dark) {
                    Box(Modifier.size(width, HEIGHT).testTag(FRAME)) { AppScaffold() }
                }
            }
        }
        waitForIdle()
        assertEquals(1, onAllNodesWithTag(FRAME).fetchSemanticsNodes().size)
        onNodeWithTag(FRAME)
        block()
    }

    private fun ComposeUiTest.tabBounds(): List<DpRect> = onAllNodes(isSelectable()).bounds()

    private fun SemanticsNodeInteractionCollection.bounds(): List<DpRect> =
        List(fetchSemanticsNodes().size) { index -> get(index).getUnclippedBoundsInRoot() }

    private fun hasAnyText() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

    private fun ComposeUiTest.labelLayouts(): List<TextLayoutResult> =
        onAllNodes(hasAnyAncestor(isSelectable()) and hasAnyText(), useUnmergedTree = true).fetchSemanticsNodes().map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single()
        }

    // 가운데 맞춘 한 줄 글자는 칸 폭으로 배치되어 `hasVisualOverflow`가 늘 참이다.
    // 높이와 글자 제 폭으로 본다.
    private fun TextLayoutResult.isClipped() = didOverflowHeight || multiParagraph.maxIntrinsicWidth > size.width + 0.5f

    private fun DpRect.overlaps(other: DpRect) = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    private fun DpRect.contains(inner: DpRect) = inner.left >= left && inner.right <= right && inner.top >= top && inner.bottom <= bottom

    private class RecordingSink : TelemetrySink {
        val events = mutableListOf<Pair<String, Map<String, String>>>()

        override fun log(event: String, params: Map<String, String>) {
            events += event to params
        }

        override fun setCustomKey(key: String, value: String) = Unit
    }

    private companion object {
        const val FRAME = "frame"
        const val FRAMES_TO_SETTLE = 4

        // 기본 전환인 0.7초 겹침.
        const val CROSS_FADE_FRAMES = 42
        const val ROW_COUNT = 40
        val HEIGHT = 800.dp
    }
}
