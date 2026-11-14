package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.sjhstudio.diary.core.designsystem.component.DiaryDateSlot
import org.sjhstudio.diary.core.designsystem.component.DiaryDateSlots
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
import org.sjhstudio.diary.feature.search.resources.search_pick_many
import org.sjhstudio.diary.feature.search.resources.search_range_end
import org.sjhstudio.diary.feature.search.resources.search_range_pick
import org.sjhstudio.diary.feature.search.resources.search_range_start

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
 * 여러 개를 고를 수 있음. 시트에서 고른 것은 [완료]를 눌러야 결과에 씀
 * 결과가 시트에 가려 바로 바꿔도 안 보이고, 모바일 필터는 다 고른 뒤 한 번에 적용하는 편이 나음(NN/g)
 * 내려서 닫으면 고르기 전으로
 */
@Composable
internal fun MoodSheet(selected: Set<Mood>, onApply: (Set<Mood>) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable(stateSaver = enumSetSaver { Mood.valueOf(it) }) { mutableStateOf(selected) }
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.search_chip_mood),
        description = stringResource(Res.string.search_pick_many)
    ) {
        DiaryMoodGrid({ it in draft }, { draft = if (it in draft) draft - it else draft + it })
        SheetButtons({ draft = emptySet() }) {
            onApply(draft)
            onDismiss()
        }
    }
}

@Composable
internal fun WeatherSheet(selected: Set<Weather>, onApply: (Set<Weather>) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable(stateSaver = enumSetSaver { Weather.valueOf(it) }) { mutableStateOf(selected) }
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.search_chip_weather),
        description = stringResource(Res.string.search_pick_many)
    ) {
        DiaryWeatherGrid({ it in draft }, { draft = if (it in draft) draft - it else draft + it })
        SheetButtons({ draft = emptySet() }) {
            onApply(draft)
            onDismiss()
        }
    }
}

/** 화면을 돌려도 시트에서 고르던 것이 남게 이름 목록으로 저장 */
private fun <T : Enum<T>> enumSetSaver(restore: (String) -> T): Saver<Set<T>, Any> =
    listSaver({ set -> set.map { it.name } }, { names -> names.map(restore).toSet() })

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
 * 직접 선택한 기간을 짧게
 * 올해 안이면 "11월 1일 ~ 11월 7일", 다른 해가 끼면 연도까지, 하루면 그날만
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
        // 고르기 목록 줄끼리는 시트 내용 간격 없이 붙임
        Column {
            for (period in SearchPeriod.entries) {
                val isCustom = period == SearchPeriod.Custom
                // 직접 선택한 기간은 줄 아래에 짧게 보여 시트를 열면 무엇을 골랐는지 바로 알게
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

/** 고르기 목록 줄(`docs/design.md` 고르기 목록). 글은 시트 안쪽 선에 맞추고 누름 면만 양옆으로 12 나옴 */
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
 * 시작일과 종료일을 차례로 누름. 달력 위 두 칸이 다음에 누를 날을 알려 줌
 * 종료일을 시작일보다 앞에 누르면 둘을 바꿈. 둘 다 고른 뒤 다른 날을 누르면 그날로 새로 시작
 * 시작일만 고르고 [완료]를 누르면 그날 하루로 찾음
 */
@Composable
internal fun RangeSheet(initial: DateRange?, today: LocalDate, onDone: (DateRange) -> Unit, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf<YearMonth>((initial?.end ?: today).yearMonth) }
    var start by remember { mutableStateOf(initial?.start) }
    var end by remember { mutableStateOf(initial?.end) }
    var active by remember { mutableStateOf<DiaryDateSlot?>(if (initial == null) DiaryDateSlot.Start else null) }
    val dates = rememberDiaryDateFormat()
    val label: (LocalDate) -> String = { if (it.year == today.year) dates.monthDay(it) else dates.fullDate(it) }
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.search_period_custom)) {
        DiaryDateSlots(
            startLabel = stringResource(Res.string.search_range_start),
            start = start?.let(label),
            endLabel = stringResource(Res.string.search_range_end),
            end = end?.let(label),
            placeholder = stringResource(Res.string.search_range_pick),
            active = active,
            onStart = { active = DiaryDateSlot.Start },
            onEnd = { active = DiaryDateSlot.End }.takeIf { start != null }
        )
        DiaryMonthPicker(month, onChange = { month = it }) {
            DiaryMonthDays(
                month = month,
                isPicked = { it == start || it == end },
                inRange = { day -> start?.let { from -> end?.let { to -> day in from..to } } ?: false },
                onPick = { day ->
                    val first = start
                    when {
                        // 시작일을 고르는 중이거나, 둘 다 고른 뒤 다시 누르면 그날로 새로 시작
                        first == null || active != DiaryDateSlot.End -> {
                            start = day
                            end = end?.takeIf { active == DiaryDateSlot.Start && it >= day }
                            active = if (end == null) DiaryDateSlot.End else null
                        }

                        day < first -> {
                            start = day
                            end = first
                            active = null
                        }

                        else -> {
                            end = day
                            active = null
                        }
                    }
                }
            )
        }
        val from = start
        DiaryButton(
            stringResource(Res.string.search_done),
            { if (from != null) onDone(DateRange(from, end ?: from)) },
            Modifier.fillMaxWidth().padding(top = DiarySpacing.x3),
            size = DiaryButtonSize.XLarge,
            enabled = from != null
        )
    }
}
