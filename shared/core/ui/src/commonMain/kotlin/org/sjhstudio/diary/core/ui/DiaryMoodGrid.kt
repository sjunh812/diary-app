package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

/**
 * 기분 고르기 칸 9개. 쓰기는 하나, 찾기는 여러 개를 고른다.
 * 두 줄이 같은 다섯 칸을 쓰고 아랫줄은 왼쪽부터 채운다.
 * 고른 칸 바탕이 얼굴보다 넓어서 그만큼 왼쪽으로 내어 얼굴을 제목 선에 맞춘다.
 */
@Composable
fun DiaryMoodGrid(isSelected: (Mood) -> Boolean, onToggle: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.offset(x = -DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        for (row in Mood.entries.chunked(MOOD_COLUMNS)) {
            Row(Modifier.fillMaxWidth()) {
                for (mood in row) {
                    Box(Modifier.weight(1f)) { MoodCell(mood, isSelected(mood)) { onToggle(mood) } }
                }
                repeat(MOOD_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoodCell(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Column(
        modifier =
            Modifier
                .width(DiaryWriteSize.moodCellWidth)
                .height(DiaryWriteSize.moodCellHeight)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .clip(DiaryShapes.r12)
                .background(if (selected) colors.selected else colors.surface)
                .semantics { this.selected = selected }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
                .padding(top = DiarySpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        MoodFace(mood, DiaryWriteSize.moodFace)
        BasicText(
            moodName(mood),
            style = (if (selected) DiaryTheme.typography.captionStrong else DiaryTheme.typography.caption).copy(
                color = colors.textSecondary
            ),
            maxLines = 1
        )
    }
}

/** 날씨 고르기 칸 7개. 기분처럼 두 줄이 같은 칸을 쓰고 아이콘과 이름은 칸 왼쪽에 맞춘다. */
@Composable
fun DiaryWeatherGrid(isSelected: (Weather) -> Boolean, onToggle: (Weather) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.offset(x = -DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        for (row in Weather.entries.chunked(WEATHER_COLUMNS)) {
            // 이름이 두 줄인 칸이 있어도 한 줄의 칸은 높이가 같다.
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (weather in row) WeatherCell(weather, isSelected(weather), Modifier.weight(1f).fillMaxHeight()) { onToggle(weather) }
                repeat(WEATHER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WeatherCell(weather: Weather, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .heightIn(min = DiaryWriteSize.weatherCellHeight)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .clip(DiaryShapes.r12)
                .background(if (selected) colors.selected else colors.surface)
                .semantics { this.selected = selected }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x2),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        WeatherIcon(weather, DiaryIconSize.i24, if (selected) colors.accentText else colors.textSecondary)
        BasicText(
            weatherName(weather),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
            maxLines = 2
        )
    }
}

private const val MOOD_COLUMNS = 5
private const val WEATHER_COLUMNS = 4
