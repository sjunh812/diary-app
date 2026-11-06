package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.DiaryTitleButton
import org.sjhstudio.diary.core.ui.SkeletonEntryRows
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberShareText
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_edit
import org.sjhstudio.diary.feature.home.resources.detail_favorite
import org.sjhstudio.diary.feature.home.resources.detail_share
import org.sjhstudio.diary.feature.home.resources.detail_trash
import org.sjhstudio.diary.feature.home.resources.home_empty_action
import org.sjhstudio.diary.feature.home.resources.home_empty_body
import org.sjhstudio.diary.feature.home.resources.home_empty_title
import org.sjhstudio.diary.feature.home.resources.home_error_body
import org.sjhstudio.diary.feature.home.resources.home_error_retry
import org.sjhstudio.diary.feature.home.resources.home_error_title
import org.sjhstudio.diary.feature.home.resources.home_favorites_empty
import org.sjhstudio.diary.feature.home.resources.home_favorites_only
import org.sjhstudio.diary.feature.home.resources.home_month_sheet_title
import org.sjhstudio.diary.feature.home.resources.home_search

/**
 * 홈(Figma "홈 (S1)").
 * 큰 제목은 지금 보이는 달을 보이고 누르면 그 달로 간다.
 * 넓은 화면에서는 왼쪽 칸이 되고 고른 줄을 [selectedId]로 표시한다.
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
    listTop: @Composable () -> Unit = {},
    adSlot: @Composable (Int) -> Unit = {}
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val rows = viewModel.rows.collectAsLazyPagingItems()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val months by viewModel.months.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    val list = rememberLazyListState()
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val dates = rememberDiaryDateFormat()
    val today = remember { currentDate() }
    var picking by remember { mutableStateOf(false) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    // 목록이 비어 있으면 오늘이 든 달을 보인다.
    // 스크롤마다 화면을 다시 그리지 않게 맨 위 일기가 바뀔 때만 값이 바뀐다.
    val shown by remember(rows, list, grid, today) {
        derivedStateOf {
            val first = if (style == ListStyle.PHOTO_GRID) grid.firstVisibleItemIndex else list.firstVisibleItemIndex
            rows.itemCount.takeIf { it > 0 }?.let { rows.peek((first - LIST_TOP_ITEMS).coerceIn(0, it - 1))?.date } ?: today
        }
    }
    HomeScreen(
        title = dates.yearMonth(shown),
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
        onMenu = { menuFor = it },
        onWrite = onWrite,
        onSearch = onSearch,
        modifier = modifier,
        backupLine = backupLine,
        listTop = listTop,
        adSlot = adSlot
    )
    menuFor?.let { id ->
        // iOS의 공유 창은 화면 컨트롤러가 있어야 해서 메뉴를 열 때만 준비한다.
        val share = rememberShareText()
        DiaryBottomSheet(onDismiss = { menuFor = null }) {
            DiarySheetAction(DiaryIcons.Pen, stringResource(Res.string.detail_edit), DiaryTheme.colors.textPrimary) {
                menuFor = null
                onEdit(id)
            }
            DiarySheetAction(DiaryIcons.StarOutline, stringResource(Res.string.detail_favorite), DiaryTheme.colors.textPrimary) {
                menuFor = null
                viewModel.toggleFavorite(id)
            }
            DiarySheetAction(DiaryIcons.Share, stringResource(Res.string.detail_share), DiaryTheme.colors.textPrimary) {
                menuFor = null
                scope.launch { viewModel.plainText(id)?.let(share) }
            }
            DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.detail_trash), DiaryTheme.colors.danger) {
                menuFor = null
                viewModel.moveToTrash(id) { onTrashed(id) }
            }
        }
    }
    if (picking) {
        DiaryMonthSheet(
            title = stringResource(Res.string.home_month_sheet_title),
            initialYear = shown.year,
            shown = YearMonth(shown.year, shown.month.number),
            hasYear = { year -> months.any { it.year == year } },
            enabled = { it in months },
            onPick = { month ->
                picking = false
                scope.launch {
                    val position = viewModel.positionOf(month) + LIST_TOP_ITEMS
                    if (style == ListStyle.PHOTO_GRID) grid.scrollToItem(position) else list.scrollToItem(position)
                }
            },
            onDismiss = { picking = false }
        )
    }
}

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
    backupLine: @Composable () -> Unit = {},
    listTop: @Composable () -> Unit = {},
    adSlot: @Composable (Int) -> Unit = {}
) {
    Box(modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxSize()) {
            TopActions(favoritesOnly, onToggleFavorites, onSearch)
            // 일기가 없으면 고를 달도 없다.
            // 불러오는 동안에는 화살표가 깜빡이지 않게 그대로 둔다.
            val noMonths = rows.itemCount == 0 && rows.loadState.refresh is LoadState.NotLoading
            DiaryTitleButton(title, onPickMonth.takeUnless { noMonths }, Modifier.padding(horizontal = LocalScreenGutter.current))
            Spacer(Modifier.height(DiarySpacing.x1))
            backupLine()
            // 스크롤하면 나타나는 선이 백업 문구에 붙지 않게 띄운다.
            // 그만큼 목록 위를 줄여 쉴 때의 자리는 그대로다.
            Spacer(Modifier.height(DiarySpacing.x2))
            ScrollEdge(if (style == ListStyle.PHOTO_GRID) grid.canScrollBackward else list.canScrollBackward)
            val refresh = rows.loadState.refresh
            when {
                rows.itemCount > 0 -> {
                    val dates = rememberDiaryDateFormat()
                    when (style) {
                        ListStyle.FEED -> Feed(rows, list, selectedId, today, previewStyle, dates, onOpen, onMenu, listTop, adSlot)
                        ListStyle.CARD -> CardList(rows, list, selectedId, previewStyle, dates, onOpen, onMenu, listTop, adSlot)
                        ListStyle.PHOTO_GRID -> PhotoGrid(rows, grid, selectedId, dates, onOpen, onMenu, listTop)
                    }
                }

                refresh is LoadState.NotLoading -> {
                    ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current))
                    Empty(favoritesOnly, onWrite)
                }

                refresh is LoadState.Error -> LoadFailed(rows::retry)

                else -> {
                    ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current))
                    SkeletonEntryRows(Modifier.padding(top = DiarySpacing.x4))
                }
            }
        }
    }
}

/**
 * 목록이 머리 밑으로 들어갔을 때만 보이는 선.
 * 잘린 글이 머리와 붙어 보이지 않게 한다.
 * 칸은 늘 있어서 목록이 움직이지 않는다.
 */
