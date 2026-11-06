package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySegmentedTabs
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.GoodShare
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCompany
import org.sjhstudio.diary.core.model.MoodGap
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.StatsChange
import org.sjhstudio.diary.core.model.StatsFactor
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.SkeletonRows
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_days
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_days_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_good
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_good_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_afternoon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_dawn
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_evening
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_morning
import org.sjhstudio.diary.feature.insight.resources.insight_adv_entry_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_factor_long
import org.sjhstudio.diary.feature.insight.resources.insight_adv_factor_photos
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_less
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_more
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_none
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_none_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_note
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_numbers
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_others
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gap_rate
import org.sjhstudio.diary.feature.insight.resources.insight_adv_gaps
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_characters
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_characters_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_days
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_days_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_entries
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_entries_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_photos
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_photos_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_run
import org.sjhstudio.diary.feature.insight.resources.insight_adv_no_moods
import org.sjhstudio.diary.feature.insight.resources.insight_adv_no_moods_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_not_enough
import org.sjhstudio.diary.feature.insight.resources.insight_adv_not_enough_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_all
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_share
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weekday
import org.sjhstudio.diary.feature.insight.resources.insight_adv_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_note
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_fri
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sat
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sun
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_thu
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_tue
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_empty

private val Weekdays: List<Pair<DayOfWeek, StringResource>> =
    listOf(
        DayOfWeek.SUNDAY to Res.string.insight_adv_wd_sun,
        DayOfWeek.MONDAY to Res.string.insight_adv_wd_mon,
        DayOfWeek.TUESDAY to Res.string.insight_adv_wd_tue,
        DayOfWeek.WEDNESDAY to Res.string.insight_adv_wd_wed,
        DayOfWeek.THURSDAY to Res.string.insight_adv_wd_thu,
        DayOfWeek.FRIDAY to Res.string.insight_adv_wd_fri,
        DayOfWeek.SATURDAY to Res.string.insight_adv_wd_sat
    )

private val DayParts: List<Pair<DayPart, StringResource>> =
    listOf(
        DayPart.DAWN to Res.string.insight_adv_day_dawn,
        DayPart.MORNING to Res.string.insight_adv_day_morning,
        DayPart.AFTERNOON to Res.string.insight_adv_day_afternoon,
        DayPart.EVENING to Res.string.insight_adv_day_evening
    )

/** 돌아보기 맨 아래 입구(Figma P6). 무료는 눌러서 Plus 안내로, Plus는 고급 통계로 간다(FR-006). */
@Composable
internal fun AdvancedEntry(action: StringResource, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .diaryPress(press, colors.textPrimary, DiaryShapes.r16)
            .clip(DiaryShapes.r16)
            .background(colors.selected)
            .clickable(interactionSource = press, indication = null, role = Role.Button, onClick = onClick)
            .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(stringResource(Res.string.insight_adv_title), style = typography.bodyStrong.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.insight_adv_entry_body), style = typography.caption.copy(color = colors.textSecondary))
        BasicText(
            stringResource(action),
            Modifier.padding(top = DiarySpacing.x2),
            style = typography.captionStrong.copy(color = colors.accentText)
        )
    }
}

/** 고급 통계(Figma P8). 기분을 누르면 그 일기들로 간다. */
@Composable
fun AdvancedStatsRoute(onBack: () -> Unit, onMood: (Mood, DateRange?) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AdvancedStatsViewModel>()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    AdvancedStatsScreen(period, data, viewModel::select, onBack, { onMood(it, viewModel.range()) }, modifier)
}

@Composable
internal fun AdvancedStatsScreen(
    period: AdvancedPeriod,
    stats: AdvancedStats?,
    onPeriod: (AdvancedPeriod) -> Unit,
    onBack: () -> Unit,
    onMood: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.insight_adv_title), onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = DiarySpacing.screenCompact)
                .padding(top = DiarySpacing.x2, bottom = DiarySpacing.x6),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x8)
        ) {
            DiarySegmentedTabs(
                labels =
                    listOf(
                        stringResource(Res.string.insight_adv_range_recent),
                        stringResource(Res.string.insight_adv_range_year),
                        stringResource(Res.string.insight_adv_range_all)
                    ),
                selected = period.ordinal,
                onSelect = { onPeriod(AdvancedPeriod.entries[it]) }
            )
            when {
                stats == null -> SkeletonRows(rows = 5)

                !stats.enough ->
                    DiaryEmptyState(
                        DiaryIcons.TabInsight,
                        stringResource(Res.string.insight_adv_not_enough),
                        description = stringResource(Res.string.insight_adv_not_enough_body)
                    )

                else -> {
                    if (stats.moodReady) Gaps(stats.gaps) else NoMoods()
                    stats.change?.let { Change(period, it) }
                    if (stats.moodReady) GoodTimes(stats)
                    if (stats.company.isNotEmpty()) Company(stats.company, onMood)
                    Habit(stats.habit)
                }
            }
        }
    }
}

