package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
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
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCompany
import org.sjhstudio.diary.core.model.MoodTrend
import org.sjhstudio.diary.core.model.RecentMood
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.StatsChange
import org.sjhstudio.diary.core.model.ToneShare
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.compactCount
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.emphasis
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.core.ui.withEmphasis
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_before
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_good
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_hard
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_top
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_characters_big
import org.sjhstudio.diary.feature.insight.resources.insight_adv_characters_small
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_afternoon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_dawn
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_evening
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_morning
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
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_all
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_better
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_good_down
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_good_up
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_hard_down
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_hard_up
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_harder
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_steady
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_steady_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_share
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weekday
import org.sjhstudio.diary.feature.insight.resources.insight_adv_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_note
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_note_hard
import org.sjhstudio.diary.feature.insight.resources.insight_adv_unit_big
import org.sjhstudio.diary.feature.insight.resources.insight_adv_unit_small
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_fri
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sat
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sun
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_thu
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_tue
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_empty
import org.sjhstudio.diary.feature.insight.resources.insight_write

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

/**
 * 고급 통계(Figma P8).
 * 기분을 누르면 그 일기들로 간다.
 */
@Composable
fun AdvancedStatsRoute(onBack: () -> Unit, onMood: (Mood, DateRange?) -> Unit, modifier: Modifier = Modifier, onWrite: () -> Unit = {}) {
    val viewModel = koinViewModel<AdvancedStatsViewModel>()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }
    AdvancedStatsScreen(period, data, viewModel::select, onBack, { onMood(it, viewModel.range()) }, modifier, onWrite)
}

@Composable
internal fun AdvancedStatsScreen(
    period: AdvancedPeriod,
    stats: AdvancedStats?,
    onPeriod: (AdvancedPeriod) -> Unit,
    onBack: () -> Unit,
    onMood: (Mood) -> Unit,
    modifier: Modifier = Modifier,
    onWrite: () -> Unit = {}
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
                .padding(horizontal = LocalScreenGutter.current)
                .padding(top = DiarySpacing.x2, bottom = DiarySpacing.x6),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x8)
        ) {
            // 빈 한 줄은 탭 아래 칸이라 다른 묶음 사이보다 가깝게 둔다.
            Column {
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
                if (stats?.enough == false) {
                    DiaryEmptyLine(stringResource(Res.string.insight_adv_not_enough), stringResource(Res.string.insight_write), onWrite)
                }
            }
            when {
                stats == null -> AdvancedSkeleton()

                !stats.enough -> Unit

                else -> {
                    stats.recent?.let { Recent(it) }
                    if (!stats.moodReady) NoMoods()
                    stats.change?.let { Change(period, it) }
                    if (stats.moodReady) MoodTimes(stats)
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

/**
 * 일기는 많은데 기분을 고른 일기가 적을 때.
 * 일기가 적다고 말하면 틀린 말이 된다.
 */
@Composable
private fun NoMoods() {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Card {
        BasicText(stringResource(Res.string.insight_adv_no_moods), style = typography.bodyStrong.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.insight_adv_no_moods_body), style = typography.caption.copy(color = colors.textSecondary))
    }
}

/**
 * 요즘 기분 한 줄.
 * 까닭이나 조언 없이 일어난 변화만 말한다.
 */
@Composable
private fun Recent(recent: RecentMood) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val title =
        when (recent.trend) {
            MoodTrend.BETTER -> Res.string.insight_adv_recent_better
            MoodTrend.HARDER -> Res.string.insight_adv_recent_harder
            MoodTrend.STEADY -> Res.string.insight_adv_recent_steady
        }
    val body =
        when {
            recent.trend == MoodTrend.STEADY -> stringResource(Res.string.insight_adv_recent_steady_body)

            recent.hardMoved ->
                stringResource(
                    if (recent.trend ==
                        MoodTrend.BETTER
                    ) {
                        Res.string.insight_adv_recent_hard_down
                    } else {
                        Res.string.insight_adv_recent_hard_up
                    },
                    emphasis(recent.before.hardPercent),
                    emphasis(recent.now.hardPercent)
                )

            else ->
                stringResource(
                    if (recent.trend ==
                        MoodTrend.BETTER
                    ) {
                        Res.string.insight_adv_recent_good_up
                    } else {
                        Res.string.insight_adv_recent_good_down
                    },
                    emphasis(recent.before.percent),
                    emphasis(recent.now.percent)
                )
        }
    Card(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        BasicText(stringResource(title), style = typography.headline.copy(color = colors.textPrimary))
        BasicText(
            body.withEmphasis(SpanStyle(color = colors.textPrimary, fontWeight = typography.bodyStrong.fontWeight)),
            style = typography.body.copy(color = colors.textSecondary)
        )
    }
}

/**
 * 값이 있는 줄만 보인다.
 * 앞 기간은 흐리게, 지금은 또렷하게 둔다.
 */
@Composable
private fun Change(period: AdvancedPeriod, change: StatsChange) {
    val before = change.before
    val now = change.now
    val title = if (period == AdvancedPeriod.THIS_YEAR) Res.string.insight_adv_change_year else Res.string.insight_adv_change_recent
    Section(title, DiarySpacing.x3) {
        val moodsBefore = before.moods
        val moodsNow = now.moods
        if (moodsBefore != null && moodsNow != null) {
            val percent = Res.string.insight_adv_share
            Value(
                stringResource(Res.string.insight_adv_change_good),
                stringResource(percent, moodsBefore.percent),
                stringResource(percent, moodsNow.percent)
            )
            Value(
                stringResource(Res.string.insight_adv_change_hard),
                stringResource(percent, moodsBefore.hardPercent),
                stringResource(percent, moodsNow.hardPercent)
            )
        }
        val topBefore = before.topMood
        val topNow = now.topMood
        if (topBefore != null && topNow != null) {
            Value(stringResource(Res.string.insight_adv_change_top), moodName(topBefore), moodName(topNow))
        }
        Value(
            stringResource(Res.string.insight_adv_habit_days),
            pluralStringResource(Res.plurals.insight_adv_habit_days_value, before.days, before.days.grouped()),
            pluralStringResource(Res.plurals.insight_adv_habit_days_value, now.days, now.days.grouped())
        )
        Value(
            stringResource(Res.string.insight_adv_habit_entries),
            pluralStringResource(Res.plurals.insight_adv_habit_entries_value, before.entries, before.entries.grouped()),
            pluralStringResource(Res.plurals.insight_adv_habit_entries_value, now.entries, now.entries.grouped())
        )
        Value(stringResource(Res.string.insight_adv_habit_characters), characters(before.characters), characters(now.characters))
        Value(
            stringResource(Res.string.insight_adv_habit_photos),
            pluralStringResource(Res.plurals.insight_adv_habit_photos_value, before.photos, before.photos.grouped()),
            pluralStringResource(Res.plurals.insight_adv_habit_photos_value, now.photos, now.photos.grouped())
        )
    }
}

@Composable
private fun Value(label: String, before: String, now: String) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    // 글자가 커져 한 줄에 못 들어가면 값이 다음 줄로 내려간다.
    FlowRow(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(label, style = typography.body.copy(color = colors.textSecondary))
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(Res.string.insight_adv_change_before, before),
                style = typography.body.copy(color = colors.textSecondary)
            )
            BasicText(now, style = typography.bodyStrong.copy(color = colors.textPrimary))
        }
    }
}