@Composable
private fun ScrollEdge(scrolled: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(DiarySpacing.hairline).background(if (scrolled) DiaryTheme.colors.line else DiaryTheme.colors.canvas)
    )
}

@Composable
private fun TopActions(favoritesOnly: Boolean, onToggle: () -> Unit, onSearch: () -> Unit) {
    val colors = DiaryTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(end = DiarySpacing.x1, top = DiarySpacing.x1),
        horizontalArrangement = Arrangement.End
    ) {
        DiaryIconButton(DiaryIcons.Search, stringResource(Res.string.home_search), onSearch)
        // 켜고 끄는 단추라 화면 읽기에 스위치로 읽히게 역할을 Switch로 둔다.
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
                tint = if (favoritesOnly) colors.accent else colors.textPrimary
            )
        }
    }
}

@Composable
private fun Empty(favoritesOnly: Boolean, onWrite: () -> Unit) {
    DiaryEmptyArea {
        if (favoritesOnly) {
            DiaryEmptyState(DiaryIcons.StarOutline, stringResource(Res.string.home_favorites_empty))
        } else {
            DiaryEmptyState(
                DiaryIcons.Pen,
                stringResource(Res.string.home_empty_title),
                description = stringResource(Res.string.home_empty_body),
                actionLabel = stringResource(Res.string.home_empty_action),
                onAction = onWrite,
                // 이 화면의 하나뿐인 쓰기 길이라 다른 빈 화면보다 강하게 둔다.
                actionKind = DiaryButtonKind.Primary
            )
        }
    }
}

