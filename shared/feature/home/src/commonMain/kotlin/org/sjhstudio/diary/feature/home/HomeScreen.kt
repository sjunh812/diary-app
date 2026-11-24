package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryCollapsingHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryCollapsingHeaderState
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryTabTopBar
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.component.rememberDiaryCollapsingHeaderState
import org.sjhstudio.diary.core.designsystem.component.titleCollapse
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.MonthCount
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.monthAt
import org.sjhstudio.diary.core.ui.ContextTipBar
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryDismissSlot
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.DiaryTitleButton
import org.sjhstudio.diary.core.ui.EntryMenuEvents
import org.sjhstudio.diary.core.ui.EntryMenuFor
import org.sjhstudio.diary.core.ui.LoadFailed
import org.sjhstudio.diary.core.ui.OnTabReselected
import org.sjhstudio.diary.core.ui.ReportDrawnWhen
import org.sjhstudio.diary.core.ui.SkeletonEntryCards
import org.sjhstudio.diary.core.ui.SkeletonEntryRows
import org.sjhstudio.diary.core.ui.SkeletonPhotoGrid
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.entryMarks
import org.sjhstudio.diary.core.ui.loadMoreFailed
import org.sjhstudio.diary.core.ui.monthHeader
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberFirstOpenTip
import org.sjhstudio.diary.core.ui.rememberToday
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.home_empty_action
import org.sjhstudio.diary.feature.home.resources.home_empty_body
import org.sjhstudio.diary.feature.home.resources.home_empty_title
import org.sjhstudio.diary.feature.home.resources.home_favorites_empty
import org.sjhstudio.diary.feature.home.resources.home_favorites_only
import org.sjhstudio.diary.feature.home.resources.home_favorites_show_all
import org.sjhstudio.diary.feature.home.resources.home_month_sheet_title
import org.sjhstudio.diary.feature.home.resources.home_search
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview_action
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview_kept
import org.sjhstudio.diary.feature.home.resources.tip_home_full_preview_kept_action
import org.sjhstudio.diary.feature.home.resources.tip_home_list_style
import org.sjhstudio.diary.feature.home.resources.tip_home_list_style_action

/**
 * 홈(Figma "홈 (S1)")
 * 큰 제목은 지금 보이는 달이고 누르면 그 달로 감. 넓은 화면에서는 왼쪽 칸, 고른 줄은 [selectedId]
 */