@Composable
private fun Section(title: StringResource, gap: Dp = DiarySpacing.x3, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        BasicText(
            stringResource(title),
            Modifier.semantics { heading() },
            style = DiaryTheme.typography.headline.copy(color = DiaryTheme.colors.textPrimary)
        )
        content()
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = DiaryTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(DiaryShapes.r16)
            .background(colors.surfaceCard)
            .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
        content = content
    )
}

/** 일기는 많은데 기분을 고른 일기가 적을 때. 일기가 적다고 말하면 틀린 말이 된다. */
@Composable
private fun NoMoods() {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Section(Res.string.insight_adv_gaps) {
        Card {
            BasicText(stringResource(Res.string.insight_adv_no_moods), style = typography.bodyStrong.copy(color = colors.textPrimary))
            BasicText(stringResource(Res.string.insight_adv_no_moods_body), style = typography.caption.copy(color = colors.textSecondary))
        }
    }
}

/** 우연으로 보기 어려운 차이만 보인다. 없으면 없다고 말한다. */
@Composable
private fun Gaps(gaps: List<MoodGap>) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Section(Res.string.insight_adv_gaps) {
        if (gaps.isEmpty()) {
            Card {
                BasicText(stringResource(Res.string.insight_adv_gap_none), style = typography.bodyStrong.copy(color = colors.textPrimary))
                BasicText(
                    stringResource(Res.string.insight_adv_gap_none_body),
                    style = typography.caption.copy(color = colors.textSecondary)
                )
            }
        } else {
            gaps.forEach { gap ->
                Card(Modifier.semantics(mergeDescendants = true) {}) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                            BasicText(factorName(gap.factor), style = typography.captionStrong.copy(color = colors.accentText))
                            BasicText(
                                stringResource(if (gap.more) Res.string.insight_adv_gap_more else Res.string.insight_adv_gap_less),
                                style = typography.bodyStrong.copy(color = colors.textPrimary)
                            )
                            BasicText(
                                stringResource(Res.string.insight_adv_gap_numbers, gap.here.total, gap.here.good),
                                style = typography.caption.copy(color = colors.textSecondary)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
                            BasicText(
                                stringResource(Res.string.insight_adv_gap_rate, gap.here.percent),
                                style = typography.title2.copy(color = colors.textPrimary)
                            )
                            BasicText(
                                stringResource(Res.string.insight_adv_gap_others, gap.others.percent),
                                style = typography.caption.copy(color = colors.textSecondary)
                            )
                        }
                    }
                }
            }
            BasicText(stringResource(Res.string.insight_adv_gap_note), style = typography.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
private fun factorName(factor: StatsFactor): String = when (factor) {
    is StatsFactor.Sky -> weatherName(factor.weather)
    is StatsFactor.Weekday -> stringResource(Weekdays.first { it.first == factor.day }.second)
    is StatsFactor.Part -> stringResource(DayParts.first { it.first == factor.part }.second)
    is StatsFactor.Place -> factor.name
    StatsFactor.WithPhotos -> stringResource(Res.string.insight_adv_factor_photos)
    StatsFactor.LongEntries -> stringResource(Res.string.insight_adv_factor_long)
}

@Composable
private fun Change(period: AdvancedPeriod, change: StatsChange) {
    val before = change.goodBefore
    val now = change.goodNow
    val title = if (period == AdvancedPeriod.THIS_YEAR) Res.string.insight_adv_change_year else Res.string.insight_adv_change_recent
    Section(title, DiarySpacing.x2) {
        if (before != null && now != null) {
            Value(
                stringResource(Res.string.insight_adv_change_good),
                stringResource(Res.string.insight_adv_change_good_value, before.percent, now.percent)
            )
        }
        Value(
            stringResource(Res.string.insight_adv_change_days),
            pluralStringResource(Res.plurals.insight_adv_change_days_value, change.daysNow, change.daysBefore, change.daysNow)
        )
    }
}

@Composable
private fun Value(label: String, value: String) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = typography.reading.copy(color = colors.textPrimary))
        BasicText(value, style = typography.body.copy(color = colors.textSecondary))
    }
}

