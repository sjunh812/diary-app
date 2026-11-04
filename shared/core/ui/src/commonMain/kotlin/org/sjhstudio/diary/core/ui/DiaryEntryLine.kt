package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow

/**
 * 목록 한 줄(`docs/design.md` 일기 줄). 메타는 기분 얼굴, 시각, 날씨, 장소, 즐겨찾기 별 순서다.
 * [showDate]는 그날 첫 줄에서만 true다. [previewStyle]은 사용자가 고른 본문 글꼴과 크기다.
 */
@Composable
fun DiaryEntryLine(
    row: EntryRow,
    showDate: Boolean,
    dividerAbove: Boolean,
    modifier: Modifier = Modifier,
    previewStyle: TextStyle = DiaryTheme.typography.body
) {
    val dates = rememberDiaryDateFormat()
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    DiaryEntryRow(
        weekday = if (showDate) dates.weekdayShort(row.date) else null,
        day = if (showDate) row.date.day.toString() else null,
        preview = row.preview,
        modifier = modifier,
        favorite = row.isFavorite,
        dividerAbove = dividerAbove,
        photo = if (row.photos.isEmpty()) null else ({ DiaryPhotoCell(row.photos) }),
        previewStyle = previewStyle,
        meta = {
            row.mood?.let { MoodFace(it, DiaryEntryFaceSize, contentDescription = moodName(it)) }
            BasicText(dates.time(row.time), style = typography.captionStrong.copy(color = colors.textSecondary), maxLines = 1)
            row.weather?.let { WeatherIcon(it, DiaryIconSize.i16, colors.textSecondary, contentDescription = weatherName(it)) }
            row.placeName?.let { BasicText(it, style = typography.caption.copy(color = colors.textSecondary), maxLines = 1) }
        }
    )
}

/** 일기 줄 메타의 기분 얼굴 크기. */
val DiaryEntryFaceSize = 18.dp
