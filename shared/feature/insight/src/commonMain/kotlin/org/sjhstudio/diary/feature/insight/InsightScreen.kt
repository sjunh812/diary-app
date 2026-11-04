package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiarySegmentedTabs
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryInsightSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.moodColor
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_all_time
import org.sjhstudio.diary.feature.insight.resources.insight_count_percent
import org.sjhstudio.diary.feature.insight.resources.insight_empty
import org.sjhstudio.diary.feature.insight.resources.insight_entry_count
import org.sjhstudio.diary.feature.insight.resources.insight_flow_all
import org.sjhstudio.diary.feature.insight.resources.insight_flow_month
import org.sjhstudio.diary.feature.insight.resources.insight_flow_week
import org.sjhstudio.diary.feature.insight.resources.insight_flow_year
import org.sjhstudio.diary.feature.insight.resources.insight_next
import org.sjhstudio.diary.feature.insight.resources.insight_no_mood_count
import org.sjhstudio.diary.feature.insight.resources.insight_no_moods
import org.sjhstudio.diary.feature.insight.resources.insight_previous
import org.sjhstudio.diary.feature.insight.resources.insight_summary_counts
import org.sjhstudio.diary.feature.insight.resources.insight_tab_all
import org.sjhstudio.diary.feature.insight.resources.insight_tab_month
import org.sjhstudio.diary.feature.insight.resources.insight_tab_week
import org.sjhstudio.diary.feature.insight.resources.insight_tab_year
import org.sjhstudio.diary.feature.insight.resources.insight_title
import org.sjhstudio.diary.feature.insight.resources.insight_top_all
import org.sjhstudio.diary.feature.insight.resources.insight_top_month
import org.sjhstudio.diary.feature.insight.resources.insight_top_past
import org.sjhstudio.diary.feature.insight.resources.insight_top_week
import org.sjhstudio.diary.feature.insight.resources.insight_top_year
import org.sjhstudio.diary.feature.insight.resources.insight_week_number
import org.sjhstudio.diary.feature.insight.resources.insight_week_range
import org.sjhstudio.diary.feature.insight.resources.insight_year_map

/** 돌아보기(Figma "돌아보기 (S5)"). 숫자보다 말로 먼저 알려 주고 기분 막대를 누르면 그 일기들로 간다. */
@Composable
fun InsightRoute(onMood: (Mood, DateRange?) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<InsightViewModel>()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val pixels by viewModel.pixels.collectAsStateWithLifecycle()
    InsightScreen(
        period = period,
        counts = counts,
        days = days,
        pixels = pixels,
        today = viewModel.today(),
        onKind = viewModel::select,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onMood = { onMood(it, period.range) },
        modifier = modifier
    )
}

@Composable
internal fun InsightScreen(
    period: StatsPeriod,
    counts: MoodCounts?,
    days: List<DayMood>,
    pixels: Map<LocalDate, Mood?>,
    today: LocalDate,
    onKind: (InsightKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMood: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    Box(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DiarySpacing.screenCompact)
                .padding(top = DiarySpacing.x6, bottom = DiarySpacing.x14 + DiarySpacing.x6)
        ) {
            BasicText(
                stringResource(Res.string.insight_title),
                modifier = Modifier.semantics { heading() },
                style = DiaryTheme.typography.display.copy(color = colors.textPrimary)
            )
            Spacer(Modifier.height(DiarySpacing.x3))
            DiarySegmentedTabs(
                labels =
                    listOf(
                        stringResource(Res.string.insight_tab_week),
                        stringResource(Res.string.insight_tab_month),
                        stringResource(Res.string.insight_tab_year),
                        stringResource(Res.string.insight_tab_all)
                    ),
                selected = period.kind.ordinal,
                onSelect = { onKind(InsightKind.entries[it]) }
            )
            Spacer(Modifier.height(DiarySpacing.x2))
            PeriodNav(periodLabel(period, dates), period.previous() != null, period.next(today) != null, onPrevious, onNext)
            Spacer(Modifier.height(DiarySpacing.x3))
            when {
                counts == null -> Unit
                counts.total == 0 -> Empty()
                else -> Content(period, counts, days, pixels, today, onMood)
            }
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod, dates: DiaryDateFormat): String = when (period) {
    is StatsPeriod.Week -> stringResource(
        Res.string.insight_week_range,
        dates.monthDay(period.range.start),
        dates.monthDay(period.range.end)
    )

    is StatsPeriod.Month -> dates.yearMonth(period.month.onDay(1))

    is StatsPeriod.Year -> dates.year(LocalDate(period.year, 1, 1))

    StatsPeriod.All -> stringResource(Res.string.insight_all_time)
}

@Composable
private fun PeriodNav(label: String, hasPrevious: Boolean, hasNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = DiaryTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavArrow(DiaryIcons.ChevronLeft, stringResource(Res.string.insight_previous), hasPrevious, onPrevious)
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            maxLines = 1
        )
        NavArrow(DiaryIcons.ChevronRight, stringResource(Res.string.insight_next), hasNext, onNext)
    }
}