/**
 * 목록을 읽지 못했을 때.
 * 뼈대를 계속 두지 않고 까닭과 다시 시도를 보인다.
 */
@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    DiaryEmptyArea {
        DiaryEmptyState(
            DiaryIcons.CircleAlert,
            stringResource(Res.string.home_error_title),
            description = stringResource(Res.string.home_error_body),
            actionLabel = stringResource(Res.string.home_error_retry),
            onAction = onRetry
        )
    }
}

/**
 * 달이 바뀌면 달 머리를, 날이 바뀌면 날짜 열을 보인다.
 * 그날 둘째 줄부터 날짜 열은 비운다.
 */
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
    adSlot: @Composable (Int) -> Unit
) {
    val colors = DiaryTheme.colors
    // 마지막 줄이 쓰기 버튼에 가리지 않게 아래를 비운다.
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = DiarySpacing.x14 + DiarySpacing.x6)) {
        item(key = LIST_TOP_KEY, contentType = LIST_TOP_KEY) { ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current)) }
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "entry" }) { index ->
            val row = rows[index]
            if (row == null) {
                Spacer(Modifier.fillMaxWidth().height(DiarySpacing.x14 + DiarySpacing.x14))
                return@items
            }
            val previous = if (index > 0) rows.peek(index - 1) else null
            val newMonth = previous == null || previous.date.year != row.date.year || previous.date.month != row.date.month
            val newDay = previous == null || previous.date != row.date
            Column {
                if (newMonth && index > 0) {
                    DiaryMonthHeader(if (row.date.year == today.year) dates.month(row.date) else dates.yearMonth(row.date))
                }
                val interaction = remember { MutableInteractionSource() }
                DiaryEntryLine(
                    row = row,
                    showDate = newDay,
                    dividerAbove = index > 0 && !newMonth,
                    previewStyle = previewStyle,
                    modifier =
                        Modifier
                            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                            .background(if (row.id == selectedId) colors.selected else colors.canvas)
                            .combinedClickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button,
                                onLongClick = { onMenu(row.id) },
                                onClick = { onOpen(row.id) }
                            )
                )
                if (isAdSlot(index)) Box(Modifier.padding(horizontal = LocalScreenGutter.current)) { adSlot(adOrder(index)) }
            }
        }
    }
}

/**
 * 목록과 함께 움직이는 맨 위 자리.
 * 권하기 카드처럼 잠깐 보이는 안내가 일기 칸을 계속 차지하지 않게 한다.
 * 아래 여백이 늘 있어서 안내가 늦게 떠도 맨 위 줄로 남고, 일기에 밀려 위로 숨지 않는다.
 */
@Composable
internal fun ListTop(content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        content()
        Spacer(Modifier.height(DiarySpacing.x1))
    }
}

/** 다섯째 일기 아래 한 개, 그 뒤 20편마다 한 개(`docs/business.md` 광고 자리). */
internal fun isAdSlot(index: Int): Boolean = index >= FIRST_AD && (index - FIRST_AD) % AD_EVERY == 0

/**
 * 몇 번째 광고 자리인지.
 * 앱이 이 번호로 얇은 줄과 카드를 번갈아 고른다.
 */
internal fun adOrder(index: Int): Int = (index - FIRST_AD) / AD_EVERY

/**
 * 일기 줄 앞에 있는 항목 수.
 * 스크롤 위치를 일기 번호로 바꿀 때 뺀다.
 */
internal const val LIST_TOP_ITEMS = 1
internal const val LIST_TOP_KEY = "list-top"

private const val FIRST_AD = 4
private const val AD_EVERY = 20