/**
 * 글자 수.
 * 십만부터는 묶음 단위로 줄인다.
 * 단위는 언어마다 달라서 문자열에 둔다.
 */
@Composable
private fun characters(value: Long): String {
    val small = stringResource(Res.string.insight_adv_unit_small).toLong()
    val big = stringResource(Res.string.insight_adv_unit_big).toLong()
    val compact = compactCount(value, small, big)
    return when {
        compact == null ->
            pluralStringResource(
                Res.plurals.insight_adv_habit_characters_value,
                value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                value.grouped()
            )

        compact.big -> stringResource(Res.string.insight_adv_characters_big, compact.digits)

        else -> stringResource(Res.string.insight_adv_characters_small, compact.digits)
    }
}

/** 일기 수를 함께 적어 적은 일기로 낸 비율을 크게 믿지 않게 한다. */
@Composable
private fun MoodTimes(stats: AdvancedStats) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var hard by rememberSaveable { mutableStateOf(false) }
    Section(Res.string.insight_adv_when) {
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryChip(stringResource(Res.string.insight_adv_change_good), selected = !hard, onClick = { hard = false })
            DiaryChip(stringResource(Res.string.insight_adv_change_hard), selected = hard, onClick = { hard = true })
        }
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
        val rows: List<Pair<String, ToneShare>> =
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
        val counts = rows.map { (_, share) -> stringResource(Res.string.insight_adv_share, if (hard) share.hardPercent else share.percent) }
        val labelWidth = widestText(rows.map { it.first }, DiaryTheme.typography.caption).coerceAtMost(DiarySpacing.x14 * 2)
        val countWidth = widestText(counts, DiaryTheme.typography.captionStrong)
        rows.forEachIndexed { index, (label, share) ->
            ShareRow(label, counts[index], if (hard) share.hardRate else share.rate, labelWidth, countWidth)
        }
        BasicText(
            stringResource(if (hard) Res.string.insight_adv_tone_note_hard else Res.string.insight_adv_tone_note),
            style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary)
        )
    }
}

@Composable
private fun ShareRow(label: String, count: String, rate: Double, labelWidth: Dp, countWidth: Dp) {
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
            style = typography.caption.copy(color = colors.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DiaryProgressBar(rate.toFloat(), Modifier.weight(1f))
        BasicText(
            count,
            Modifier.width(countWidth),
            style = typography.captionStrong.copy(color = colors.textPrimary, textAlign = TextAlign.End),
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
            stringResource(Res.string.insight_adv_habit_characters) to characters(habit.characters),
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
        BasicText(tileValue(value), style = typography.numeral.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * 숫자를 먼저 읽히게 단위는 작은 보조색으로 둔다.
 * 색을 더하지 않아도 숫자가 앞에 선다(`docs/design.md` 합계 칸).
 */
@Composable
private fun tileValue(value: String): AnnotatedString {
    val colors = DiaryTheme.colors
    val unit = DiaryTheme.typography.caption
    return remember(value, colors, unit) {
        val small = SpanStyle(fontSize = unit.fontSize, fontWeight = FontWeight.Medium, color = colors.textSecondary)
        buildAnnotatedString {
            val number = NUMBER.find(value)
            if (number == null) {
                // 숫자가 없는 값(시간대)은 그대로 둔다.
                append(value)
            } else {
                append(value.substring(0, number.range.last + 1))
                // 크기가 달라 붙어 보이지 않게 숫자와 단위 사이에 작은 빈칸을 둔다.
                withStyle(SpanStyle(fontSize = unit.fontSize * UNIT_GAP)) { append(' ') }
                withStyle(small) { append(value.substring(number.range.last + 1)) }
            }
        }
    }
}

private val NUMBER = Regex("[0-9][0-9,.]*")
private const val UNIT_GAP = 0.5f
