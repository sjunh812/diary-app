package org.sjhstudio.diary.app.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
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
import org.sjhstudio.diary.app.ads.AdShape
import org.sjhstudio.diary.app.ads.AdSlot
import org.sjhstudio.diary.app.navigation.AdvancedStatsKey
import org.sjhstudio.diary.app.navigation.BackupSettingsKey
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.DetailPane
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.FULL_SCREEN
import org.sjhstudio.diary.app.navigation.FontSettingsKey
import org.sjhstudio.diary.app.navigation.FullScreenEntry
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.InsightKey
import org.sjhstudio.diary.app.navigation.ListDetailSceneStrategy
import org.sjhstudio.diary.app.navigation.ListPane
import org.sjhstudio.diary.app.navigation.LocalInListDetail
import org.sjhstudio.diary.app.navigation.LocalRailWidth
import org.sjhstudio.diary.app.navigation.PhotoViewerKey
import org.sjhstudio.diary.app.navigation.PlusKey
import org.sjhstudio.diary.app.navigation.RestorePreviewKey
import org.sjhstudio.diary.app.navigation.Screen
import org.sjhstudio.diary.app.navigation.SearchKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.TAB_ROOT
import org.sjhstudio.diary.app.navigation.TabRoot
import org.sjhstudio.diary.app.navigation.TabScreen
import org.sjhstudio.diary.app.navigation.WriteKey
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.detail_pick_hint
import org.sjhstudio.diary.app.resources.detail_trashed
import org.sjhstudio.diary.app.resources.detail_trashed_undo
import org.sjhstudio.diary.app.resources.tab_calendar
import org.sjhstudio.diary.app.resources.tab_home
import org.sjhstudio.diary.app.resources.tab_insight
import org.sjhstudio.diary.app.resources.tab_settings
import org.sjhstudio.diary.app.resources.write_draft_kept
import org.sjhstudio.diary.app.resources.write_new
import org.sjhstudio.diary.app.resources.write_saved
import org.sjhstudio.diary.app.settings.settingsEntries
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryNavItem
import org.sjhstudio.diary.core.designsystem.component.DiaryNavigationBar
import org.sjhstudio.diary.core.designsystem.component.DiaryNavigationRail
import org.sjhstudio.diary.core.designsystem.component.DiaryRailMinWidth
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastResult
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryWriteButton
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.backup.BackupFoundPrompt
import org.sjhstudio.diary.feature.backup.BackupNudgeCard
import org.sjhstudio.diary.feature.backup.BackupStatusLine
import org.sjhstudio.diary.feature.calendar.CalendarRoute
import org.sjhstudio.diary.feature.home.EntryDetailRoute
import org.sjhstudio.diary.feature.home.HomeRoute
import org.sjhstudio.diary.feature.home.PhotoViewerRoute
import org.sjhstudio.diary.feature.insight.AdvancedStatsRoute
import org.sjhstudio.diary.feature.insight.InsightRoute
import org.sjhstudio.diary.feature.search.SearchRoute
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
 * 쓰기 버튼은 홈·달력·돌아보기에 뜨고 레일에는 늘 있다. 일기가 없는 홈은 빈 상태의 버튼 하나만 둔다. 쓰기 화면은 탭과 레일 없이 전체 화면이다.
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
    val onWrite: () -> Unit = { backStack.push(WriteKey()) }
    val writeLabel = stringResource(Res.string.write_new)
    // 앱 틀 전체에 하나 두는 토스트. 지금은 "쓰던 글꼴 대신 ○○ 글꼴로 보여 드려요"만 쓴다.
    val toast = remember { DiaryToastState() }
    FontReplacedNotice(toast) {
        backStack.selectTab(SettingsKey)
        backStack.push(FontSettingsKey)
    }
    // 쓰기, 사진 크게 보기, 결제는 전체 화면이다. 탭과 레일을 그리지 않는다.
    val writing = backStack.lastOrNull() is WriteKey
    val fullScreen = writing || backStack.lastOrNull() is PhotoViewerKey || backStack.lastOrNull() is PlusKey
    // 달을 읽기 전에는 일기가 있다고 보고 버튼을 둔다. 일기가 있는 사람에게 버튼이 늦게 뜨면 안 된다.
    val entries = koinInject<EntryRepository>()
    val months by remember(entries) { entries.months() }.collectAsStateWithLifecycle(null)
    val emptyHome = backStack.lastOrNull() == HomeKey && months?.isEmpty() == true
    val floatingWrite = backStack.size == 1 && backStack.lastOrNull() != SettingsKey && !emptyHome

    BoxWithConstraints(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) {
        val wide = maxWidth >= DiaryRailMinWidth
        val twoPane = maxWidth >= DiaryWriteSize.twoPaneMin
        // 키 입력은 포커스가 있는 곳으로만 간다. 앱을 막 열면 포커스가 없어서 틀이 받아 둔다.
        val keyTarget = remember { FocusRequester() }
        val top = backStack.lastOrNull()
        LaunchedEffect(top) {
            if (!writing && top !is SearchKey) keyTarget.requestFocus()
        }
        val shortcuts =
            Modifier.diaryShortcuts(
                onNewEntry = { if (!writing) onWrite() },
                onSearch = { if (!fullScreen && backStack.none { it is SearchKey }) backStack.push(SearchKey()) }
            ).focusRequester(keyTarget).focusTarget()
        // 화면 칸은 폭이 바뀌어도 같은 자리에 둔다. 갈라 두면 접거나 돌릴 때 쓰던 화면이 새로 만들어진다.
        // 레일과 탭 바는 위에 얹는다. 쓰기로 넘어갈 때 사라져도 지난 화면이 밀리지 않는다.
        val density = LocalDensity.current
        var railWidth by remember { mutableStateOf(Dp.Hairline) }
        var barHeight by remember { mutableStateOf(Dp.Hairline) }
        val tabsShown = !wide && backStack.size <= 1
        val barInset = if (tabsShown) barHeight else Dp.Hairline
        // 토스트가 떠 있는 동안 쓰기 버튼을 그 위로 올린다. 토스트는 제자리에 뜨고 버튼은 가리지 않는다.
        var toastHeight by remember { mutableStateOf(Dp.Hairline) }
        val reduceMotion = rememberReduceMotion()
        val writeLift by animateDpAsState(
            if (toast.current != null) toastHeight + DiarySpacing.x2 else Dp.Hairline,
            tween(if (reduceMotion) 0 else DiaryMotion.QUICK_MILLIS)
        )
        Box(Modifier.fillMaxSize().then(shortcuts)) {
            CompositionLocalProvider(
                LocalRailWidth provides if (wide) railWidth else Dp.Hairline,
                LocalTabBarHeight provides if (wide) Dp.Hairline else barHeight
            ) {
                ContentWithToast(
                    backStack,
                    toast,
                    twoPane,
                    onWrite,
                    Modifier.fillMaxSize(),
                    bottomInset = barInset,
                    onToastHeight = { toastHeight = it }
                )
            }
            if (wide && !fullScreen) {
                Box(Modifier.onSizeChanged { railWidth = with(density) { it.width.toDp() } }) {
                    DiaryNavigationRail(items, selected, onSelect, header = {
                        DiaryWriteButton(writeLabel, onWrite, Modifier.padding(bottom = DiarySpacing.x8))
                    })
                }
            }
            if (!wide && floatingWrite) {
                DiaryWriteButton(
                    writeLabel,
                    onWrite,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = DiarySpacing.screenCompact, bottom = DiarySpacing.x4 + barInset)
                        .offset { IntOffset(0, -writeLift.roundToPx()) }
                )
            }
            if (tabsShown) {
                Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = with(density) { it.height.toDp() } }) {
                    DiaryNavigationBar(items, selected, onSelect)
                }
            }
        }
    }
}

