package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.DiaryMonthDays
import org.sjhstudio.diary.core.ui.DiaryMonthPicker
import org.sjhstudio.diary.core.ui.DiaryMoodGrid
import org.sjhstudio.diary.core.ui.DiaryWeatherGrid
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.search.resources.Res
import org.sjhstudio.diary.feature.search.resources.search_chip_mood
import org.sjhstudio.diary.feature.search.resources.search_chip_period
import org.sjhstudio.diary.feature.search.resources.search_chip_weather
import org.sjhstudio.diary.feature.search.resources.search_clear_filter
import org.sjhstudio.diary.feature.search.resources.search_done
import org.sjhstudio.diary.feature.search.resources.search_period_custom
import org.sjhstudio.diary.feature.search.resources.search_period_month
import org.sjhstudio.diary.feature.search.resources.search_period_range
import org.sjhstudio.diary.feature.search.resources.search_period_week
import org.sjhstudio.diary.feature.search.resources.search_period_year

@Composable
internal fun periodLabel(period: SearchPeriod): String = stringResource(
    when (period) {
        SearchPeriod.Week -> Res.string.search_period_week
        SearchPeriod.Month -> Res.string.search_period_month
        SearchPeriod.Year -> Res.string.search_period_year
        SearchPeriod.Custom -> Res.string.search_period_custom
    }
)

/**
 * 여러 개를 고를 수 있다.
 * 고를 때마다 결과가 바로 바뀐다.
 */
@Composable
internal fun MoodSheet(selected: Set<Mood>, onChange: (Set<Mood>) -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_chip_mood)) {
        DiaryMoodGrid({ it in selected }, { onChange(if (it in selected) selected - it else selected + it) })
        SheetButtons({ onChange(emptySet()) }, onDismiss)
    }
}

@Composable
internal fun WeatherSheet(selected: Set<Weather>, onChange: (Set<Weather>) -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_chip_weather)) {
        DiaryWeatherGrid({ it in selected }, { onChange(if (it in selected) selected - it else selected + it) })
        SheetButtons({ onChange(emptySet()) }, onDismiss)
    }
}

@Composable
private fun SheetButtons(onClear: () -> Unit, onDone: () -> Unit) {
    DiaryButtonPair(
        stringResource(Res.string.search_clear_filter),
        onClear,
        stringResource(Res.string.search_done),
        onDone,
        size = DiaryButtonSize.XLarge
    )
}

/**
 * 직접 선택한 기간을 짧게 쓴다.
 * 올해 안이면 "11월 1일 ~ 11월 7일", 다른 해가 끼면 연도까지 쓴다. 하루면 그날만 쓴다.
 */
@Composable
internal fun rangeLabel(range: DateRange, today: LocalDate): String {
    val dates = rememberDiaryDateFormat()
    val thisYear = range.start.year == today.year && range.end.year == today.year
    val format: (LocalDate) -> String = if (thisYear) dates::monthDay else dates::fullDate
    if (range.start == range.end) return format(range.start)
    return stringResource(Res.string.search_period_range, format(range.start), format(range.end))
}

@Composable
internal fun PeriodSheet(
    selected: SearchPeriod?,
    customRange: String?,
    onPick: (SearchPeriod?) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_chip_period)) {
        // 고르기 목록의 줄끼리는 시트의 내용 간격 없이 붙인다.
        Column {
            for (period in SearchPeriod.entries) {
                val isCustom = period == SearchPeriod.Custom
                // 직접 선택한 기간은 줄 아래에 짧게 보여, 시트를 열었을 때 무엇을 골랐는지 바로 안다(2026-11-08 사용자 제안).
                val description = if (isCustom && period == selected) customRange else null
                PeriodRow(periodLabel(period), period == selected, description) {
                    when {
                        isCustom -> onCustom()
                        period == selected -> onPick(null)
                        else -> onPick(period)
                    }
                }
            }
        }
    }
}

/**
 * 고르기 목록 줄(`docs/design.md` 고르기 목록).
 * 글은 시트 안쪽 선에 맞추고 누름 면만 글 양옆으로 12 나온다.
 */
@Composable
private fun PeriodRow(label: String, selected: Boolean, description: String?, onClick: () -> Unit) {
    CompositionLocalProvider(LocalScreenGutter provides DiarySpacing.x3) {
        DiarySettingsChoiceRow(
            title = label,
            selected = selected,
            onClick = onClick,
            description = description,
            modifier =
                Modifier.layout { measurable, constraints ->
                    val outset = DiarySpacing.x3.roundToPx()
                    val width = constraints.maxWidth + outset * 2
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-outset, 0) }
                }
        )
    }
}

/**
 * 시작 날과 끝 날을 차례로 누른다.
 * 끝을 시작보다 앞에 누르면 둘을 바꾼다.
 */
@Composable
internal fun RangeSheet(initial: DateRange?, today: LocalDate, onDone: (DateRange) -> Unit, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf<YearMonth>((initial?.end ?: today).yearMonth) }
    var start by remember { mutableStateOf(initial?.start) }
    var end by remember { mutableStateOf(initial?.end) }
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_period_custom)) {
        DiaryMonthPicker(month, onChange = { month = it }) {
            DiaryMonthDays(
                month = month,
                isPicked = { it == start || it == end },
                inRange = { day -> start?.let { from -> end?.let { to -> day in from..to } } ?: false },
                onPick = { day ->
                    val first = start
                    when {
                        first == null || end != null -> {
                            start = day
                            end = null
                        }

                        day < first -> {
                            start = day
                            end = first
                        }

                        else -> end = day
                    }
                }
            )
        }
        val from = start
        val to = end
        DiaryButton(
            stringResource(Res.string.search_done),
            { if (from != null && to != null) onDone(DateRange(from, to)) },
            Modifier.fillMaxWidth().padding(top = DiarySpacing.x3),
            size = DiaryButtonSize.XLarge,
            enabled = from != null && to != null
        )
    }
}
