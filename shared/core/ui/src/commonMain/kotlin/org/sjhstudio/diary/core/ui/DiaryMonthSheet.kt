package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.ui_next_year
import org.sjhstudio.diary.core.ui.resources.ui_previous_year

/**
 * 연·월 고르기(Figma "S1 · 연·월 고르기")
 * 홈은 일기가 있는 달만, 달력은 넘길 수 있는 달만 [enabled]로 열기(FR-017)
 * [hasYear]가 false인 해로는 넘어가지 않음
 */
@Composable
fun DiaryMonthSheet(
    title: String,
    initialYear: Int,
    shown: YearMonth,
    hasYear: (Int) -> Boolean,
    enabled: (YearMonth) -> Boolean,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    var year by remember { mutableIntStateOf(initialYear) }
    DiaryBottomSheet(onDismiss = onDismiss, title = title) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            YearArrow(DiaryIcons.ChevronLeft, stringResource(Res.string.ui_previous_year), enabled = hasYear(year - 1)) { year-- }
            BasicText(
                year.toString(),
                modifier = Modifier.weight(1f),
                style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
            )
            YearArrow(DiaryIcons.ChevronRight, stringResource(Res.string.ui_next_year), enabled = hasYear(year + 1)) { year++ }
        }
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            for (quarter in (1..12).chunked(CELLS_IN_ROW)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                    for (number in quarter) {
                        val month = YearMonth(year, number)
                        MonthCell(dates.monthShort(month.onDay(1)), month == shown, enabled(month), Modifier.weight(1f)) { onPick(month) }
                    }
                }
            }
        }
    }
}

/** 해 고르기. 달 고르기와 같은 칸으로 한 줄에 넷 */
@Composable
fun DiaryYearSheet(
    title: String,
    years: IntRange,
    shown: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = rememberDiaryDateFormat()
    DiaryBottomSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            for (row in years.toList().chunked(CELLS_IN_ROW)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                    for (year in row) {
                        MonthCell(dates.year(LocalDate(year, 1, 1)), year == shown, enabled = true, Modifier.weight(1f)) { onPick(year) }
                    }
                    // 해가 모자란 줄도 칸 폭은 같게
                    repeat(CELLS_IN_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val CELLS_IN_ROW = 4

@Composable
private fun YearArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.full)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(
            icon,
            contentDescription = label,
            tint = if (enabled) colors.textPrimary else colors.placeholder,
            size = DiaryIconSize.i20
        )
    }
}

@Composable
private fun MonthCell(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clip(DiaryShapes.r12)
            // 못 고르는 달은 칠하지 않음
            // canvas로 칠하면 다크 시트 위에서 구멍처럼 보임
            .then(
                when {
                    selected -> Modifier.background(colors.selected)
                    enabled -> Modifier.background(colors.sunken)
                    else -> Modifier
                }
            )
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val color =
            when {
                selected -> colors.accentText
                enabled -> colors.textPrimary
                else -> colors.placeholder
            }
        BasicText(label, style = DiaryTheme.typography.label.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
