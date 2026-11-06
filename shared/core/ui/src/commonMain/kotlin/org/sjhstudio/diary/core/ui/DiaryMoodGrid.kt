package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

/**
 * 기분 고르기 칸 9개.
 * 쓰기는 하나, 찾기는 여러 개를 고른다.
 * 두 줄이 같은 다섯 칸을 쓰고 아랫줄은 왼쪽부터 채운다.
 * 칸이 얼굴보다 넓어서 그만큼 왼쪽으로 내어 얼굴을 제목 선에 맞춘다.
 */
@Composable
fun DiaryMoodGrid(isSelected: (Mood) -> Boolean, onToggle: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val side = (placeable.width / GRID_COLUMNS - DiaryWriteSize.moodFace.roundToPx()) / 2
            layout(placeable.width, placeable.height) { placeable.placeRelative(-side, 0) }
        },
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        for (row in Mood.ordered.chunked(GRID_COLUMNS)) {
            Row(Modifier.fillMaxWidth()) {
                for (mood in row) {
                    Box(Modifier.weight(1f)) { MoodCell(mood, isSelected(mood)) { onToggle(mood) } }
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
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
                .fillMaxWidth()
                .heightIn(min = DiaryWriteSize.moodCellHeight)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
                .clip(DiaryShapes.r16)
                .background(if (selected) colors.selected else colors.surface)
                .semantics { this.selected = selected }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
                .padding(horizontal = DiarySpacing.x0_5, vertical = DiarySpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        MoodFace(mood, DiaryWriteSize.moodFace)
        BasicText(
            moodName(mood),
            style = (if (selected) DiaryTheme.typography.captionStrong else DiaryTheme.typography.caption).copy(
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            ),
            // 칸 폭이 고정이라 긴 이름은 두 줄까지 쓴다.
            maxLines = NAME_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 날씨 고르기.
 * 기분이 주인공이라 날씨는 칩으로 가볍게 두고, 고른 칩만 색이 든다.
 * 칩은 폭만큼 줄을 바꿔서 긴 영어 이름이나 큰 글씨에서도 잘리지 않는다.
 */
@Composable
fun DiaryWeatherGrid(isSelected: (Weather) -> Boolean, onToggle: (Weather) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        for (weather in Weather.entries) {
            DiaryChip(weatherName(weather), isSelected(weather), { onToggle(weather) }, leadingIcon = weather.icon())
        }
    }
}

private const val GRID_COLUMNS = 5
private const val NAME_LINES = 2
