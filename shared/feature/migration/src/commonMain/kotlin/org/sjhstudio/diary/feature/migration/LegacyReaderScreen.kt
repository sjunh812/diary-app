package org.sjhstudio.diary.feature.migration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.migration.LegacyEntryView
import org.sjhstudio.diary.core.ui.DiaryEntryFaceSize
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_empty
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_notice
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_title

/**
 * 예전 일기 보기(Figma M4). 기록 이전이 실패한 기기에서 사본을 읽기만 한다.
 * 줄 모양은 일기 핵심 목록과 같은 일기 줄이다. 기분 얼굴 그림을 그리기 전까지는 기분 색 점으로 보인다.
 */
@Composable
fun LegacyReaderScreen(
    state: LegacyReaderState,
    currentYear: Int,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 10
        }
    }
    LaunchedEffect(nearEnd, state.entries.size) { if (nearEnd && !state.end) onLoadMore() }
    val dates = rememberDiaryDateFormat()
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding()) {
        DiaryTopBar(title = stringResource(Res.string.legacy_reader_title), onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "notice") {
                DiaryNotice(
                    stringResource(Res.string.legacy_reader_notice),
                    Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2)
                )
            }
            if (!state.loading && state.entries.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(DiarySpacing.x10), contentAlignment = Alignment.Center) {
                        BasicText(
                            stringResource(Res.string.legacy_reader_empty),
                            style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
            itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                val previous = state.entries.getOrNull(index - 1)
                val newMonth = previous == null || previous.date.year != entry.date.year || previous.date.month != entry.date.month
                val newDay = previous == null || previous.date != entry.date
                Column {
                    if (newMonth && index > 0) {
                        DiaryMonthHeader(if (entry.date.year == currentYear) dates.month(entry.date) else dates.yearMonth(entry.date))
                    }
                    DiaryEntryRow(
                        weekday = if (newDay) dates.weekdayShort(entry.date) else null,
                        day = if (newDay) entry.date.day.toString() else null,
                        preview = entry.text,
                        favorite = entry.favorite,
                        dividerAbove = index > 0 && !newMonth,
                        photo = if (entry.photos.isEmpty()) null else ({ DiaryPhotoCell(entry.photos) }),
                        meta = {
                            entry.mood?.let { MoodFace(it, DiaryEntryFaceSize) }
                            for (item in listOfNotNull(dates.time(entry.time), entry.placeName)) {
                                BasicText(
                                    item,
                                    style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary),
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