@Composable
fun HomeRoute(
    onOpen: (String) -> Unit,
    onWrite: () -> Unit,
    onSearch: () -> Unit,
    onEdit: (String) -> Unit,
    onTrashed: (String) -> Unit,
    selectedId: String?,
    modifier: Modifier = Modifier,
    backupLine: @Composable () -> Unit = {},
    backupAlert: @Composable () -> Unit = {},
    // 목록을 다 읽었는지 받음. 다 읽은 뒤 정해진 카드는 목록을 밀고 끼어들지 않게 자리를 열며 들어옴
    listTop: @Composable (listRead: Boolean) -> Unit = {},
    adSlot: @Composable (Int, Modifier) -> Unit = { _, _ -> },
    onListStyle: (() -> Unit)? = null
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val allRows = viewModel.allRows.collectAsLazyPagingItems()
    val favoriteRows = viewModel.favoriteRows.collectAsLazyPagingItems()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val rows = if (favoritesOnly) favoriteRows else allRows
    // 첫 목록을 읽어야 다 그려진 것(시작 시간 측정 기준)
    ReportDrawnWhen { allRows.loadState.refresh !is LoadState.Loading }
    val allMonths by viewModel.allMonths.collectAsStateWithLifecycle()
    val favoriteMonths by viewModel.favoriteMonths.collectAsStateWithLifecycle()
    val monthCounts = if (favoritesOnly) favoriteMonths else allMonths
    val look by viewModel.look.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val previewLength by viewModel.previewLength.collectAsStateWithLifecycle()
    val fullPreviewKept by viewModel.fullPreviewKept.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    val list = rememberLazyListState()
    val grid = rememberLazyGridState()
    val header = rememberDiaryCollapsingHeaderState()
    OnTabReselected {
        // 코드로 올리면 중첩 스크롤을 안 거쳐 머리가 접힌 채 남아 머리도 함께 펼침
        // 멀리서 부드럽게 올리면 수백 줄을 지나며 느리고 번쩍여, 첫 일기 줄로 바로 옮긴 뒤 짧게만 올림(Google 앱 방식)
        // 첫 줄은 늘 맨 위 달이라 올라가는 동안 제목에 다른 달이 끼지 않음
        coroutineScope {
            launch { header.expand() }
            if (style == ListStyle.PHOTO_GRID) {
                if (grid.firstVisibleItemIndex > RESELECT_JUMP) grid.scrollToItem(RESELECT_JUMP)
                grid.animateScrollToItem(0)
            } else {
                if (list.firstVisibleItemIndex > RESELECT_JUMP) list.scrollToItem(RESELECT_JUMP)
                list.animateScrollToItem(0)
            }
        }
    }
    val dates = rememberDiaryDateFormat()
    val today = rememberToday()
    var picking by remember { mutableStateOf(false) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    // 스크롤마다 다시 그리지 않게 맨 위 줄의 달이 바뀔 때만 값이 바뀜
    val shown by remember(rows, list, grid, monthCounts, today) {
        derivedStateOf {
            val first = if (style == ListStyle.PHOTO_GRID) grid.firstVisibleItemIndex else list.firstVisibleItemIndex
            val position = (first - LIST_TOP_ITEMS).coerceAtLeast(0)
            val loaded = if (position < rows.itemCount) rows.peek(position)?.date else null
            // 달별 편수를 끝내 못 읽어도 목록 읽기가 끝나면 오늘 달로. 제목이 빈 채로 남지 않게
            val counts = monthCounts ?: emptyList<MonthCount>().takeIf { rows.loadState.refresh !is LoadState.Loading }
            shownMonth(loaded, counts, position, today)
        }
    }
    HomeScreen(
        // 맨 위 달을 알기 전에는 자리만 둠. 오늘 달을 먼저 보였다가 바꾸지 않게
        title = shown?.let { dates.yearMonth(it.firstDay) }.orEmpty(),
        rows = rows,
        list = list,
        grid = grid,
        style = style,
        favoritesOnly = favoritesOnly,
        selectedId = selectedId,
        today = today,
        previewStyle = diaryPreviewTextStyle(look.font, look.size) { family },
        onToggleFavorites = viewModel::toggleFavoritesOnly,
        onPickMonth = { picking = true },
        onOpen = onOpen,
        onOpenClippedRow = viewModel::clippedOpened,
        onMenu = { menuFor = it },
        onWrite = onWrite,
        onSearch = onSearch,
        modifier = modifier,
        backupLine = backupLine,
        backupAlert = backupAlert,
        header = header,
        listTop = {
            val listRead = rows.loadState.refresh !is LoadState.Loading
            listTop(listRead)
            // 백업 안내가 떠 있으면 앱이 onListStyle에 null을 넘겨 목록 위 안내가 겹치지 않음
            // 자리는 늘 두어 카드가 닫히고 팁이 생길 때도 처음 그리기로 보지 않고 같은 움직임을 씀
            // 설정을 읽기 전에는 팁을 고르지 않음. 다른 팁이 먼저 그날의 팁으로 적힘
            HomeTip(
                listRead = listRead,
                candidates =
                    if (onListStyle == null || previewLength == null || fullPreviewKept == null) {
                        emptyList()
                    } else {
                        buildList {
                            // 사진 격자는 본문 길이를 따르지 않음
                            if (style != ListStyle.PHOTO_GRID) {
                                // 팁이 미뤄진 사이 길이를 직접 골랐으면 "예전처럼 전체"가 사실이 아님
                                if (fullPreviewKept == true) add(ContextTip.HOME_FULL_PREVIEW_KEPT)
                                if (previewLength != PreviewLength.FULL) add(ContextTip.HOME_FULL_PREVIEW)
                            }
                            // 목록 보기가 설정 안에만 있어 처음 한 번 알림. 일기가 생기고 아직 기본 피드일 때만
                            if (style == ListStyle.FEED && rows.itemCount >= LIST_STYLE_TIP_AFTER) add(ContextTip.HOME_LIST_STYLE)
                        }
                    },
                onListStyle = onListStyle ?: {},
                onLength = viewModel::setPreviewLength
            )
        },
        adSlot = adSlot
    )
    menuFor?.let { id ->
        EntryMenuFor(
            id,
            favorite = rows.itemSnapshotList.items.firstOrNull { it.id == id }?.isFavorite == true,
            work = viewModel.menu,
            onDismiss = { menuFor = null },
            onEdit = onEdit
        )
    }
    EntryMenuEvents(viewModel.menu, onTrashed)
    LaunchedEffect(viewModel, style) {
        viewModel.jumps.collect { position ->
            val index = position + LIST_TOP_ITEMS
            if (style == ListStyle.PHOTO_GRID) grid.scrollToItem(index) else list.scrollToItem(index)
        }
    }
    // 고를 달이 있어야 제목을 누를 수 있어 열렸으면 맨 위 달도 앎
    val current = shown
    if (picking && current != null) {
        val months = remember(monthCounts) { monthCounts.orEmpty().mapTo(HashSet()) { it.month } }
        DiaryMonthSheet(
            title = stringResource(Res.string.home_month_sheet_title),
            initialYear = current.year,
            shown = current,
            years = remember(months) { months.map { it.year }.distinct().sorted() },
            enabled = { it in months },
            onPick = { month ->
                picking = false
                viewModel.jumpTo(month)
            },
            onDismiss = { picking = false }
        )
    }
}

/**
 * 제목에 보일 달
 * 맨 위 줄을 읽었으면 그 줄의 달, 아직 자리 표시 줄이면 달별 편수로 센 달
 * 건너뛰거나 맨 위로 갈 때 앞서 보던 달이 남지 않음. 일기가 없으면 오늘이 든 달
 * 줄도 달별 편수도 아직 읽지 않았으면 null
 */
internal fun shownMonth(loaded: LocalDate?, counts: List<MonthCount>?, position: Int, today: LocalDate): YearMonth? =
    loaded?.yearMonth ?: counts?.let { it.monthAt(position) ?: today.yearMonth }

@Composable
internal fun HomeScreen(
    title: String,
    rows: LazyPagingItems<EntryRow>,
    list: LazyListState,
    grid: LazyGridState,
    style: ListStyle,
    favoritesOnly: Boolean,
    selectedId: String?,
    today: LocalDate,
    previewStyle: TextStyle,
    onToggleFavorites: () -> Unit,
    onPickMonth: () -> Unit,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
    onWrite: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenClippedRow: () -> Unit = {},
    backupLine: @Composable () -> Unit = {},
    backupAlert: @Composable () -> Unit = {},
    header: DiaryCollapsingHeaderState = rememberDiaryCollapsingHeaderState(),
    listTop: @Composable () -> Unit = {},
    adSlot: @Composable (Int, Modifier) -> Unit = { _, _ -> }
) {
    Box(modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxSize()) {
            // 일기가 없으면 고를 달도 없음
            // 불러오는 동안은 마지막으로 다 불러왔을 때의 답 유지. 즐겨찾기를 켜고 끌 때 화살표가 반짝이지 않게
            val loadedEmpty = if (rows.loadState.refresh is LoadState.NotLoading) rows.itemCount == 0 else null
            // 처음 다 읽기 전에는 null이고 숨김. 보였다 숨기면 일기 없는 첫 실행에서 반짝이고, 숨겼다 보이면 목록과 같이 나옴
            // 전체와 즐겨찾기 목록마다 따로. 즐겨찾기를 끄는 순간 즐겨찾기의 빈 안내가 전체 목록에 남지 않게
            var lastEmpty by remember(rows) { mutableStateOf<Boolean?>(null) }
            SideEffect { loadedEmpty?.let { lastEmpty = it } }
            val noMonths = loadedEmpty ?: lastEmpty ?: true
            val pickMonth = onPickMonth.takeUnless { noMonths }
            // 스크롤하면 큰 제목과 백업 줄이 접히고 바에 작은 제목이 나타남(`docs/design.md` 탭 상단 바)
            // 백업 줄은 접힌 동안 감추고, 손봐야 할 때만 바에 경고 단추
            // 작은 제목은 큰 제목이 가려진 만큼만 따름. 머리 전체로 재면 큰 제목이 다 가려지기 전에 진해져 겹쳐 보임
            var titleHeight by remember { mutableIntStateOf(0) }
            DiaryTabTopBar(
                title = title,
                collapsed = { titleCollapse((-header.offset).toInt(), titleHeight) },
                onTitleClick = pickMonth,
                accessory = backupAlert,
                actions = { TopActions(favoritesOnly, onToggleFavorites, onSearch) }
            )
            DiaryCollapsingHeader(header) {
                Column {
                    DiaryTitleButton(
                        title,
                        pickMonth,
                        Modifier.padding(horizontal = LocalScreenGutter.current).onSizeChanged { titleHeight = it.height }
                    )
                    Spacer(Modifier.height(DiarySpacing.x1))
                    backupLine()
                    Spacer(Modifier.height(DiarySpacing.x2))
                }
            }
            val refresh = rows.loadState.refresh
            Box(Modifier.weight(1f).nestedScroll(header.connection)) {
                when {
                    rows.itemCount > 0 -> {
                        val dates = rememberDiaryDateFormat()
                        when (style) {
                            ListStyle.FEED ->
                                Feed(rows, list, selectedId, today, previewStyle, dates, onOpen, onMenu, listTop, adSlot, onOpenClippedRow)

                            ListStyle.CARD ->
                                CardList(
                                    rows,
                                    list,
                                    selectedId,
                                    today,
                                    previewStyle,
                                    dates,
                                    onOpen,
                                    onMenu,
                                    listTop,
                                    adSlot,
                                    onOpenClippedRow
                                )

                            ListStyle.PHOTO_GRID -> PhotoGrid(rows, grid, selectedId, today, dates, onOpen, onMenu, listTop)
                        }
                    }

                    // 목록 위 카드 아래로 차례로 쌓음. 겹쳐 두면 빈 안내나 뼈대가 카드를 덮음
                    refresh is LoadState.NotLoading ->
                        Column {
                            ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current))
                            Box(Modifier.testTag(HOME_EMPTY_TAG)) { Empty(favoritesOnly, onWrite, onToggleFavorites) }
                        }

                    refresh is LoadState.Error -> LoadFailed(rows::retry)

                    // 빈 홈에서 첫 일기를 쓰고 돌아와 다시 읽는 동안은 빈 안내를 그대로 둠. 뼈대가 스쳤다 목록이 되지 않게
                    lastEmpty == true ->
                        Column {
                            ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current))
                            Box(Modifier.testTag(HOME_EMPTY_TAG)) { Empty(favoritesOnly, onWrite, onToggleFavorites) }
                        }

                    else ->
                        Column {
                            ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current))
                            // 곧 나올 목록과 같은 꼴. 처음 읽기는 늘 늦어 기다리지 않고 바로, 다시 읽기는 0.3초 뒤
                            val delayed = lastEmpty != null
                            Box(Modifier.testTag(HOME_SKELETON_TAG)) {
                                when (style) {
                                    ListStyle.FEED -> SkeletonEntryRows(Modifier.padding(top = DiarySpacing.x4), delayed = delayed)
                                    ListStyle.CARD -> SkeletonEntryCards(Modifier.padding(top = DiarySpacing.x4), delayed = delayed)
                                    ListStyle.PHOTO_GRID -> SkeletonPhotoGrid(delayed = delayed)
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun TopActions(favoritesOnly: Boolean, onToggle: () -> Unit, onSearch: () -> Unit) {
    val colors = DiaryTheme.colors
    Row {
        DiaryIconButton(DiaryIcons.Search, stringResource(Res.string.home_search), onSearch)
        // 켜고 끄는 단추라 화면 읽기에서 스위치로 읽게 역할을 Switch로
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(DiarySpacing.minTouchTarget)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.full)
                .clickable(interactionSource = interaction, indication = null, role = Role.Switch, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(
                if (favoritesOnly) DiaryIcons.StarFilled else DiaryIcons.StarOutline,
                contentDescription = stringResource(Res.string.home_favorites_only),
                tint = if (favoritesOnly) colors.accentIcon else colors.textPrimary
            )
        }
    }
}

@Composable
private fun Empty(favoritesOnly: Boolean, onWrite: () -> Unit, onShowAll: () -> Unit) {
    DiaryEmptyArea {
        if (favoritesOnly) {
            // 위 별을 다시 눌러야 풀린다는 걸 알기 어려워 여기서도 풀 수 있게
            DiaryEmptyState(
                DiaryIcons.StarOutline,
                stringResource(Res.string.home_favorites_empty),
                actionLabel = stringResource(Res.string.home_favorites_show_all),
                onAction = onShowAll
            )
        } else {
            DiaryEmptyState(
                DiaryIcons.Pen,
                stringResource(Res.string.home_empty_title),
                description = stringResource(Res.string.home_empty_body),
                actionLabel = stringResource(Res.string.home_empty_action),
                onAction = onWrite,
                // 이 화면의 하나뿐인 쓰기 길이라 다른 빈 화면보다 강하게
                actionKind = DiaryButtonKind.Primary
            )
        }
    }
}

/** 달이 바뀌면 달 머리, 날이 바뀌면 날짜 열을 보임. 그날 둘째 줄부터 날짜 열은 비움 */
@Composable
private fun Feed(
    rows: LazyPagingItems<EntryRow>,
    list: LazyListState,
    selectedId: String?,
    today: LocalDate,
    previewStyle: TextStyle,
    dates: DiaryDateFormat,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
    listTop: @Composable () -> Unit,
    adSlot: @Composable (Int, Modifier) -> Unit,
    onOpenClippedRow: () -> Unit
) {
    val colors = DiaryTheme.colors
    // 마지막 줄이 쓰기 버튼에 가리지 않게 아래 여백
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = DiarySpacing.x14 + DiarySpacing.x6)) {
        item(key = LIST_TOP_KEY, contentType = LIST_TOP_KEY) { ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current)) }
        loadMoreFailed(rows.loadState.prepend is LoadState.Error, rows::retry, "prepend_failed")
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "entry" }) { index ->
            val row = rows[index]
            if (row == null) {
                Spacer(Modifier.fillMaxWidth().height(DiarySpacing.x14 + DiarySpacing.x14))
                return@items
            }
            val previous = if (index > 0) rows.peek(index - 1) else null
            val (newMonth, newDay) = entryMarks(index, row, previous)
            Column {
                if (newMonth && index > 0) {
                    DiaryMonthHeader(dates.monthHeader(row.date, today.year, previous?.date))
                }
                val interaction = remember { MutableInteractionSource() }
                // 글을 그려 봐야 잘렸는지 앎. 그리는 데만 쓰여 다시 그리지 않음
                val clipped = remember(row.id) { ClippedFlag() }
                DiaryEntryLine(
                    row = row,
                    showDate = newDay,
                    dividerAbove = index > 0 && !newMonth,
                    previewStyle = previewStyle,
                    onPreviewClip = { clipped.value = it },
                    modifier =
                        Modifier
                            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                            .background(if (row.id == selectedId) colors.selected else colors.canvas)
                            .combinedClickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button,
                                onLongClick = { onMenu(row.id) },
                                onClick = {
                                    if (row.previewCut || clipped.value) onOpenClippedRow()
                                    onOpen(row.id)
                                }
                            )
                )
                // 광고 없는 사람에게 빈 틈이 남지 않게 여백은 광고에만
                if (isAdSlot(
                        index
                    )
                ) {
                    adSlot(adOrder(index), Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3))
                }
            }
        }
        loadMoreFailed(rows.loadState.append is LoadState.Error, rows::retry)
    }
}

