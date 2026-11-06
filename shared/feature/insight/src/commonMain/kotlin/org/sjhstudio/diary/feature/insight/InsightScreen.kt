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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
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
import org.sjhstudio.diary.core.ui.SkeletonRows
import org.sjhstudio.diary.core.ui.moodColor
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_entry_open
import org.sjhstudio.diary.feature.insight.resources.insight_adv_entry_plus
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
import org.sjhstudio.diary.feature.insight.resources.insight_section_days
import org.sjhstudio.diary.feature.insight.resources.insight_section_moods
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
fun InsightRoute(
    onMood: (Mood, DateRange?) -> Unit,
    modifier: Modifier = Modifier,
    onPlus: () -> Unit = {},
    onAdvanced: () -> Unit = {},
    adSlot: @Composable () -> Unit = {}
) {
    val viewModel = koinViewModel<InsightViewModel>()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val pixels by viewModel.pixels.collectAsStateWithLifecycle()
    val advancedOpen by viewModel.advancedOpen.collectAsStateWithLifecycle()
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
        modifier = modifier,
        advancedOpen = advancedOpen,
        onPlus = onPlus,
        onAdvanced = onAdvanced,
        adSlot = adSlot
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
    modifier: Modifier = Modifier,
    advancedOpen: Boolean = false,
    onPlus: () -> Unit = {},
    onAdvanced: () -> Unit = {},
    adSlot: @Composable () -> Unit = {}
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
                .padding(top = DiarySpacing.tabTitleTop, bottom = DiarySpacing.x14 + DiarySpacing.x6)
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
                counts == null -> SkeletonRows(rows = 5)
                counts.total == 0 -> Empty()
                else -> Content(period, counts, days, pixels, today, onMood)
            }
            if (counts != null && counts.total > 0) {
                Spacer(Modifier.height(DiarySpacing.x8))
                if (advancedOpen) {
                    AdvancedEntry(Res.string.insight_adv_entry_open, onAdvanced)
                } else {
                    AdvancedEntry(Res.string.insight_adv_entry_plus, onPlus)
                }
                Spacer(Modifier.height(DiarySpacing.x4))
                adSlot()
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
        // 전체 기간처럼 넘길 곳이 없으면 자리는 두고 흐리게 보인다.
        DiaryIconButton(DiaryIcons.ChevronLeft, stringResource(Res.string.insight_previous), onPrevious, enabled = hasPrevious)
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DiaryIconButton(DiaryIcons.ChevronRight, stringResource(Res.string.insight_next), onNext, enabled = hasNext)
    }
}

@Composable
private fun Empty() {
    // 화면 전체가 스크롤되어 빈 상태를 칸에 놓을 수 없다. 전과 같이 위아래 여백으로 자리를 잡는다.
    DiaryEmptyState(
        DiaryIcons.TabInsight,
        stringResource(Res.string.insight_empty),
        Modifier.padding(vertical = DiarySpacing.x14 + DiarySpacing.x14)
    )
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
        SectionTitle(stringResource(Res.string.insight_section_days))
        Spacer(Modifier.height(DiarySpacing.x3))
        YearMap(period.year, pixels, today)
        Spacer(Modifier.height(DiarySpacing.x5))
    }
    SectionTitle(stringResource(Res.string.insight_section_moods))
    Spacer(Modifier.height(DiarySpacing.x1))
    val most = counts.counts.values.maxOrNull() ?: 0
    val rows = counts.sorted()
    // 이름과 숫자 칸은 언어와 일기 수로 길이가 달라져서, 가장 긴 글에 맞춰 줄마다 같은 폭으로 둔다.
    val names = rows.map { (mood, _) -> moodName(mood) }
    val amounts = rows.map { (_, count) -> moodAmount(count, percentOf(count, counts.total)) }
    val nameWidth = widestText(names, DiaryTheme.typography.caption)
    val amountWidth = widestText(amounts, DiaryTheme.typography.captionStrong)
    rows.forEachIndexed { index, (mood, count) ->
        MoodBar(mood, count, most, names[index], nameWidth, amounts[index], amountWidth, onMood)
    }
}

/** 돌아보기 묶음 제목. 기분 흐름, 연간 지도, 기분 막대가 같이 쓴다. */
@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier.semantics { heading() },
        style = DiaryTheme.typography.captionStrong.copy(color = DiaryTheme.colors.textSecondary)
    )
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
        SectionTitle(title)
        val buckets = remember(period, days) { moodBuckets(period, days) }
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
                            EmptyMoodSlot(DiaryInsightSize.flowFace)
                        }
                        BasicText(
                            slotLabel(bucket.slot, dates),
                            style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 해가 적어도 칸 폭은 같게 둔다.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 고른 기분이 없는 칸. 여기는 기분을 보는 줄이라 일기를 썼는지는 가르지 않는다.
 * 쓴 날은 달력이 보이고, 기분 없는 일기의 수는 위 요약 줄에 있다.
 */
@Composable
private fun EmptyMoodSlot(size: Dp) {
    Box(Modifier.size(size).clip(DiaryShapes.full).background(DiaryTheme.colors.sunken))
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
    val moodColors = Mood.entries.associateWith { moodColor(it) }
    Column(Modifier.semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(DiaryInsightSize.mapGap)) {
        for (number in 1..MONTHS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    number.toString(),
                    modifier = Modifier.width(DiaryInsightSize.mapLabel * LocalDensity.current.fontScale),
                    style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

private fun percentOf(count: Int, total: Int): Int = if (total == 0) 0 else count * PERCENT / total

/** 없는 기분은 숫자만 둔다. */
@Composable
private fun moodAmount(count: Int, percent: Int): String =
    if (count > 0) stringResource(Res.string.insight_count_percent, count, percent) else count.toString()

/** 글을 잘리지 않게 담을 폭. 글자 크기와 고른 글꼴을 그대로 따른다. */
@Composable
internal fun widestText(texts: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts, style, density, measurer) {
        with(density) { (texts.maxOfOrNull { measurer.measure(it, style, maxLines = 1).size.width } ?: 0).toDp() }
    }
}

@Composable
private fun MoodBar(
    mood: Mood,
    count: Int,
    most: Int,
    name: String,
    nameWidth: Dp,
    amount: String,
    amountWidth: Dp,
    onClick: (Mood) -> Unit
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
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
            name,
            modifier = Modifier.width(nameWidth),
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
            amount,
            modifier = Modifier.width(amountWidth),
            style = DiaryTheme.typography.captionStrong.copy(
                color = if (count > 0) colors.textPrimary else colors.textSecondary,
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
