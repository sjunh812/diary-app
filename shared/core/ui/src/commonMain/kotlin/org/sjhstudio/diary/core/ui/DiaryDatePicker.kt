package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize

/** 시트 안의 달 넘기기 한 줄. */
@Composable
fun DiaryMonthStepper(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val dates = rememberDiaryDateFormat()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonthArrow(DiaryIcons.ChevronLeft) { onChange(month.shifted(-1)) }
        BasicText(
            dates.yearMonth(month.onDay(1)),
            modifier = Modifier.weight(1f),
            style = DiaryTheme.typography.headline.copy(color = DiaryTheme.colors.textPrimary, textAlign = TextAlign.Center)
        )
        MonthArrow(DiaryIcons.ChevronRight) { onChange(month.shifted(1)) }
    }
}

private fun YearMonth.shifted(by: Int): YearMonth {
    val total = year * MONTHS + (month.number - 1) + by
    return YearMonth(total.floorDiv(MONTHS), total.mod(MONTHS) + 1)
}

@Composable
private fun MonthArrow(icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = null, tint = DiaryTheme.colors.textPrimary, size = DiaryIconSize.i20)
    }
}

/** 날짜 고르기 칸. 주는 일요일에 시작하고 앞뒤 달의 날도 흐리게 보인다. [inRange]는 기간을 고를 때 사이의 날이다. */
@Composable
fun DiaryMonthDays(
    month: YearMonth,
    isPicked: (LocalDate) -> Boolean,
    onPick: (LocalDate) -> Unit,
    inRange: (LocalDate) -> Boolean = { false }
) {
    val first = month.onDay(1)
    val start = first.minus(DatePeriod(days = (first.dayOfWeek.ordinal + 1) % DAYS_IN_WEEK))
    Column(Modifier.fillMaxWidth()) {
        var day = start
        while (day <= month.onDay(month.numberOfDays)) {
            Row(Modifier.fillMaxWidth()) {
                repeat(DAYS_IN_WEEK) {
                    val current = day
                    DayCell(current, isPicked(current), inRange(current), inMonth = current.month == first.month, Modifier.weight(1f)) {
                        onPick(current)
                    }
                    day = day.plus(DatePeriod(days = 1))
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, between: Boolean, inMonth: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val color =
        when {
            !inMonth -> colors.placeholder
            selected -> colors.onAccent
            date.dayOfWeek == DayOfWeek.SUNDAY -> colors.danger
            else -> colors.textPrimary
        }
    val background =
        when {
            selected -> colors.accent
            between -> colors.selected
            else -> colors.surface
        }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.height(DiaryWriteSize.dayCell), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(DiaryWriteSize.dayCell)
                .clip(DiaryShapes.full)
                .background(background)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            BasicText(date.day.toString(), style = DiaryTheme.typography.body.copy(color = color))
        }
    }
}

private const val DAYS_IN_WEEK = 7
private const val MONTHS = 12
