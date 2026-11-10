package org.sjhstudio.diary.app.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
import org.sjhstudio.diary.app.navigation.FileBackupKey
import org.sjhstudio.diary.app.navigation.FontSettingsKey
import org.sjhstudio.diary.app.navigation.FullScreenEntry
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.InsightKey
import org.sjhstudio.diary.app.navigation.ListDetailSceneStrategy
import org.sjhstudio.diary.app.navigation.ListPane
import org.sjhstudio.diary.app.navigation.LocalInListDetail
import org.sjhstudio.diary.app.navigation.LocalRailWidth
import org.sjhstudio.diary.app.navigation.OpenRequestedScreens
import org.sjhstudio.diary.app.navigation.PhotoViewerKey
import org.sjhstudio.diary.app.navigation.PlusKey
import org.sjhstudio.diary.app.navigation.ROOT_OF
import org.sjhstudio.diary.app.navigation.RestorePreviewKey
import org.sjhstudio.diary.app.navigation.RestoreProgressKey
import org.sjhstudio.diary.app.navigation.Screen
import org.sjhstudio.diary.app.navigation.SearchKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.TABS
import org.sjhstudio.diary.app.navigation.TAB_ROOT
import org.sjhstudio.diary.app.navigation.TabNavigation
import org.sjhstudio.diary.app.navigation.TabScreen
import org.sjhstudio.diary.app.navigation.TabSwap
import org.sjhstudio.diary.app.navigation.WriteKey
import org.sjhstudio.diary.app.navigation.decoratedEntries
import org.sjhstudio.diary.app.navigation.dimmedAlpha
import org.sjhstudio.diary.app.navigation.popTransition
import org.sjhstudio.diary.app.navigation.predictivePopTransition
import org.sjhstudio.diary.app.navigation.pushTransition
import org.sjhstudio.diary.app.navigation.rememberTabNavigation
import org.sjhstudio.diary.app.navigation.tabRoot
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.detail_pick_hint
import org.sjhstudio.diary.app.resources.detail_trashed
import org.sjhstudio.diary.app.resources.detail_trashed_undo
import org.sjhstudio.diary.app.resources.tab_calendar
import org.sjhstudio.diary.app.resources.tab_home
import org.sjhstudio.diary.app.resources.tab_insight
import org.sjhstudio.diary.app.resources.tab_settings
import org.sjhstudio.diary.app.resources.write_draft_kept
import org.sjhstudio.diary.app.resources.write_failed
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
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.LocalTabReselect
import org.sjhstudio.diary.core.ui.WriteFailures
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.backup.BackupAlertButton
import org.sjhstudio.diary.feature.backup.BackupFoundPrompt
import org.sjhstudio.diary.feature.backup.BackupNudgeCard
import org.sjhstudio.diary.feature.backup.BackupStatusLine
import org.sjhstudio.diary.feature.backup.BundleStoppedNotice
import org.sjhstudio.diary.feature.backup.RestoreResultNotice
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

/**
 * 앱 틀(contracts/app-shell.md, `docs/design.md` 넓은 화면).
 * 폭 600dp 미만은 하단 탭, 이상은 레일이다.
 * 탭은 홈·달력·돌아보기·설정이고, 하단 탭은 탭의 맨 위 화면에서만 보인다(Figma S6·G2).
 * 탭마다 화면을 따로 쌓아 옮겼다 와도 보던 자리 그대로다.
 * 쓰기 버튼은 홈·달력·돌아보기에 뜨고 레일에는 늘 있다.
 * 일기가 없는 홈은 빈 상태의 버튼 하나만 둔다.
 * 쓰기 화면은 탭과 레일 없이 전체 화면이다.
 * 잠금·이전 관문은 이 바깥에 있어 뒤로 가기로 벗어날 수 없다.
 */
