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
 * 주는 날짜를 눌러 그 주로, 월·년은 칸을 눌러 바로 감
 * 화살표와 같은 범위: [first]가 든 기간부터 오늘이 든 기간까지. 지금 보는 기간이 더 이르면 그 기간부터
 */
@Composable
internal fun InsightPeriodSheet(
    period: StatsPeriod,
    today: LocalDate,
    first: LocalDate,
    onPick: (StatsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val title = stringResource(Res.string.insight_period_sheet_title)
    val from = minOf(first, period.range?.start ?: first)
    val years = from.year..today.year
    when (period) {
        is StatsPeriod.Week -> WeekSheet(period, today, from, title, onPick, onDismiss)

        is StatsPeriod.Month -> DiaryMonthSheet(
            title = title,
            initialYear = period.month.year,
            shown = period.month,
            years = years.toList(),
            enabled = { it in from.yearMonth..today.yearMonth },
            onPick = { onPick(StatsPeriod.Month(it)) },
            onDismiss = onDismiss
        )

        is StatsPeriod.Year -> DiaryYearSheet(title, years, period.year, { onPick(StatsPeriod.Year(it)) }, onDismiss)

        StatsPeriod.All -> Unit
    }
}

/** [from]이 든 달부터 넘기고, [from]이 든 주부터 고름 */
@Composable
private fun WeekSheet(
    week: StatsPeriod.Week,
    today: LocalDate,
    from: LocalDate,
    title: String,
    onPick: (StatsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val first = from.yearMonth.index
    val earliest = StatsPeriod.Week.of(from).start
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
                enabled = { it >= earliest && StatsPeriod.Week.of(it).start <= today },
                isPickedWeek = { it == week.start }
            )
        }
    }
}

private val YearMonth.index: Int get() = year * MONTHS + month.number - 1

private fun monthAt(index: Int) = YearMonth(index.floorDiv(MONTHS), index.mod(MONTHS) + 1)

private const val MONTHS = 12