/** 일기 수를 함께 적어 적은 일기로 낸 비율을 크게 믿지 않게 한다. */
@Composable
private fun GoodTimes(stats: AdvancedStats) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Section(Res.string.insight_adv_when) {
        DiarySegmentedTabs(
            labels =
                listOf(
                    stringResource(Res.string.insight_adv_tab_weekday),
                    stringResource(Res.string.insight_adv_tab_part),
                    stringResource(Res.string.insight_adv_tab_weather),
                    stringResource(Res.string.insight_adv_tab_place)
                ),
            selected = tab,
            onSelect = { tab = it }
        )
        val rows: List<Pair<String, GoodShare>> =
            when (tab) {
                0 -> Weekdays.mapNotNull { (day, label) -> stats.weekdays[day]?.let { stringResource(label) to it } }
                1 -> DayParts.mapNotNull { (part, label) -> stats.dayParts[part]?.let { stringResource(label) to it } }
                2 -> stats.weather.entries.sortedByDescending { it.value.total }.map { weatherName(it.key) to it.value }
                else -> stats.places
            }
        if (rows.isEmpty()) {
            BasicText(
                stringResource(Res.string.insight_adv_when_empty),
                style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary)
            )
        }
        val counts = rows.map { (_, share) -> stringResource(Res.string.insight_adv_share, share.good, share.total) }
        val labelWidth = widestText(rows.map { it.first }, DiaryTheme.typography.body).coerceAtMost(DiarySpacing.x14 * 2)
        val countWidth = widestText(counts, DiaryTheme.typography.caption)
        rows.forEachIndexed { index, (label, share) -> ShareRow(label, counts[index], share, labelWidth, countWidth) }
        BasicText(
            stringResource(Res.string.insight_adv_tone_note),
            style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary)
        )
    }
}

@Composable
private fun ShareRow(label: String, count: String, share: GoodShare, labelWidth: Dp, countWidth: Dp) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$label $count" },
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            Modifier.width(labelWidth),
            style = typography.body.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DiaryProgressBar(share.rate.toFloat(), Modifier.weight(1f))
        BasicText(
            count,
            Modifier.width(countWidth),
            style = typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.End),
            maxLines = 1
        )
    }
}

@Composable
private fun Company(company: List<MoodCompany>, onMood: (Mood) -> Unit) {
    Section(Res.string.insight_adv_company, DiarySpacing.x1) {
        company.forEach { row -> CompanyRow(row) { onMood(row.mood) } }
    }
}

@Composable
private fun CompanyRow(row: MoodCompany, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val press = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .diaryPress(press, colors.textPrimary, DiaryShapes.r12)
            .clickable(interactionSource = press, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoodFace(row.mood, DiarySpacing.x8, contentDescription = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BasicText(
                pluralStringResource(Res.plurals.insight_adv_company_title, row.entries, moodName(row.mood), row.entries.grouped()),
                style = typography.bodyStrong.copy(color = colors.textPrimary)
            )
            val together =
                listOfNotNull(
                    row.weather?.let { weatherName(it) },
                    row.weekday?.let { day -> stringResource(Weekdays.first { it.first == day }.second) },
                    row.place
                )
            BasicText(
                together.joinToString(" · "),
                style = typography.caption.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DiaryIcon(DiaryIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary, size = DiaryIconSize.i20)
    }
}

@Composable
private fun Habit(habit: RecordHabit) {
    val part = habit.topPart?.let { top -> stringResource(DayParts.first { it.first == top }.second) }.orEmpty()
    val tiles =
        listOf(
            stringResource(Res.string.insight_adv_habit_entries) to
                pluralStringResource(Res.plurals.insight_adv_habit_entries_value, habit.entries, habit.entries.grouped()),
            stringResource(Res.string.insight_adv_habit_days) to
                pluralStringResource(Res.plurals.insight_adv_habit_days_value, habit.daysWritten, habit.daysWritten.grouped()),
            stringResource(Res.string.insight_adv_habit_run) to
                pluralStringResource(Res.plurals.insight_adv_habit_days_value, habit.longestRun, habit.longestRun.grouped()),
            stringResource(Res.string.insight_adv_habit_characters) to
                pluralStringResource(
                    Res.plurals.insight_adv_habit_characters_value,
                    habit.characters.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    habit.characters.grouped()
                ),
            stringResource(Res.string.insight_adv_habit_photos) to
                pluralStringResource(Res.plurals.insight_adv_habit_photos_value, habit.photos, habit.photos.grouped()),
            stringResource(Res.string.insight_adv_habit_part) to part
        )
    Section(Res.string.insight_adv_habit) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                pair.forEach { (label, value) -> Tile(label, value, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Card(modifier.semantics(mergeDescendants = true) {}) {
        BasicText(label, style = typography.caption.copy(color = colors.textSecondary))
        BasicText(value, style = typography.title2.copy(color = colors.textPrimary))
    }
}
