package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryHomeSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryEntryCard
import org.sjhstudio.diary.core.ui.DiaryGridCellContent
import org.sjhstudio.diary.core.ui.entryMarks
import org.sjhstudio.diary.core.ui.loadMoreFailed
import org.sjhstudio.diary.core.ui.monthHeader

/**
 * 카드 목록(Figma "홈 · 카드"). 일기마다 카드 하나, 사진은 네 칸까지
 * 피드형처럼 달이 바뀌는 자리에 달 머리. 올해가 아니거나 해가 바뀌면 연도까지
 */
@Composable
internal fun CardList(
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
    LazyColumn(
        Modifier.fillMaxSize(),
        state = list,
        // 달 머리는 화면 여백에 맞추고 카드만 안쪽으로
        // 카드 사이 12는 카드 위에만 둠. 목록 맨 위 칸과 첫 카드 사이는 피드형과 같은 백업 줄 아래 12(Figma S1-b)
        contentPadding = PaddingValues(bottom = DiarySpacing.x14 + DiarySpacing.x6)
    ) {
        item(key = LIST_TOP_KEY, contentType = LIST_TOP_KEY) { ListTop(listTop, Modifier.padding(horizontal = DiarySpacing.x4)) }
        loadMoreFailed(rows.loadState.prepend is LoadState.Error, rows::retry, "prepend_failed")
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "card" }) { index ->
            val row = rows[index]
            if (row == null) {
                Box(Modifier.padding(top = DiarySpacing.x3).fillMaxWidth().aspectRatio(CARD_PLACEHOLDER_RATIO))
            } else {
                val previous = if (index > 0) rows.peek(index - 1) else null
                Column(
                    Modifier.padding(top = if (index > 0) DiarySpacing.x3 else Dp.Hairline),
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
                ) {
                    if (index > 0 && entryMarks(index, row, previous).newMonth) {
                        DiaryMonthHeader(dates.monthHeader(row.date, today.year, previous?.date))
                    }
                    Column(Modifier.padding(horizontal = DiarySpacing.x4), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                        val clipped = remember(row.id) { ClippedFlag() }
                        DiaryEntryCard(
                            row,
                            previewStyle,
                            dates,
                            selected = row.id == selectedId,
                            onClick = {
                                if (row.previewCut || clipped.value) onOpenClippedRow()
                                onOpen(row.id)
                            },
                            onLongClick = { onMenu(row.id) },
                            onPreviewLayout = { clipped.value = it.hasVisualOverflow }
                        )
                        if (isAdSlot(index)) adSlot(adOrder(index), Modifier)
                    }
                }
            }
        }
        loadMoreFailed(rows.loadState.append is LoadState.Error, rows::retry)
    }
}

/** 사진 격자(FR-018). 사진 없는 일기는 첫 글을 대신 보여 일기가 빠지지 않게 */
@Composable
internal fun PhotoGrid(
    rows: LazyPagingItems<EntryRow>,
    grid: LazyGridState,
    selectedId: String?,
    today: LocalDate,
    dates: DiaryDateFormat,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
    listTop: @Composable () -> Unit
) {
    val colors = DiaryTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Adaptive(DiaryHomeSize.gridCell),
        modifier = Modifier.fillMaxSize(),
        state = grid,
        contentPadding = PaddingValues(start = DiarySpacing.x0_5, end = DiarySpacing.x0_5, bottom = DiarySpacing.x14 + DiarySpacing.x6),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)
    ) {
        item(key = LIST_TOP_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = LIST_TOP_KEY) {
            ListTop(listTop, Modifier.padding(horizontal = LocalScreenGutter.current - DiarySpacing.x0_5))
        }
        loadMoreFailed(rows.loadState.prepend is LoadState.Error, rows::retry, "prepend_failed")
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "photo" }) { index ->
            val row = rows[index]
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .aspectRatio(1f)
                    .diaryPress(interaction, colors.textPrimary)
                    .background(if (row?.id == selectedId) colors.selected else colors.sunken)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onLongClick = { row?.let { onMenu(it.id) } },
                        onClick = { row?.let { onOpen(it.id) } }
                    )
            ) {
                when {
                    row == null -> Unit
                    else -> DiaryGridCellContent(row, today, dates)
                }
            }
        }
        loadMoreFailed(rows.loadState.append is LoadState.Error, rows::retry)
    }
}

private const val CARD_PLACEHOLDER_RATIO = 2f