@Composable
private fun NavArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.full)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 전체 기간처럼 넘길 곳이 없으면 자리는 두고 흐리게 보인다.
        DiaryIcon(icon, contentDescription = description, tint = if (enabled) colors.textPrimary else colors.placeholder)
    }
}

@Composable
private fun Empty() {
    Box(Modifier.fillMaxWidth().padding(vertical = DiarySpacing.x14 + DiarySpacing.x14), contentAlignment = Alignment.Center) {
        BasicText(
            stringResource(Res.string.insight_empty),
            style = DiaryTheme.typography.headline.copy(color = DiaryTheme.colors.textPrimary)
        )
    }
}

@Composable
private fun Content(
    period: StatsPeriod,
    counts: MoodCounts,
    days: List<DayMood>,
    pixels: Map<LocalDate, Mood?>,
    today: LocalDate,
    onMood: (Mood) -> Unit
) {
    val colors = DiaryTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        BasicText(summary(period, counts, today), style = DiaryTheme.typography.title2.copy(color = colors.textPrimary))
        BasicText(countLine(counts), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
    }
    Spacer(Modifier.height(DiarySpacing.x5))
    MoodFlow(period, days, rememberDiaryDateFormat())
    Spacer(Modifier.height(DiarySpacing.x5))
    if (period is StatsPeriod.Year) {
        YearMap(period.year, pixels, today)
        Spacer(Modifier.height(DiarySpacing.x4))
    }
    val most = counts.counts.values.maxOrNull() ?: 0
    val rows = counts.sorted()
    for ((mood, count) in rows) MoodBar(mood, count, most, counts.total, onMood)
}

/** 기분 이름은 모두 받침으로 끝나서 문구에 조사 "이"를 그대로 둔다. */
@Composable
private fun summary(period: StatsPeriod, counts: MoodCounts, today: LocalDate): String {
    val top = counts.top
    if (top.isEmpty()) return stringResource(Res.string.insight_no_moods)
    val names = top.map { moodName(it) }.joinToString(", ")
    return stringResource(summaryResource(period, today), names)
}

private fun summaryResource(period: StatsPeriod, today: LocalDate): StringResource = when {
    period == StatsPeriod.All -> Res.string.insight_top_all
    period.next(today) != null -> Res.string.insight_top_past
    period is StatsPeriod.Week -> Res.string.insight_top_week
    period is StatsPeriod.Month -> Res.string.insight_top_month
    else -> Res.string.insight_top_year
}

@Composable
private fun countLine(counts: MoodCounts): String {
    val entries = pluralStringResource(Res.plurals.insight_entry_count, counts.total, counts.total)
    if (counts.noMood == 0) return entries
    return stringResource(
        Res.string.insight_summary_counts,
        entries,
        pluralStringResource(Res.plurals.insight_no_mood_count, counts.noMood, counts.noMood)
    )
}

/** 기분 흐름. 칸마다 가장 많은 기분의 얼굴을 보인다. 색만 있는 띠는 기분이 여러 가지라 읽히지 않았다. */
@Composable
private fun MoodFlow(period: StatsPeriod, days: List<DayMood>, dates: DiaryDateFormat) {
    val colors = DiaryTheme.colors
    val title =
        stringResource(
            when (period) {
                is StatsPeriod.Week -> Res.string.insight_flow_week
                is StatsPeriod.Month -> Res.string.insight_flow_month
                is StatsPeriod.Year -> Res.string.insight_flow_year
                StatsPeriod.All -> Res.string.insight_flow_all
            }
        )
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        BasicText(title, style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary))
        val buckets = moodBuckets(period, days)
        val columns = if (period is StatsPeriod.Year || period == StatsPeriod.All) WIDE_COLUMNS else buckets.size
        for (row in buckets.chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                for (bucket in row) {
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
                    ) {
                        val mood = bucket.mood
                        if (mood != null) {
                            MoodFace(mood, DiaryInsightSize.flowFace, contentDescription = moodName(mood))
                        } else {
                            Box(
                                Modifier
                                    .size(DiaryInsightSize.flowFace)
                                    .clip(DiaryShapes.full)
                                    .background(if (bucket.written) colors.lineStrong else colors.sunken)
                            )
                        }
                        BasicText(
                            slotLabel(bucket.slot, dates),
                            style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                            maxLines = 1
                        )
                    }
                }
                // 해가 적어도 칸 폭은 같게 둔다.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun slotLabel(slot: BucketSlot, dates: DiaryDateFormat): String = when (slot) {
    is BucketSlot.Day -> dates.weekdayShort(slot.date)
    is BucketSlot.Week -> stringResource(Res.string.insight_week_number, slot.number)
    is BucketSlot.Month -> dates.monthShort(slot.date)
    is BucketSlot.Year -> dates.year(slot.date)
}

