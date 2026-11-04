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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
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

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { Telemetry(sink, strict = true) }
                    viewModel { FontReplacedNoticeViewModel(FakeSettingsRepository()) }
                    single<EntryRepository> { FakeEntryRepository() }
                    viewModel { HomeViewModel(get(), FakeSettingsRepository(), NoFonts) }
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
                    val labels = onAllNodes(hasAnyAncestor(isSelectable()) and hasText(), useUnmergedTree = true).bounds()
                    labels.forEach { label ->
                        assertTrue(tabs.any { it.contains(label) }, "$width dark=$dark label=$label tabs=$tabs")
                    }
                    // 칸에 맞춰 잘린 글자도 없다. 잘리면 칸 안에 있어도 글자가 다 보이지 않는다.
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

    private fun hasText() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

    private fun ComposeUiTest.labelLayouts(): List<TextLayoutResult> =
        onAllNodes(hasAnyAncestor(isSelectable()) and hasText(), useUnmergedTree = true).fetchSemanticsNodes().map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single()
        }

    // 가운데 맞춘 한 줄 글자는 칸 폭으로 배치되어 `hasVisualOverflow`가 늘 참이다. 높이와 글자 제 폭으로 본다.
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
        val HEIGHT = 800.dp
    }
}
