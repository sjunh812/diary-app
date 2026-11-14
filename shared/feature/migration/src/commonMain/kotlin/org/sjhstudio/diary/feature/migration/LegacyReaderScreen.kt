package org.sjhstudio.diary.feature.migration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.DiaryEntryFaceSize
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.monthHeader
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_empty
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_failed
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_notice
import org.sjhstudio.diary.feature.migration.resources.legacy_reader_title
import org.sjhstudio.diary.feature.migration.resources.migration_retry

/**
 * 예전 일기 보기(Figma M4)
 * 기록 이전이 실패한 기기에서 사본을 읽기만 함
 * 줄 모양은 일기 핵심 목록과 같은 일기 줄
 * 기분 얼굴 그림을 그리기 전까지는 기분 색 점으로 보임
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
    // 읽지 못했으면 스스로 다시 부르지 않고 다시 시도 버튼을 기다림
    LaunchedEffect(nearEnd, state.entries.size) { if (nearEnd && !state.end && !state.failed) onLoadMore() }
    val dates = rememberDiaryDateFormat()
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.legacy_reader_title), onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxSize()) {
            item(key = "notice") {
                DiaryNotice(
                    stringResource(Res.string.legacy_reader_notice),
                    Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)
                )
            }
            if (!state.loading && !state.failed && state.entries.isEmpty()) {
                item(key = "empty") {
                    DiaryEmptyState(
                        DiaryIcons.BookOpen,
                        stringResource(Res.string.legacy_reader_empty),
                        Modifier.padding(vertical = DiarySpacing.x10)
                    )
                }
            }
            itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                val previous = state.entries.getOrNull(index - 1)
                val newMonth = previous == null || previous.date.year != entry.date.year || previous.date.month != entry.date.month
                val newDay = previous == null || previous.date != entry.date
                Column {
                    if (newMonth && index > 0) {
                        DiaryMonthHeader(dates.monthHeader(entry.date, currentYear, previous?.date))
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
                            BasicText(
                                dates.time(entry.time),
                                style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            entry.placeName?.let {
                                // 장소가 길어도 뒤의 즐겨찾기 별을 밀어내지 않음
                                BasicText(
                                    it,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }
            if (state.failed) {
                item(key = "failed") {
                    DiaryEmptyState(
                        DiaryIcons.CircleAlert,
                        stringResource(Res.string.legacy_reader_failed),
                        Modifier.padding(vertical = DiarySpacing.x10),
                        actionLabel = stringResource(Res.string.migration_retry),
                        onAction = onLoadMore
                    )
                }
            }
        }
    }
}
