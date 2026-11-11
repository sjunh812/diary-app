package org.sjhstudio.diary.feature.insight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.ui.DiaryMonthDays
import org.sjhstudio.diary.core.ui.DiaryMonthPicker
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.DiaryYearSheet
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_period_sheet_title

/**
 * 기간 고르기(Figma "시안 · 돌아보기 기간 고르기")
 * 주는 날짜를 눌러 그 주로, 월·년은 칸을 눌러 바로 감. 오늘 뒤로는 가지 않음
 * [firstYear]는 첫 일기의 해. 지금 보는 해가 더 이르면 그 해부터
 */
@Composable
internal fun InsightPeriodSheet(
    period: StatsPeriod,
    today: LocalDate,
    firstYear: Int?,
    onPick: (StatsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val title = stringResource(Res.string.insight_period_sheet_title)
    val shownYear = period.range?.start?.year ?: today.year
    val years = minOf(firstYear ?: today.year, shownYear, today.year)..today.year
    when (period) {
        is StatsPeriod.Week -> WeekSheet(period, today, years.first, title, onPick, onDismiss)

        is StatsPeriod.Month -> DiaryMonthSheet(
            title = title,
            initialYear = period.month.year,
            shown = period.month,
            hasYear = { it in years },
            enabled = { it <= today.yearMonth },
            onPick = { onPick(StatsPeriod.Month(it)) },
            onDismiss = onDismiss
        )

        is StatsPeriod.Year -> DiaryYearSheet(title, years, period.year, { onPick(StatsPeriod.Year(it)) }, onDismiss)

        StatsPeriod.All -> Unit
    }
}

@Composable
private fun WeekSheet(
    week: StatsPeriod.Week,
    today: LocalDate,
    firstYear: Int,
    title: String,
    onPick: (StatsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val first = YearMonth(firstYear, 1).index
    val last = today.yearMonth.index
    // 주가 두 달에 걸치면 오늘에 가까운 쪽 달을 먼저 보임
    // 돌려도 넘기던 달 유지
    var shown by rememberSaveable { mutableIntStateOf(minOf(week.range.end, today).yearMonth.index) }
    val month = monthAt(shown)
    DiaryBottomSheet(onDismiss = onDismiss, title = title) {
        DiaryMonthPicker(
            month,
            onChange = { shown = it.index.coerceIn(first, last) },
            hasPrevious = shown > first,
            hasNext = shown < last
        ) {
            DiaryMonthDays(
                month,
                isPicked = { false },
                onPick = { onPick(StatsPeriod.Week.of(it)) },
                enabled = { StatsPeriod.Week.of(it).start <= today },
                isPickedWeek = { it == week.start }
            )
        }
    }
}

private val YearMonth.index: Int get() = year * MONTHS + month.number - 1

private fun monthAt(index: Int) = YearMonth(index.floorDiv(MONTHS), index.mod(MONTHS) + 1)

private const val MONTHS = 12