/** 위에 얹은 탭 바의 높이. 탭 화면은 전환 중에도 이 높이만큼 비워 둔다. */
private val LocalTabBarHeight = compositionLocalOf { Dp.Hairline }

/**
 * 탭 맨 위 화면은 탭 바 자리를, 전체 화면이 아니면 레일 자리를 비운다. 쓰기처럼 전체를 쓰는 화면은 그대로 둔다.
 * 탭 바가 시스템 내비게이션 높이까지 맡으니 그만큼 다 쓴 것으로 알린다.
 * 안쪽 화면은 탭 바가 없는 넓은 화면에서만 시스템 내비게이션 높이를 비우게 된다.
 */
private val TabBarSpace =
    NavEntryDecorator<NavKey>(decorate = { entry ->
        val bottom = if (entry.metadata[TAB_ROOT] == true) LocalTabBarHeight.current else Dp.Hairline
        val start = if (entry.metadata[FULL_SCREEN] == true || LocalInListDetail.current) Dp.Hairline else LocalRailWidth.current
        Box(Modifier.fillMaxSize().padding(start = start, bottom = bottom).consumeWindowInsets(PaddingValues(bottom = bottom))) {
            entry.Content()
        }
    })

/** 화면을 닫고 나서 앱 틀이 띄우는 알림. */
private enum class Message { Saved, DraftKept }