@Composable
fun AppScaffold(startTab: TabScreen = HomeKey) {
    val nav = rememberTabNavigation(startTab)
    OpenRequestedScreens(koinInject(), nav)
    val items =
        listOf(
            DiaryNavItem(stringResource(Res.string.tab_home), DiaryIcons.TabHome, DiaryIcons.TabHomeSelected),
            DiaryNavItem(stringResource(Res.string.tab_calendar), DiaryIcons.TabCalendar, DiaryIcons.TabCalendarSelected),
            DiaryNavItem(stringResource(Res.string.tab_insight), DiaryIcons.TabInsight, DiaryIcons.TabInsightSelected),
            DiaryNavItem(stringResource(Res.string.tab_settings), DiaryIcons.TabSettings, DiaryIcons.TabSettingsSelected)
        )
    val selected = TABS.indexOf(nav.tab)
    val onSelect = { index: Int -> nav.selectTab(TABS[index]) }
    val onWrite: () -> Unit = { nav.push(WriteKey()) }
    val writeLabel = stringResource(Res.string.write_new)
    // 앱 틀 전체에 하나 두는 토스트.
    // 지금은 "쓰던 글꼴 대신 ○○ 글꼴로 보여 드려요"만 쓴다.
    val toast = remember { DiaryToastState() }
    FontReplacedNotice(toast) { nav.openInTab(SettingsKey, FontSettingsKey) }
    // 쓰기, 사진 크게 보기, 결제는 전체 화면이다.
    // 탭과 레일을 그리지 않는다.
    val writing = nav.top is WriteKey
    val fullScreen = writing || nav.top is PhotoViewerKey || nav.top is PlusKey
    // 파일 불러오기는 일기를 지우지 않으니 다른 탭에 쌓인 화면(쓰던 일기 포함)을 비울 까닭이 없다.
    BundleStoppedNotice(toast) { nav.openInTab(SettingsKey, FileBackupKey) }
    // 복원 중에 화면을 떠나도 결과를 다시 열 수 있게 한다.
    RestoreResultNotice(toast, onResultScreen = nav.top is RestoreProgressKey, quiet = fullScreen) {
        nav.push(RestoreProgressKey)
    }
    // 홈 쓰기 버튼은 일기 유무를 읽은 뒤 표시. 미리 두면 빈 홈에서 보였다 사라짐
    val entries = koinInject<EntryRepository>()
    val months by remember(entries) { entries.months() }.collectAsStateWithLifecycle(null)

    BoxWithConstraints(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) {
        val wide = maxWidth >= DiaryRailMinWidth
        val twoPane = maxWidth >= DiaryWriteSize.twoPaneMin
        // 키 입력은 포커스가 있는 곳으로만 간다.
        // 앱을 막 열면 포커스가 없어서 틀이 받아 둔다.
        val keyTarget = remember { FocusRequester() }
        val top = nav.top
        LaunchedEffect(top) {
            if (!writing && top !is SearchKey) keyTarget.requestFocus()
        }
        val shortcuts =
            Modifier.diaryShortcuts(
                onNewEntry = { if (!writing) onWrite() },
                onSearch = { if (!fullScreen && nav.current.none { it is SearchKey }) nav.push(SearchKey()) }
            ).focusRequester(keyTarget).focusTarget()
        // 화면 칸은 폭이 바뀌어도 같은 자리에 둔다.
        // 갈라 두면 접거나 돌릴 때 쓰던 화면이 새로 만들어진다.
        // 레일과 탭 바는 위에 얹는다.
        // 쓰기로 넘어갈 때 사라져도 지난 화면이 밀리지 않는다.
        val density = LocalDensity.current
        var railWidth by remember { mutableStateOf(Dp.Hairline) }
        var barHeight by remember { mutableStateOf(Dp.Hairline) }
        val tabsShown = !wide && nav.current.size <= 1
        val barInset = if (tabsShown) barHeight else Dp.Hairline
        // 토스트가 떠 있는 동안 쓰기 버튼을 그 위로 올린다.
        // 토스트는 제자리에 뜨고 버튼은 가리지 않는다.
        var toastHeight by remember { mutableStateOf(Dp.Hairline) }
        val reduceMotion = rememberReduceMotion()
        val writeLift by animateDpAsState(
            if (toast.current != null) toastHeight + DiarySpacing.x2 else Dp.Hairline,
            tween(if (reduceMotion) 0 else DiaryMotion.QUICK_MILLIS)
        )
        Box(Modifier.fillMaxSize().then(shortcuts)) {
            CompositionLocalProvider(
                LocalRailWidth provides if (wide) railWidth else Dp.Hairline,
                LocalTabBarHeight provides if (wide) Dp.Hairline else barHeight,
                LocalTabReselect provides nav.reselected,
                LocalRootChrome provides
                    if (wide) {
                        null
                    } else {
                        { root ->
                            val hideOnHome = root == HomeKey && months?.isEmpty() != false
                            if (root != SettingsKey && !hideOnHome) {
                                DiaryWriteButton(
                                    writeLabel,
                                    onWrite,
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = LocalScreenGutter.current, bottom = DiarySpacing.x4 + barHeight)
                                        .offset { IntOffset(0, -writeLift.roundToPx()) }
                                )
                            }
                            Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = with(density) { it.height.toDp() } }) {
                                DiaryNavigationBar(items, TABS.indexOf(root), onSelect)
                            }
                        }
                    }
            ) {
                ContentWithToast(
                    nav,
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
        }
    }
}

