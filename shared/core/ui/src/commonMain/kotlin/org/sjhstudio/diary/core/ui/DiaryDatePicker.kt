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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthWheel
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.currentDate

/**
 * 시트 안의 달력. 화살표로 한 달씩 넘기고, 연월 제목을 누르면 날짜 칸 자리에 연·월 휠이 나와 먼 달로 바로 간다.
 * 제목을 다시 누르면 고른 달의 날짜 칸으로 돌아온다. [days]는 날짜 칸이다.
 */
@Composable
fun DiaryMonthPicker(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier, days: @Composable () -> Unit) {
    val dates = rememberDiaryDateFormat()
    var wheel by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 휠을 여는 동안 화살표는 쓸 일이 없다. 자리는 그대로 둬서 제목이 움직이지 않게 한다.
            if (wheel) {
                Spacer(Modifier.size(DiarySpacing.minTouchTarget))
            } else {
                MonthArrow(DiaryIcons.ChevronLeft) {
                    onChange(month.shifted(-1))
                }
            }
            MonthTitle(dates.yearMonth(month.onDay(1)), wheel, Modifier.weight(1f)) { wheel = !wheel }
            if (wheel) {
                Spacer(Modifier.size(DiarySpacing.minTouchTarget))
            } else {
                MonthArrow(DiaryIcons.ChevronRight) {
                    onChange(month.shifted(1))
                }
            }
        }
        if (wheel) {
            val thisYear = currentDate().year
            DiaryMonthWheel(
                year = month.year,
                month = month.month.number,
                years = minOf(FIRST_YEAR, month.year)..maxOf(thisYear, month.year),
                yearLabel = { dates.year(LocalDate(it, 1, 1)) },
                monthLabel = { dates.month(LocalDate(2026, it, 1)) },
                onChange = { year, number -> onChange(YearMonth(year, number)) }
            )
        } else {
            days()
        }
    }
}

@Composable
private fun MonthTitle(text: String, open: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .heightIn(min = DiarySpacing.minTouchTarget)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = DiarySpacing.x2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
        ) {
            BasicText(text, style = DiaryTheme.typography.headline.copy(color = colors.textPrimary))
            DiaryIcon(
                DiaryIcons.ChevronDown,
                contentDescription = null,
                tint = colors.textSecondary,
                size = DiaryIconSize.i16,
                modifier = Modifier.rotate(if (open) HALF_TURN else 0f)
            )
        }
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
private const val HALF_TURN = 180f

/** 연 휠의 첫해. 이보다 옛 일기가 있으면 그 해부터 보인다. */
private const val FIRST_YEAR = 2010