/** 토스트는 탭과 레일 위, 화면 아래쪽에 뜬다(`docs/design.md` 토스트). */
@Composable
private fun ContentWithToast(
    backStack: NavBackStack<NavKey>,
    toast: DiaryToastState,
    twoPane: Boolean,
    onWrite: () -> Unit,
    modifier: Modifier,
    bottomInset: Dp = Dp.Hairline,
    onToastHeight: (Dp) -> Unit = {}
) {
    Box(modifier) {
        val saved = stringResource(Res.string.write_saved)
        val draftKept = stringResource(Res.string.write_draft_kept)
        val trashed = stringResource(Res.string.detail_trashed)
        val undo = stringResource(Res.string.detail_trashed_undo)
        val entries = koinInject<EntryRepository>()
        val scope = rememberCoroutineScope()
        Content(
            backStack,
            twoPane,
            onWrite,
            Modifier.fillMaxSize(),
            notify = { message -> scope.launch { toast.show(if (message == Message.Saved) saved else draftKept) } },
            onTrashed = { id ->
                // 알림이 사라진 뒤에도 앱 틀은 그대로라 화면이 닫혀도 되돌릴 수 있다.
                scope.launch { if (toast.show(trashed, undo) == DiaryToastResult.ActionPerformed) entries.restore(id) }
            }
        )
        val density = LocalDensity.current
        // 탭 바 높이에는 시스템 내비게이션 높이가 이미 들어 있다. 또 더하면 그만큼 높이 뜬다.
        DiaryToastHost(
            toast,
            Modifier
                .align(Alignment.BottomCenter)
                .then(if (bottomInset > Dp.Hairline) Modifier else Modifier.navigationBarsPadding())
                .padding(start = LocalRailWidth.current, bottom = DiarySpacing.x4 + bottomInset)
                .onSizeChanged { if (it.height > 0) onToastHeight(with(density) { it.height.toDp() }) }
        )
    }
}

/** 화면을 연다. 연타로 같은 화면이 두 번 쌓이지 않게 맨 위와 같으면 그대로 둔다. */
private fun NavBackStack<NavKey>.push(key: NavKey) {
    if (lastOrNull() != key) add(key)
}

/** 탭을 누르면 그 탭의 맨 위 화면으로 간다. 지금 탭을 다시 누르면 맨 위로 돌아간다. */
private fun NavBackStack<NavKey>.selectTab(tab: TabScreen) {
    clear()
    add(tab)
}

private fun SearchKey.toFilter() = SearchFilter(
    moods = moods.mapNotNullTo(HashSet()) { Mood.fromId(it) },
    period = if (from != null && to != null) DateRange(LocalDate.parse(from), LocalDate.parse(to)) else null
)

/** 상세를 연다. 넓은 화면에서 다른 줄을 고르면 상세가 쌓이지 않고 바뀐다. */
private fun NavBackStack<NavKey>.openEntry(entryId: String) {
    if (lastOrNull() is EntryDetailKey) removeAt(lastIndex)
    add(EntryDetailKey(entryId))
}