/**
 * 위에 얹은 탭 바의 높이.
 * 탭 화면은 전환 중에도 이 높이만큼 비워 둔다.
 */
private val LocalTabBarHeight = compositionLocalOf { Dp.Hairline }

/**
 * 탭 맨 위 화면은 탭 바 자리를, 전체 화면이 아니면 레일 자리를 비운다.
 * 쓰기처럼 전체를 쓰는 화면은 그대로 둔다.
 * 탭 바가 시스템 내비게이션 높이까지 맡으니 그만큼 다 쓴 것으로 알린다.
 * 안쪽 화면은 탭 바가 없는 넓은 화면에서만 시스템 내비게이션 높이를 비우게 된다.
 */
private val TabBarSpace =
    NavEntryDecorator<NavKey>(decorate = { entry ->
        val bottom = if (entry.metadata[TAB_ROOT] == true) LocalTabBarHeight.current else Dp.Hairline
        val start = if (entry.metadata[FULL_SCREEN] == true || LocalInListDetail.current) Dp.Hairline else LocalRailWidth.current
        val chrome = LocalRootChrome.current
        val root = entry.metadata[ROOT_OF] as? TabScreen
        val colors = DiaryTheme.colors
        // 전환 중에는 두 화면 모두 터치 막음. 빠르게 누르면 사라지는 화면이 눌렸음
        val moving = LocalNavAnimatedContentScope.current.transition.isRunning
        // 바탕이 빈 화면은 전환 중 뒤의 검은 바탕이 비쳐 바탕을 깖.
        // 양옆 바깥 선은 전환 중에만 두 화면 사이에 보임(다크에서 두 화면 구분)
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val edge = Dp.Hairline.toPx().coerceAtLeast(1f)
                    drawRect(colors.line, topLeft = Offset(-edge, 0f), size = Size(edge, size.height))
                    drawRect(colors.line, topLeft = Offset(size.width, 0f), size = Size(edge, size.height))
                }.background(colors.canvas)
        ) {
            Box(Modifier.fillMaxSize().padding(start = start, bottom = bottom).consumeWindowInsets(PaddingValues(bottom = bottom))) {
                entry.Content()
            }
            if (root != null) chrome?.invoke(this, root)
            if (moving) {
                Box(
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                    }
                )
            }
        }
    })

/**
 * 탭 첫 화면이 함께 그리는 탭 바와 쓰기 버튼(폰).
 * 화면 위에 따로 얹으면 상세로 넘어갈 때 먼저 사라지거나 새 화면 위에 남음. 넓은 화면은 레일이라 null
 */
private val LocalRootChrome = compositionLocalOf<(@Composable BoxScope.(TabScreen) -> Unit)?> { null }

/**
 * 저장 뒤 전면 광고를 띄워도 되는 자리. 홈과 달력의 목록으로 돌아왔을 때만이다.
 * 넓은 화면은 상세를 보다 레일로 설정에 갈 수 있는데 설정에는 광고가 없다(헌법 III). 다음에 목록으로 갈 때까지 미룬다.
 */
internal fun interstitialMayShow(backStack: List<NavKey>): Boolean =
    backStack.size == 1 && (backStack.last() == HomeKey || backStack.last() == CalendarKey)