/**
 * 목록과 함께 움직이는 맨 위 자리. 권하기 카드처럼 잠깐 보이는 안내가 일기 칸을 계속 차지하지 않게
 * 아래 여백이 늘 있어 안내가 늦게 떠도 맨 위 줄로 남고 일기에 밀려 위로 숨지 않음
 */
@Composable
internal fun ListTop(content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        content()
        Spacer(Modifier.height(DiarySpacing.x1))
    }
}

/** 다섯째 일기 아래 한 개, 그 뒤 10편마다 한 개(`docs/business.md` 광고 자리) */
internal fun isAdSlot(index: Int): Boolean = index >= FIRST_AD && (index - FIRST_AD) % AD_EVERY == 0

/** 몇 번째 광고 자리인지. 앱이 이 번호로 얇은 줄과 카드를 고름 */
internal fun adOrder(index: Int): Int = (index - FIRST_AD) / AD_EVERY

/** 일기 줄 앞 항목 수. 스크롤 위치를 일기 번호로 바꿀 때 뺌 */
internal const val LIST_TOP_ITEMS = 1

/** 목록 보기 팁을 띄우는 일기 수. 이보다 적으면 모양을 바꿔도 차이가 잘 안 보임 */
private const val LIST_STYLE_TIP_AFTER = 1
internal const val LIST_TOP_KEY = "list-top"