/** 연간 지도: 달마다 한 줄, 하루 한 칸(Daylio Year in Pixels와 같은 꼴). 앞으로 올 날은 비워 둔다. */
@Composable
private fun YearMap(year: Int, pixels: Map<LocalDate, Mood?>, today: LocalDate) {
    val colors = DiaryTheme.colors
    val description = stringResource(Res.string.insight_year_map)
    val moodColors = remember(colors) { mutableMapOf<Mood, Color>() }
    for (mood in Mood.entries) moodColors[mood] = moodColor(mood)
    Column(Modifier.semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(DiaryInsightSize.mapGap)) {
        for (number in 1..MONTHS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    number.toString(),
                    modifier = Modifier.width(DiaryInsightSize.mapLabel * LocalDensity.current.fontScale),
                    style = DiaryTheme.typography.micro.copy(color = colors.textTertiary),
                    maxLines = 1
                )
                val month = YearMonth(year, number)
                Canvas(Modifier.weight(1f).height(DiaryInsightSize.mapRow)) {
                    val gap = DiaryInsightSize.mapGap.toPx()
                    val cell = (size.width - gap * (DAYS_MAX - 1)) / DAYS_MAX
                    val radius = CornerRadius(DiaryInsightSize.mapRadius.toPx())
                    for (day in 1..month.numberOfDays) {
                        val date = month.onDay(day)
                        val mood = pixels[date]
                        val color =
                            when {
                                mood != null -> moodColors.getValue(mood)
                                date in pixels -> colors.lineStrong
                                date > today -> null
                                else -> colors.sunken
                            }
                        if (color != null) {
                            drawRoundRect(color, Offset((day - 1) * (cell + gap), 0f), Size(cell, size.height), radius)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodBar(mood: Mood, count: Int, most: Int, total: Int, onClick: (Mood) -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val percent = if (total == 0) 0 else count * PERCENT / total
    // 이름과 숫자 칸은 기기 글자 크기만큼 넓어져야 잘리지 않는다.
    val fontScale = LocalDensity.current.fontScale
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = DiaryInsightSize.barRow)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clickable(enabled = count > 0, interactionSource = interaction, indication = null, role = Role.Button) { onClick(mood) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        MoodFace(mood, DiaryInsightSize.barFace)
        BasicText(
            moodName(mood),
            modifier = Modifier.width(DiaryInsightSize.barName * fontScale),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
            maxLines = 1
        )
        Box(Modifier.weight(1f).height(DiaryInsightSize.barHeight).clip(DiaryShapes.full).background(colors.sunken)) {
            if (count > 0) {
                val filled = count.toFloat() / most
                Box(Modifier.fillMaxWidth(filled).fillMaxHeight().clip(DiaryShapes.full).background(moodColor(mood)))
            }
        }
        BasicText(
            // 없는 기분은 숫자만 흐리게 두어 있는 기분이 먼저 보이게 한다.
            if (count > 0) stringResource(Res.string.insight_count_percent, count, percent) else count.toString(),
            modifier = Modifier.width(DiaryInsightSize.barCount * fontScale),
            style = DiaryTheme.typography.captionStrong.copy(
                color = if (count >
                    0
                ) {
                    colors.textPrimary
                } else {
                    colors.textTertiary
                },
                textAlign = TextAlign.End
            ),
            maxLines = 1
        )
    }
}

private const val MONTHS = 12
private const val WIDE_COLUMNS = 6
private const val DAYS_MAX = 31
private const val PERCENT = 100