/**
 * 저장 뒤 전면 광고를 띄울 차례.
 * 일기를 저장하면 그 탭을 기억하고, 같은 탭의 목록으로 돌아오면 한 번 띄운다.
 * 다른 탭으로 가면 기회를 버린다. 몇 시간 뒤 탭을 누를 때 뜨면 "저장하고 목록으로 돌아갈 때"가 아니다.
 * 하루 한 번인지는 [AdRepository]가 본다.
 */
@Stable
internal class AfterSaveAdTurn {
    /** 기다리는 탭. 화면이 바뀔 때 다시 보도록 상태로 둔다. */
    var savedOn: TabScreen? by mutableStateOf(null)
        private set

    fun saved(tab: TabScreen) {
        savedOn = tab
    }

    /** 화면이 바뀔 때마다 부른다. 참이면 지금 띄우고 차례는 끝난다. */
    fun takeTurn(tab: TabScreen, backStack: List<NavKey>): Boolean {
        val pending = savedOn
        val show = pending == tab && interstitialMayShow(backStack)
        if (pending != tab || show) savedOn = null
        return show
    }
}

/** 화면을 닫고 나서 앱 틀이 띄우는 알림. */
private enum class Message { Saved, DraftKept }

/** 토스트는 탭과 레일 위, 화면 아래쪽에 뜬다(`docs/design.md` 토스트). */
@Composable
private fun ContentWithToast(
    nav: TabNavigation,
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
        val writeFailed = stringResource(Res.string.write_failed)
        LaunchedEffect(toast) { WriteFailures.events.collect { toast.show(writeFailed) } }
        Content(
            nav,
            twoPane,
            onWrite,
            Modifier.fillMaxSize(),
            notify = { message -> scope.launch { toast.show(if (message == Message.Saved) saved else draftKept) } },
            onTrashed = { id ->
                // 알림이 사라진 뒤에도 앱 틀은 그대로라 화면이 닫혀도 되돌릴 수 있다.
                scope.launchWrite { if (toast.show(trashed, undo) == DiaryToastResult.ActionPerformed) entries.restore(id) }
            }
        )
        val density = LocalDensity.current
        // 탭 바 높이에는 시스템 내비게이션 높이가 이미 들어 있다.
        // 또 더하면 그만큼 높이 뜬다.
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

private fun SearchKey.toFilter() = SearchFilter(
    moods = moods.mapNotNullTo(HashSet()) { Mood.fromId(it) },
    period = if (from != null && to != null) DateRange(LocalDate.parse(from), LocalDate.parse(to)) else null
)

@Composable
private fun Content(
    nav: TabNavigation,
    twoPane: Boolean,
    onWrite: () -> Unit,
    modifier: Modifier,
    notify: (Message) -> Unit,
    onTrashed: (String) -> Unit
) {
    val pop: () -> Unit = nav::back
    // 두 칸에서 그 일기가 열려 있으면 닫는다.
    val closeTrashed: (String) -> Unit = { id ->
        nav.closeEntry(id)
        onTrashed(id)
    }
    // 화면 목록은 쌓임이 바뀔 때만 다시 만들어진다.
    // 창 폭은 그 사이에도 바뀌니 값을 잡아 두지 않고 읽는다.
    val splitWindow by rememberUpdatedState(twoPane)
    // 보는 화면이 바뀔 때마다 화면 키 이름만 보낸다(contracts/app-shell.md 사용 통계 고정 표).
    val telemetry = koinInject<Telemetry>()
    val top = nav.top
    LaunchedEffect(top) { if (top != null) telemetry.screen(top.toString()) }
    // 저장 뒤 상세에서 목록으로 돌아올 때 하루 한 번 전면 광고를 띄운다.
    // 저장은 이미 끝나 있다(FR-010).
    val ads = koinInject<AdRepository>()
    val adScope = rememberCoroutineScope()
    val adTurn = remember { AfterSaveAdTurn() }
    LaunchedEffect(top, adTurn.savedOn) {
        // 값이 바뀌면 이 효과가 다시 시작되니 광고는 앱 틀의 범위에서 띄운다.
        if (adTurn.savedOn != null && adTurn.takeTurn(nav.tab, nav.current)) adScope.launch { ads.backToList() }
    }
    val listDetail = remember(twoPane) { ListDetailSceneStrategy(twoPane) { PickHint() } }
    val homeStack = nav.stacks.getValue(HomeKey)
    val dimmed = dimmedAlpha(DiaryTheme.colors.canvas.luminance() < HALF)
    val pushMotion = remember(dimmed) { pushTransition<NavKey>(dimmed) }
    val popMotion = remember(dimmed) { popTransition<NavKey>(dimmed) }
    val predictivePopMotion = remember(dimmed) { predictivePopTransition<NavKey>(dimmed) }
    // 동작 줄이기를 켠 사람에게는 밀어 들어오는 전환을 쓰지 않는다.
    val reduceMotion = rememberReduceMotion()
    NavDisplay(
        entries =
            nav.decoratedEntries(
                TabBarSpace,
                entryProvider {
                    entry<HomeKey>(metadata = ListPane + tabRoot(HomeKey)) {
                        HomeRoute(
                            backupLine = {
                                BackupStatusLine(onOpen = {
                                    nav.openTab(SettingsKey)
                                    nav.push(BackupSettingsKey)
                                })
                                BackupFoundPrompt(onRestoreEntry = { nav.push(RestorePreviewKey(it)) })
                            },
                            backupAlert = {
                                BackupAlertButton(onOpen = {
                                    nav.openTab(SettingsKey)
                                    nav.push(BackupSettingsKey)
                                })
                            },
                            listTop = { BackupNudgeCard() },
                            onOpen = nav::openEntry,
                            onWrite = onWrite,
                            onSearch = { nav.push(SearchKey()) },
                            onEdit = { nav.push(WriteKey(entryId = it)) },
                            onTrashed = closeTrashed,
                            selectedId = homeStack.filterIsInstance<EntryDetailKey>().lastOrNull()?.entryId?.takeIf { splitWindow },
                            // 같은 자리에서 얇은 줄과 카드를 번갈아 쓴다.
                            // 광고 수는 늘지 않는다.
                            // 두 칸에서 상세가 열려 있는 동안은 옆 목록에도 광고를 두지 않는다.
                            // 한 칸에서는 상세가 목록을 덮으니 따지지 않는다.
                            // 따지면 일기를 누르는 순간 광고가 빠져, 넘어가는 동안 목록이 들썩였다(사용자 지적 2026-11-09).
                            adSlot = { order, slot ->
                                if (!splitWindow || homeStack.none { it is EntryDetailKey }) {
                                    AdSlot("home-$order", if (order % 2 == 0) AdShape.Row else AdShape.Card, slot)
                                }
                            }
                        )
                    }
                    entry<SearchKey>(clazzContentKey = { "search:${it.moods}:${it.from}:${it.to}" }, metadata = ListPane) { key ->
                        SearchRoute(
                            initial = key.toFilter(),
                            selectedId = nav.current.filterIsInstance<EntryDetailKey>().lastOrNull()?.entryId?.takeIf { splitWindow },
                            onBack = pop,
                            onOpen = nav::openEntry
                        )
                    }
                    // 화면 키의 문자열에는 일기 ID를 담지 않으니, 저장 자리 구분은 ID로 따로 준다.
                    entry<EntryDetailKey>(clazzContentKey = { it.entryId }, metadata = DetailPane) { key ->
                        EntryDetailRoute(
                            entryId = key.entryId,
                            // 창이 넓어도 달력에서 연 상세는 한 칸이다.
                            // 목록이 옆에 그려질 때만 뒤로 버튼을 뺀다.
                            onBack = pop.takeUnless { LocalInListDetail.current },
                            onEdit = { nav.push(WriteKey(entryId = key.entryId)) },
                            onPhoto = { nav.push(PhotoViewerKey(key.entryId, it)) },
                            onOtherVersion = nav::openEntry,
                            onTrashed = {
                                nav.closeEntry(key.entryId)
                                onTrashed(key.entryId)
                            }
                        )
                    }
                    entry<PhotoViewerKey>(clazzContentKey = { "${it.entryId}/${it.index}" }, metadata = FullScreenEntry) { key ->
                        PhotoViewerRoute(key.entryId, key.index, onClose = pop)
                    }
                    entry<CalendarKey>(metadata = tabRoot(CalendarKey)) {
                        CalendarRoute(
                            twoPane = splitWindow,
                            onOpen = nav::openEntry,
                            onWriteOn = { nav.push(WriteKey(date = it.toString())) },
                            onEdit = { nav.push(WriteKey(entryId = it)) },
                            onTrash = closeTrashed
                        )
                    }
                    entry<InsightKey>(metadata = tabRoot(InsightKey)) {
                        InsightRoute(
                            onMood = { mood, range ->
                                nav.push(SearchKey(listOf(mood.id), range?.start?.toString(), range?.end?.toString()))
                            },
                            onPeriod = { range -> nav.push(SearchKey(from = range.start.toString(), to = range.end.toString())) },
                            onPlus = { nav.push(PlusKey) },
                            onAdvanced = { nav.push(AdvancedStatsKey) },
                            onWrite = onWrite,
                            adSlot = { AdSlot("insight", AdShape.Card) }
                        )
                    }
                    entry<AdvancedStatsKey> {
                        // 구독이 끝난 채 앱이 이 화면을 복원해도 머물지 않고 돌아보기로 돌아간다.
                        val plan by koinInject<PlanRepository>().plan.collectAsStateWithLifecycle()
                        LaunchedEffect(plan) { if (!plan.advancedStats) pop() }
                        AdvancedStatsRoute(
                            onBack = pop,
                            onMood = { mood, range ->
                                nav.push(SearchKey(listOf(mood.id), range?.start?.toString(), range?.end?.toString()))
                            },
                            onWrite = onWrite
                        )
                    }
                    settingsEntries(push = nav::push, pop = pop, home = { nav.startOver(HomeKey) })
                    entry<WriteKey>(clazzContentKey = { it.contentKey }, metadata = FullScreenEntry) { key ->
                        val target = key.entryId?.let { WriteTarget.Edit(it) } ?: WriteTarget.New(key.date?.let(LocalDate::parse))
                        WriteRoute(
                            target = target,
                            onSaved = { id ->
                                pop()
                                // 수정은 보던 상세로 돌아가고, 새 일기는 그 일기의 상세로 간다(FR-012).
                                if ((nav.top as? EntryDetailKey)?.entryId != id) nav.openEntry(id)
                                notify(Message.Saved)
                                adTurn.saved(nav.tab)
                                adScope.launch { ads.afterSave() }
                            },
                            onClosed = { kept ->
                                pop()
                                if (kept) notify(Message.DraftKept)
                            },
                            onPlus = { nav.push(PlusKey) }
                        )
                    }
                }
            ),
        // 밀려나는 화면을 어둡게 하는 뒤 바탕(DiaryTransitions). 평소에는 화면에 가려 안 보임
        modifier = modifier.background(DiaryTheme.colors.scrim.copy(alpha = 1f)),
        sceneStrategies = listOf(listDetail),
        transitionSpec = { if (nav.movedBetweenTabs || reduceMotion) TabSwap else pushMotion() },
        popTransitionSpec = { if (nav.movedBetweenTabs || reduceMotion) TabSwap else popMotion() },
        predictivePopTransitionSpec = { if (reduceMotion) TabSwap else predictivePopMotion(it) },
        onBack = pop
    )
}

private const val HALF = 0.5f

/**
 * 두 칸에서 고른 일기가 없을 때의 오른쪽.
 * 일기가 아직 없어도 칸이 통째로 비지 않게 늘 보인다.
 * 아이콘은 두지 않는다. 연필은 쓰기 단추와 같아 누를 것처럼 보였다(사용자 결정 2026-11-09, Apple 메일·메모의 빈 칸처럼 글 한 줄).
 */
@Composable
private fun PickHint() {
    DiaryEmptyArea(Modifier.background(DiaryTheme.colors.canvas)) {
        DiaryEmptyState(icon = null, title = stringResource(Res.string.detail_pick_hint))
    }
}
