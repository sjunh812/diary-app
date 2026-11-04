package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.DiaryMonthDays
import org.sjhstudio.diary.core.ui.DiaryMonthStepper
import org.sjhstudio.diary.core.ui.DiaryMoodGrid
import org.sjhstudio.diary.core.ui.DiaryWeatherGrid
import org.sjhstudio.diary.feature.search.resources.Res
import org.sjhstudio.diary.feature.search.resources.search_chip_mood
import org.sjhstudio.diary.feature.search.resources.search_chip_period
import org.sjhstudio.diary.feature.search.resources.search_chip_weather
import org.sjhstudio.diary.feature.search.resources.search_clear_filter
import org.sjhstudio.diary.feature.search.resources.search_done
import org.sjhstudio.diary.feature.search.resources.search_period_custom
import org.sjhstudio.diary.feature.search.resources.search_period_month
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

/** 여러 개를 고를 수 있다. 고를 때마다 결과가 바로 바뀐다. */
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        DiaryButton(stringResource(Res.string.search_clear_filter), onClear, Modifier.weight(1f), kind = DiaryButtonKind.Secondary)
        DiaryButton(stringResource(Res.string.search_done), onDone, Modifier.weight(2f))
    }
}

@Composable
internal fun PeriodSheet(selected: SearchPeriod?, onPick: (SearchPeriod?) -> Unit, onCustom: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_chip_period)) {
        for (period in SearchPeriod.entries) {
            val isCustom = period == SearchPeriod.Custom
            PeriodRow(periodLabel(period), period == selected) {
                when {
                    isCustom -> onCustom()
                    period == selected -> onPick(null)
                    else -> onPick(period)
                }
            }
        }
    }
}

@Composable
private fun PeriodRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = DiarySpacing.minTouchTarget)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = DiaryTheme.typography.body.copy(color = if (selected) colors.accentText else colors.textPrimary)
        )
        if (selected) DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.accentText)
    }
}

/** 시작 날과 끝 날을 차례로 누른다. 끝을 시작보다 앞에 누르면 둘을 바꾼다. */
@Composable
internal fun RangeSheet(initial: DateRange?, today: LocalDate, onDone: (DateRange) -> Unit, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf<YearMonth>((initial?.end ?: today).yearMonth) }
    var start by remember { mutableStateOf(initial?.start) }
    var end by remember { mutableStateOf(initial?.end) }
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_period_custom)) {
        DiaryMonthStepper(month, onChange = { month = it })
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
        val from = start
        val to = end
        DiaryButton(
            stringResource(Res.string.search_done),
            { if (from != null && to != null) onDone(DateRange(from, to)) },
            Modifier.fillMaxWidth(),
            enabled = from != null && to != null
        )
    }
}
