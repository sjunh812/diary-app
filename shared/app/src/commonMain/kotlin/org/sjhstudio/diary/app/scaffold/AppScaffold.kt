package org.sjhstudio.diary.app.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.FontSettingsKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.InsightKey
import org.sjhstudio.diary.app.navigation.Screen
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.TabScreen
import org.sjhstudio.diary.app.navigation.WriteKey
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.tab_calendar
import org.sjhstudio.diary.app.resources.tab_home
import org.sjhstudio.diary.app.resources.tab_insight
import org.sjhstudio.diary.app.resources.tab_settings
import org.sjhstudio.diary.app.resources.write_draft_kept
import org.sjhstudio.diary.app.resources.write_new
import org.sjhstudio.diary.app.resources.write_saved
import org.sjhstudio.diary.app.settings.settingsEntries
import org.sjhstudio.diary.core.designsystem.component.DiaryNavItem
import org.sjhstudio.diary.core.designsystem.component.DiaryNavigationBar
import org.sjhstudio.diary.core.designsystem.component.DiaryNavigationRail
import org.sjhstudio.diary.core.designsystem.component.DiaryRailMinWidth
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryWriteButton
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.feature.settings.FontReplacedNotice
import org.sjhstudio.diary.feature.write.WriteRoute
import org.sjhstudio.diary.feature.write.WriteTarget

private val TABS: List<TabScreen> = listOf(HomeKey, CalendarKey, InsightKey, SettingsKey)

private val BackStackConfiguration =
    SavedStateConfiguration {
        serializersModule = SerializersModule { polymorphic(NavKey::class) { subclassesOfSealed<Screen>() } }
    }

/**
 * 앱 틀(contracts/app-shell.md, `docs/design.md` 넓은 화면). 폭 600dp 미만은 하단 탭, 이상은 레일이다.
 * 탭은 홈·달력·돌아보기·설정이고, 하단 탭은 탭의 맨 위 화면에서만 보인다(Figma S6·G2).
 * 쓰기 버튼은 홈·달력·돌아보기에 뜨고 레일에는 늘 있다. 쓰기 화면은 탭과 레일 없이 전체 화면이다.
 * 잠금·이전 관문은 이 바깥에 있어 뒤로 가기로 벗어날 수 없다.
 */
@Composable
fun AppScaffold(startTab: TabScreen = HomeKey) {
    val backStack = rememberNavBackStack(BackStackConfiguration, startTab)
    val items =
        listOf(
            DiaryNavItem(stringResource(Res.string.tab_home), DiaryIcons.TabHome, DiaryIcons.TabHomeSelected),
            DiaryNavItem(stringResource(Res.string.tab_calendar), DiaryIcons.TabCalendar, DiaryIcons.TabCalendarSelected),
            DiaryNavItem(stringResource(Res.string.tab_insight), DiaryIcons.TabInsight, DiaryIcons.TabInsightSelected),
            DiaryNavItem(stringResource(Res.string.tab_settings), DiaryIcons.TabSettings, DiaryIcons.TabSettingsSelected)
        )
    val selected = TABS.indexOf(backStack.firstOrNull()).coerceAtLeast(0)
    val onSelect = { index: Int -> backStack.selectTab(TABS[index]) }
    val onWrite: () -> Unit = { backStack.add(WriteKey()) }
    val writeLabel = stringResource(Res.string.write_new)
    // 앱 틀 전체에 하나 두는 토스트. 지금은 "쓰던 글꼴 대신 ○○ 글꼴로 보여 드려요"만 쓴다.
    val toast = remember { DiaryToastState() }
    FontReplacedNotice(toast) {
        backStack.selectTab(SettingsKey)
        backStack.add(FontSettingsKey)
    }
    // 쓰기는 전체 화면이다. 탭과 레일을 그리지 않는다.
    val writing = backStack.lastOrNull() is WriteKey
    val floatingWrite = backStack.size == 1 && backStack.lastOrNull() != SettingsKey

    BoxWithConstraints(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) {
        val wide = maxWidth >= DiaryRailMinWidth
        val shortcuts = Modifier.diaryShortcuts(onNewEntry = { if (!writing) onWrite() })
        if (wide) {
            Row(Modifier.fillMaxSize().then(shortcuts)) {
                if (!writing) {
                    DiaryNavigationRail(items, selected, onSelect, header = {
                        DiaryWriteButton(writeLabel, onWrite, Modifier.padding(bottom = DiarySpacing.x8))
                    })
                }
                ContentWithToast(backStack, toast, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxSize().then(shortcuts)) {
                Box(Modifier.weight(1f)) {
                    ContentWithToast(backStack, toast, Modifier.fillMaxSize(), aboveWriteButton = floatingWrite)
                    if (floatingWrite) {
                        DiaryWriteButton(
                            writeLabel,
                            onWrite,
                            Modifier.align(Alignment.BottomEnd).padding(end = DiarySpacing.screenCompact, bottom = DiarySpacing.x4)
                        )
                    }
                }
                if (backStack.size <= 1) DiaryNavigationBar(items, selected, onSelect)
            }
        }
    }
}

/** 화면을 닫고 나서 앱 틀이 띄우는 알림. */
private enum class Message { Saved, DraftKept }

/** 토스트는 탭과 레일 위, 화면 아래쪽에 뜬다(`docs/design.md` 토스트). */
@Composable
private fun ContentWithToast(
    backStack: NavBackStack<NavKey>,
    toast: DiaryToastState,
    modifier: Modifier,
    aboveWriteButton: Boolean = false
) {
    Box(modifier) {
        val saved = stringResource(Res.string.write_saved)
        val draftKept = stringResource(Res.string.write_draft_kept)
        val scope = rememberCoroutineScope()
        Content(backStack, Modifier.fillMaxSize()) { message ->
            scope.launch { toast.show(if (message == Message.Saved) saved else draftKept) }
        }
        val bottom = if (aboveWriteButton) DiarySpacing.x4 + DiarySpacing.x14 + DiarySpacing.x2 else DiarySpacing.x6
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom))
    }
}

/** 탭을 누르면 그 탭의 맨 위 화면으로 간다. 지금 탭을 다시 누르면 맨 위로 돌아간다. */
private fun NavBackStack<NavKey>.selectTab(tab: TabScreen) {
    clear()
    add(tab)
}

@Composable
private fun Content(backStack: NavBackStack<NavKey>, modifier: Modifier, notify: (Message) -> Unit) {
    val pop: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    // 보는 화면이 바뀔 때마다 화면 키 이름만 보낸다(contracts/app-shell.md 사용 통계 고정 표).
    val telemetry = koinInject<Telemetry>()
    val top = backStack.lastOrNull()
    LaunchedEffect(top) { if (top != null) telemetry.log(AnalyticsEvents.SCREEN_VIEW, mapOf("screen" to top.toString())) }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = pop,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider =
            entryProvider {
                entry<HomeKey> { EmptyTab() }
                entry<CalendarKey> { EmptyTab() }
                entry<InsightKey> { EmptyTab() }
                settingsEntries(push = { backStack.add(it) }, pop = pop)
                entry<WriteKey> { key ->
                    val target = key.entryId?.let { WriteTarget.Edit(it) } ?: WriteTarget.New(key.date?.let(LocalDate::parse))
                    WriteRoute(
                        target = target,
                        onSaved = {
                            pop()
                            notify(Message.Saved)
                        },
                        onClosed = { kept ->
                            pop()
                            if (kept) notify(Message.DraftKept)
                        }
                    )
                }
            }
    )
}

@Composable
private fun EmptyTab() {
    Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas))
}