@Composable
private fun Content(
    backStack: NavBackStack<NavKey>,
    twoPane: Boolean,
    onWrite: () -> Unit,
    modifier: Modifier,
    notify: (Message) -> Unit,
    onTrashed: (String) -> Unit
) {
    val pop: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    // 보는 화면이 바뀔 때마다 화면 키 이름만 보낸다(contracts/app-shell.md 사용 통계 고정 표).
    val telemetry = koinInject<Telemetry>()
    val top = backStack.lastOrNull()
    LaunchedEffect(top) { if (top != null) telemetry.log(AnalyticsEvents.SCREEN_VIEW, mapOf("screen" to top.toString())) }
    // 저장 뒤 상세에서 목록으로 돌아올 때 하루 한 번 전면 광고를 띄운다. 저장은 이미 끝나 있다(FR-010).
    val ads = koinInject<AdRepository>()
    val adScope = rememberCoroutineScope()
    var adAfterSave by remember { mutableStateOf(false) }
    LaunchedEffect(top, adAfterSave) {
        if (adAfterSave && backStack.size <= 1) {
            adAfterSave = false
            // 값이 바뀌면 이 효과가 다시 시작되니 광고는 앱 틀의 범위에서 띄운다.
            adScope.launch { ads.backToList() }
        }
    }
    val listDetail = remember(twoPane) { ListDetailSceneStrategy(twoPane) { PickHint() } }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = pop,
        sceneStrategies = listOf(listDetail),
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator(), TabBarSpace),
        entryProvider =
            entryProvider {
                entry<HomeKey>(metadata = ListPane + TabRoot) {
                    HomeRoute(
                        backupLine = {
                            BackupStatusLine(onOpen = {
                                backStack.selectTab(SettingsKey)
                                backStack.push(BackupSettingsKey)
                            })
                            BackupFoundPrompt(onRestoreEntry = { backStack.push(RestorePreviewKey(it)) })
                        },
                        listTop = { BackupNudgeCard() },
                        onOpen = backStack::openEntry,
                        onWrite = onWrite,
                        onSearch = { backStack.push(SearchKey()) },
                        onEdit = { backStack.push(WriteKey(entryId = it)) },
                        onTrashed = { id ->
                            // 두 칸에서 그 일기가 열려 있으면 닫는다.
                            backStack.removeAll { it is EntryDetailKey && it.entryId == id }
                            onTrashed(id)
                        },
                        selectedId = backStack.filterIsInstance<EntryDetailKey>().lastOrNull()?.entryId?.takeIf { twoPane },
                        // 같은 자리에서 얇은 줄과 카드를 번갈아 쓴다. 광고 수는 늘지 않는다.
                        adSlot = { order -> AdSlot("home-$order", if (order % 2 == 0) AdShape.Row else AdShape.Card) }
                    )
                }
                entry<SearchKey>(clazzContentKey = { "search:${it.moods}:${it.from}:${it.to}" }, metadata = ListPane) { key ->
                    SearchRoute(
                        initial = key.toFilter(),
                        selectedId = backStack.filterIsInstance<EntryDetailKey>().lastOrNull()?.entryId?.takeIf { twoPane },
                        onBack = pop,
                        onOpen = backStack::openEntry
                    )
                }
                // 화면 키의 문자열에는 일기 ID를 담지 않으니, 저장 자리 구분은 ID로 따로 준다.
                entry<EntryDetailKey>(clazzContentKey = { it.entryId }, metadata = DetailPane) { key ->
                    EntryDetailRoute(
                        entryId = key.entryId,
                        onBack = pop.takeUnless { twoPane },
                        onEdit = { backStack.push(WriteKey(entryId = key.entryId)) },
                        onPhoto = { backStack.push(PhotoViewerKey(key.entryId, it)) },
                        onOtherVersion = backStack::openEntry,
                        onTrashed = {
                            backStack.remove(key)
                            onTrashed(key.entryId)
                        }
                    )
                }
                entry<PhotoViewerKey>(clazzContentKey = { "${it.entryId}/${it.index}" }, metadata = FullScreenEntry) { key ->
                    PhotoViewerRoute(key.entryId, key.index, onClose = pop)
                }
                entry<CalendarKey>(metadata = TabRoot) {
                    CalendarRoute(
                        twoPane = twoPane,
                        onOpen = backStack::openEntry,
                        onWriteOn = { backStack.push(WriteKey(date = it.toString())) }
                    )
                }
                entry<InsightKey>(metadata = TabRoot) {
                    InsightRoute(
                        onMood = { mood, range ->
                            backStack.push(SearchKey(listOf(mood.id), range?.start?.toString(), range?.end?.toString()))
                        },
                        onPlus = { backStack.push(PlusKey) },
                        onAdvanced = { backStack.push(AdvancedStatsKey) },
                        adSlot = { AdSlot("insight", AdShape.Card) }
                    )
                }
                entry<AdvancedStatsKey> {
                    // 구독이 끝난 채 앱이 이 화면을 되살려도 머물지 않고 돌아보기로 돌아간다.
                    val plan by koinInject<PlanRepository>().plan.collectAsStateWithLifecycle()
                    LaunchedEffect(plan) { if (!plan.advancedStats) pop() }
                    AdvancedStatsRoute(
                        onBack = pop,
                        onMood = { mood, range ->
                            backStack.push(SearchKey(listOf(mood.id), range?.start?.toString(), range?.end?.toString()))
                        }
                    )
                }
                settingsEntries(push = { backStack.push(it) }, pop = pop, home = { backStack.selectTab(HomeKey) })
                entry<WriteKey>(clazzContentKey = { it.contentKey }, metadata = FullScreenEntry) { key ->
                    val target = key.entryId?.let { WriteTarget.Edit(it) } ?: WriteTarget.New(key.date?.let(LocalDate::parse))
                    WriteRoute(
                        target = target,
                        onSaved = { id ->
                            pop()
                            // 고치기는 보던 상세로 돌아가고, 새 일기는 그 일기의 상세로 간다(FR-012).
                            if ((backStack.lastOrNull() as? EntryDetailKey)?.entryId != id) backStack.openEntry(id)
                            notify(Message.Saved)
                            adAfterSave = true
                            adScope.launch { ads.afterSave() }
                        },
                        onClosed = { kept ->
                            pop()
                            if (kept) notify(Message.DraftKept)
                        },
                        onPlus = { backStack.push(PlusKey) }
                    )
                }
            }
    )
}

/** 두 칸에서 고른 일기가 없을 때의 오른쪽. 일기가 아직 없어도 칸이 통째로 비지 않게 늘 보인다. */
@Composable
private fun PickHint() {
    DiaryEmptyArea(Modifier.background(DiaryTheme.colors.canvas)) {
        DiaryEmptyState(DiaryIcons.Pen, stringResource(Res.string.detail_pick_hint))
    }
}
