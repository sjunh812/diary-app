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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberShareText
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_share
import org.sjhstudio.diary.feature.home.resources.detail_trash
import org.sjhstudio.diary.feature.home.resources.home_empty_action
import org.sjhstudio.diary.feature.home.resources.home_empty_body
import org.sjhstudio.diary.feature.home.resources.home_empty_title
import org.sjhstudio.diary.feature.home.resources.home_favorites_empty
import org.sjhstudio.diary.feature.home.resources.home_favorites_only
import org.sjhstudio.diary.feature.home.resources.home_month_sheet_title
import org.sjhstudio.diary.feature.home.resources.home_search

/**
 * 홈(Figma "홈 (S1)"). 큰 제목은 지금 보이는 달을 보이고 누르면 그 달로 간다.
 * 넓은 화면에서는 왼쪽 칸이 되고 고른 줄을 [selectedId]로 표시한다.
 */
@Composable
fun HomeRoute(
    onOpen: (String) -> Unit,
    onWrite: () -> Unit,
    onSearch: () -> Unit,
    onTrashed: (String) -> Unit,
    selectedId: String?,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val rows = viewModel.rows.collectAsLazyPagingItems()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val months by viewModel.months.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dates = rememberDiaryDateFormat()
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var picking by remember { mutableStateOf(false) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    // 목록이 비어 있으면 오늘이 든 달을 보인다.
    val shown = rows.itemCount.takeIf { it > 0 }?.let { rows.peek(list.firstVisibleItemIndex.coerceAtMost(it - 1))?.date } ?: today
    HomeScreen(
        title = dates.yearMonth(shown),
        rows = rows,
        list = list,
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
        modifier = modifier
    )
    menuFor?.let { id ->
        // iOS의 공유 창은 화면 컨트롤러가 있어야 해서 메뉴를 열 때만 준비한다.
        val share = rememberShareText()
        DiaryBottomSheet(onDismiss = { menuFor = null }) {
            SheetAction(DiaryIcons.Share, stringResource(Res.string.detail_share), DiaryTheme.colors.textPrimary) {
                menuFor = null
                scope.launch { viewModel.plainText(id)?.let(share) }
            }
            SheetAction(DiaryIcons.Trash, stringResource(Res.string.detail_trash), DiaryTheme.colors.danger) {
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
                scope.launch { list.scrollToItem(viewModel.positionOf(month)) }
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
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding()) {
        TopActions(favoritesOnly, onToggleFavorites, onSearch)
        Title(title, onPickMonth)
        Spacer(Modifier.height(DiarySpacing.x3))
        when {
            rows.itemCount > 0 -> {
                val dates = rememberDiaryDateFormat()
                when (style) {
                    ListStyle.FEED -> Feed(rows, list, selectedId, today, previewStyle, dates, onOpen, onMenu)
                    ListStyle.CARD -> CardList(rows, list, selectedId, previewStyle, dates, onOpen, onMenu)
                    ListStyle.PHOTO_GRID -> PhotoGrid(rows, selectedId, dates, onOpen, onMenu)
                }
            }

            rows.loadState.refresh is LoadState.NotLoading -> Empty(favoritesOnly, onWrite)
        }
    }
}

@Composable
private fun TopActions(favoritesOnly: Boolean, onToggle: () -> Unit, onSearch: () -> Unit) {
    val colors = DiaryTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(end = DiarySpacing.x1, top = DiarySpacing.x1),
        horizontalArrangement = Arrangement.End
    ) {
        val searching = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(DiarySpacing.minTouchTarget)
                .diaryPress(searching, colors.textPrimary, DiaryShapes.full)
                .clickable(interactionSource = searching, indication = null, role = Role.Button, onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(DiaryIcons.Search, contentDescription = stringResource(Res.string.home_search), tint = colors.textPrimary)
        }
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
private fun Title(text: String, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .padding(horizontal = DiarySpacing.screenCompact)
            .semantics { heading() }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(text, style = DiaryTheme.typography.display.copy(color = colors.textPrimary), maxLines = 1)
        DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i20)
    }
}

@Composable
private fun Empty(favoritesOnly: Boolean, onWrite: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (favoritesOnly) {
            DiaryEmptyState(DiaryIcons.StarOutline, stringResource(Res.string.home_favorites_empty))
        } else {
            DiaryEmptyState(
                DiaryIcons.Pen,
                stringResource(Res.string.home_empty_title),
                description = stringResource(Res.string.home_empty_body),
                actionLabel = stringResource(Res.string.home_empty_action),
                onAction = onWrite
            )
        }
    }
}

/** 달이 바뀌면 달 머리를, 날이 바뀌면 날짜 열을 보인다. 그날 둘째 줄부터 날짜 열은 비운다. */
@Composable
private fun Feed(
    rows: LazyPagingItems<EntryRow>,
    list: LazyListState,
    selectedId: String?,
    today: LocalDate,
    previewStyle: TextStyle,
    dates: DiaryDateFormat,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit
) {
    val colors = DiaryTheme.colors
    // 마지막 줄이 쓰기 버튼에 가리지 않게 아래를 비운다.
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = DiarySpacing.x14 + DiarySpacing.x6)) {
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
            }
        }
    }
}