/** 화면 시험이 뼈대와 빈 안내 자리를 찾는 이름표 */
internal const val HOME_SKELETON_TAG = "home-skeleton"
internal const val HOME_EMPTY_TAG = "home-empty"

private const val FIRST_AD = 4
private const val AD_EVERY = 10

/** 탭을 다시 눌렀을 때 이보다 아래면 이 줄(첫 일기)까지 바로 옮긴 뒤 부드럽게 올림 */
private const val RESELECT_JUMP = LIST_TOP_ITEMS

/** 줄의 미리보기가 잘렸는지. 누를 때만 읽어 상태로 두지 않음 */
internal class ClippedFlag(var value: Boolean = false)

/**
 * 목록 위 팁 자리. 열린 첫 팁 하나만(스펙 008 FR-011)
 * 옛 설정 이음 > 전체 보기 > 목록 보기 순서
 */
@Composable
private fun HomeTip(candidates: List<ContextTip>, listRead: Boolean, onListStyle: () -> Unit, onLength: (PreviewLength) -> Unit) {
    val modifier = Modifier.padding(top = DiarySpacing.x2, bottom = DiarySpacing.x3)
    // 목록을 읽기 전에 고른 팁은 목록과 함께 처음 정해지는 것이라 움직이지 않음
    DiaryDismissSlot(TipSlot(rememberFirstOpenTip(candidates), listRead), key = { it.tip }, known = { it.listRead }) { slot ->
        when (val tip = slot.tip) {
            ContextTip.HOME_FULL_PREVIEW_KEPT ->
                ContextTipBar(
                    tip,
                    stringResource(Res.string.tip_home_full_preview_kept),
                    modifier,
                    action = stringResource(Res.string.tip_home_full_preview_kept_action),
                    onAction = { onLength(PreviewLength.THREE_LINES) }
                )

            ContextTip.HOME_FULL_PREVIEW ->
                ContextTipBar(
                    tip,
                    stringResource(Res.string.tip_home_full_preview),
                    modifier,
                    action = stringResource(Res.string.tip_home_full_preview_action),
                    onAction = { onLength(PreviewLength.FULL) }
                )

            ContextTip.HOME_LIST_STYLE ->
                ContextTipBar(
                    tip,
                    stringResource(Res.string.tip_home_list_style),
                    modifier,
                    action = stringResource(Res.string.tip_home_list_style_action),
                    onAction = onListStyle
                )

            else -> Unit
        }
    }
}

/** 팁 자리의 상태. 목록을 다 읽었는지까지 알아야 처음 정해지는 팁을 움직이지 않음 */
private data class TipSlot(val tip: ContextTip?, val listRead: Boolean)
