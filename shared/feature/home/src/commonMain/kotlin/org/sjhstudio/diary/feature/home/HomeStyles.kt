package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryHomeSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName

/**
 * 카드 목록(Figma "홈 · 카드").
 * 일기마다 카드 하나, 사진은 네 칸까지다.
 */
@Composable
internal fun CardList(
    rows: LazyPagingItems<EntryRow>,
    list: LazyListState,
    selectedId: String?,
    previewStyle: TextStyle,
    dates: DiaryDateFormat,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
    listTop: @Composable () -> Unit,
    adSlot: @Composable (Int) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = list,
        contentPadding =
            PaddingValues(
                start = DiarySpacing.x4,
                end = DiarySpacing.x4,
                bottom = DiarySpacing.x14 + DiarySpacing.x6
            ),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        item(key = LIST_TOP_KEY, contentType = LIST_TOP_KEY) { ListTop(listTop) }
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "card" }) { index ->
            val row = rows[index]
            if (row == null) {
                Box(Modifier.fillMaxWidth().aspectRatio(CARD_PLACEHOLDER_RATIO))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                    EntryCard(row, row.id == selectedId, previewStyle, dates, { onOpen(row.id) }, { onMenu(row.id) })
                    if (isAdSlot(index)) adSlot(adOrder(index))
                }
            }
        }
    }
}

@Composable
private fun EntryCard(
    row: EntryRow,
    selected: Boolean,
    previewStyle: TextStyle,
    dates: DiaryDateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
            .clip(DiaryShapes.r16)
            .background(if (selected) colors.selected else colors.surfaceCard)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(DiarySpacing.x4)
    ) {
        // 기분 얼굴은 날짜 줄과 시각 줄을 합친 높이의 가운데에 둔다(2026-11-06 사용자 지적).
        // 날짜 줄에만 맞추면 위로 떠 보인다.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(Modifier.fillMaxWidth().padding(end = if (row.mood != null) DiaryHomeSize.cardFace + DiarySpacing.x2 else Dp.Hairline)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)) {
                    BasicText(
                        row.date.day.toString(),
                        modifier = Modifier.alignByBaseline(),
                        style = typography.numeral.copy(color = colors.textPrimary)
                    )
                    BasicText(
                        dates.weekday(row.date),
                        modifier = Modifier.alignByBaseline(),
                        style = typography.caption.copy(color = colors.textSecondary)
                    )
                }
                Row(
                    Modifier.padding(top = DiarySpacing.x1),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
                ) {
                    BasicText(
                        dates.time(row.time),
                        style = typography.captionStrong.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    row.weather?.let { WeatherIcon(it, DiaryHomeSize.cardIcon, colors.textSecondary, contentDescription = weatherName(it)) }
                    row.placeName?.let {
                        // 장소가 길어도 시각과 날씨를 밀어내지 않는다.
                        BasicText(
                            it,
                            modifier = Modifier.weight(1f, fill = false),
                            style = typography.caption.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            row.mood?.let { MoodFace(it, DiaryHomeSize.cardFace, contentDescription = moodName(it)) }
        }
        if (row.preview.isNotEmpty()) {
            BasicText(
                row.preview,
                modifier = Modifier.padding(top = DiarySpacing.x2),
                style = previewStyle.copy(color = colors.textPrimary),
                maxLines = PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (row.photos.isNotEmpty()) {
            // 글이 없어도 사진은 시각 줄과 같은 간격으로 떨어진다.
            Row(Modifier.padding(top = DiarySpacing.x3), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                val shown = row.photos.take(CARD_PHOTOS)
                for (photo in shown) {
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(DiaryShapes.r12)) { DiaryPhotoCell(listOf(photo)) }
                }
                repeat(CARD_PHOTOS - shown.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 사진 격자(FR-018).
 * 사진이 없는 일기는 첫 글을 대신 보여서 일기가 빠지지 않게 한다.
 */
@Composable
internal fun PhotoGrid(
    rows: LazyPagingItems<EntryRow>,
    grid: LazyGridState,
    selectedId: String?,
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

                    row.photos.isNotEmpty() -> DiaryPhotoCell(row.photos.take(1))

                    else ->
                        Column(Modifier.padding(DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                            BasicText(
                                dates.monthDay(row.date),
                                style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            BasicText(
                                row.preview,
                                style = DiaryTheme.typography.caption.copy(color = colors.textPrimary),
                                maxLines = GRID_LINES,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                }
            }
        }
    }
}

private const val PREVIEW_LINES = 3
private const val CARD_PHOTOS = 4
private const val CARD_PLACEHOLDER_RATIO = 2f
private const val GRID_LINES = 4
