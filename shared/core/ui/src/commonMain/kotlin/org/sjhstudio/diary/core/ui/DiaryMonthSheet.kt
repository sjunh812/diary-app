package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 연·월 고르기(Figma "S1 · 연·월 고르기").
 * 홈은 일기가 있는 달만, 달력은 넘길 수 있는 달만 [enabled]로 연다(FR-017).
 * [hasYear]가 false인 해로는 넘어가지 않는다.
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
            YearArrow(DiaryIcons.ChevronLeft, enabled = hasYear(year - 1)) { year-- }
            BasicText(
                year.toString(),
                modifier = Modifier.weight(1f),
                style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
            )
            YearArrow(DiaryIcons.ChevronRight, enabled = hasYear(year + 1)) { year++ }
        }
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            for (quarter in (1..12).chunked(4)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                    for (number in quarter) {
                        val month = YearMonth(year, number)
                        MonthCell(dates.month(month.onDay(1)), month == shown, enabled(month), Modifier.weight(1f)) { onPick(month) }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearArrow(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = null, tint = if (enabled) colors.textPrimary else colors.placeholder, size = DiaryIconSize.i20)
    }
}

@Composable
private fun MonthCell(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(DiarySpacing.minTouchTarget)
            .clip(DiaryShapes.r12)
            .background(
                when {
                    selected -> colors.selected
                    enabled -> colors.sunken
                    else -> colors.canvas
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
