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
import androidx.compose.foundation.text.TextAutoSize
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodGroup
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.mood_group_body
import org.sjhstudio.diary.core.ui.resources.mood_group_good
import org.sjhstudio.diary.core.ui.resources.mood_group_mind
import org.sjhstudio.diary.core.ui.resources.mood_group_neutral

/**
 * 기분 고르기 칸 15개. 묶음([MoodGroup])마다 이름 한 줄과 5칸 줄
 * 쓰기는 하나, 찾기는 여러 개를 고름
 * 칸이 곧 누르는 곳이라 칸끼리 맞닿게 두고 격자는 가운데
 * 좁은 폭에서는 첫·마지막 얼굴이 내용 선을 넘지 않을 만큼 칸을 줄임
 * 모자란 줄은 왼쪽부터 채워 위 줄과 세로 줄을 맞춤
 */
@Composable
fun DiaryMoodGrid(isSelected: (Mood) -> Boolean, onToggle: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.layout { measurable, constraints ->
            val width = constraints.maxWidth
            val face = DiaryWriteSize.moodFace.roundToPx()
            // 칸 다섯 개가 내용 폭보다 넓으면 양쪽 얼굴 바깥 여백만큼만 넘침
            val fit = (width - face) / (GRID_COLUMNS - 1)
            val cell = minOf(DiaryWriteSize.moodCellWidth.roundToPx(), fit)
            val grid = cell * GRID_COLUMNS
            val placeable = measurable.measure(constraints.copy(minWidth = grid, maxWidth = grid))
            layout(width, placeable.height) { placeable.placeRelative((width - grid) / 2, 0) }
        },
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        MoodGroup.entries.forEachIndexed { index, group ->
            // 묶음 사이는 이름 위 12를 더해 16
            MoodGroupName(group, if (index == 0) Modifier else Modifier.padding(top = DiarySpacing.x3))
            for (row in group.moods.chunked(GRID_COLUMNS)) {
                Row(Modifier.fillMaxWidth()) {
                    for (mood in row) {
                        Box(Modifier.weight(1f)) { MoodCell(mood, isSelected(mood)) { onToggle(mood) } }
                    }
                    repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 묶음 이름. 첫 칸 얼굴 선에 맞춤 */
@Composable
private fun MoodGroupName(group: MoodGroup, modifier: Modifier = Modifier) {
    val face = DiaryWriteSize.moodFace
    BasicText(
        stringResource(group.label()),
        modifier.layout { measurable, constraints ->
            val inset = ((constraints.maxWidth / GRID_COLUMNS - face.roundToPx()) / 2).coerceAtLeast(0)
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth - inset))
            layout(constraints.maxWidth, placeable.height) { placeable.placeRelative(inset, 0) }
        },
        style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun MoodGroup.label(): StringResource = when (this) {
    MoodGroup.GOOD -> Res.string.mood_group_good
    MoodGroup.MIND -> Res.string.mood_group_mind
    MoodGroup.BODY -> Res.string.mood_group_body
    MoodGroup.NEUTRAL -> Res.string.mood_group_neutral
}

@Composable
private fun MoodCell(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    // 누르는 곳은 칸 전체, 칠하는 바탕만 양옆 2 안으로
    // 찾기에서 여러 개를 고를 때 칠한 칸끼리 하나로 붙어 보이지 않게
    Box(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .padding(horizontal = DiarySpacing.x0_5)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiaryWriteSize.moodCellHeight)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
                    .clip(DiaryShapes.r16)
                    // 시트와 쓰기 옆 칸 어디서나 바탕 위에 그대로 놓이게 고른 칸만 칠함
                    .then(if (selected) Modifier.background(colors.selected) else Modifier)
                    .padding(vertical = DiarySpacing.x2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
        ) {
            MoodFace(mood, DiaryWriteSize.moodFace)
            MoodName(moodName(mood), selected)
        }
    }
}

/** 기분 칸 아래 이름 */
@Composable
internal fun MoodName(name: String, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val style =
        (if (selected) DiaryTheme.typography.captionStrong else DiaryTheme.typography.caption).copy(
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    // 칸 폭이 고정이라 긴 이름은 두 줄까지
    // 한 낱말은 글자 단위로 쪼개지지 않게 한 줄로 두고, 넘치면 글자를 조금 줄임(영어 Laughing, 일본어 具合が悪い)
    // 말줄임을 함께 걸면 줄이기 전에 …로 맞춰 버려 줄이기가 동작하지 않음
    val oneWord = ' ' !in name
    BasicText(
        name,
        modifier = modifier,
        style = style,
        maxLines = if (oneWord) 1 else NAME_LINES,
        overflow = if (oneWord) TextOverflow.Clip else TextOverflow.Ellipsis,
        autoSize = if (oneWord) {
            TextAutoSize.StepBased(
                minFontSize = style.fontSize * MIN_NAME_SCALE,
                maxFontSize = style.fontSize
            )
        } else {
            null
        }
    )
}

/**
 * 날씨 고르기
 * 기분이 주인공이라 날씨는 칩으로 가볍게 두고 고른 칩만 색을 칠함
 * 칩은 폭만큼 줄을 바꿔서 긴 영어 이름이나 큰 글씨에서도 잘리지 않음
 */
@Composable
fun DiaryWeatherGrid(isSelected: (Weather) -> Boolean, onToggle: (Weather) -> Unit, modifier: Modifier = Modifier) {
    // 칩 줄 높이가 누르는 곳 48이라 줄 사이를 더 띄우지 않음
    // 보이는 틈은 16
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        for (weather in Weather.entries) {
            DiaryChip(weatherName(weather), isSelected(weather), { onToggle(weather) }, leadingIcon = weather.icon())
        }
    }
}

private const val GRID_COLUMNS = 5
private const val NAME_LINES = 2

/** 한 낱말 이름을 줄일 수 있는 끝(본래 크기의 80%) */
private const val MIN_NAME_SCALE = 0.8f
