package org.sjhstudio.diary.feature.insight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiarySegmentedTabs
import org.sjhstudio.diary.core.designsystem.component.DiaryTabTopBar
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.component.SnapTitleOnRest
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.component.diaryTouchOutset
import org.sjhstudio.diary.core.designsystem.component.titleCollapse
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.CenteredLineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryInsightSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.percentOf
import org.sjhstudio.diary.core.ui.ContextTipBar
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.LoadFailedLine
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.OnTabReselected
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.moodColor
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_title
import org.sjhstudio.diary.feature.insight.resources.insight_count_percent
import org.sjhstudio.diary.feature.insight.resources.insight_empty
import org.sjhstudio.diary.feature.insight.resources.insight_entry_count
import org.sjhstudio.diary.feature.insight.resources.insight_flow_all
import org.sjhstudio.diary.feature.insight.resources.insight_flow_month
import org.sjhstudio.diary.feature.insight.resources.insight_flow_week
import org.sjhstudio.diary.feature.insight.resources.insight_flow_year
import org.sjhstudio.diary.feature.insight.resources.insight_map_hint
import org.sjhstudio.diary.feature.insight.resources.insight_mood_join
import org.sjhstudio.diary.feature.insight.resources.insight_next
import org.sjhstudio.diary.feature.insight.resources.insight_no_mood_count
import org.sjhstudio.diary.feature.insight.resources.insight_no_moods
import org.sjhstudio.diary.feature.insight.resources.insight_period_sheet_title
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
import org.sjhstudio.diary.feature.insight.resources.insight_write
import org.sjhstudio.diary.feature.insight.resources.insight_year_map
import org.sjhstudio.diary.feature.insight.resources.tip_insight_open

/** 돌아보기(Figma "돌아보기 (S5)"). 숫자보다 말로 먼저 알리고, 기분 막대를 누르면 그 일기들로 감 */
@Composable
fun InsightRoute(
    onMood: (Mood, DateRange?) -> Unit,
    modifier: Modifier = Modifier,
    onPeriod: (DateRange) -> Unit = {},
    onPlus: () -> Unit = {},
    onAdvanced: () -> Unit = {},
    onWrite: () -> Unit = {},
    adSlot: @Composable () -> Unit = {}
) {
    val viewModel = koinViewModel<InsightViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period = state.period
    val advancedOpen by viewModel.advancedOpen.collectAsStateWithLifecycle()
    val today by viewModel.todayState.collectAsStateWithLifecycle()
    val firstYear by viewModel.firstYear.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }
    InsightScreen(
        period = period,
        data = state.data,
        failed = state.failed,
        onRetry = viewModel::retry,
        today = today,
        onKind = viewModel::select,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onJump = viewModel::jump,
        firstYear = firstYear,
        onMood = {
            viewModel.tried()
            onMood(it, period.range)
        },
        modifier = modifier,
        onPeriod = {
            viewModel.tried()
            onPeriod(it)
        },
        advancedOpen = advancedOpen,
        onPlus = onPlus,
        onAdvanced = onAdvanced,
        onWrite = onWrite,
        adSlot = adSlot
    )
}

@Composable
internal fun InsightScreen(
    period: StatsPeriod,
    data: InsightData?,
    today: LocalDate,
    onKind: (InsightKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMood: (Mood) -> Unit,
    modifier: Modifier = Modifier,
    onJump: (StatsPeriod) -> Unit = {},
    firstYear: Int? = null,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
    onPeriod: (DateRange) -> Unit = {},
    advancedOpen: Boolean = false,
    onPlus: () -> Unit = {},
    onAdvanced: () -> Unit = {},
    onWrite: () -> Unit = {},
    adSlot: @Composable () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val scroll = rememberScrollState()
    OnTabReselected { scroll.animateScrollTo(0) }
    var titleHeight by remember { mutableIntStateOf(0) }
    SnapTitleOnRest(scroll, titleHeight)
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 큰 제목이 바 밑으로 스크롤된 만큼 바에 작은 제목이 나타남(`docs/design.md` 탭 상단 바)
        // 고급 통계 단추는 탭마다 같아 어느 탭에서나 보이는 바 오른쪽에
        DiaryTabTopBar(
            title = stringResource(Res.string.insight_title),
            collapsed = { titleCollapse(scroll.value, titleHeight) },
            contentMaxWidth = DiaryWriteSize.bodyMax,
            actions = {
                DiaryButton(
                    stringResource(Res.string.insight_adv_title),
                    if (advancedOpen) onAdvanced else onPlus,
                    modifier = Modifier.padding(end = LocalScreenGutter.current - DiarySpacing.x1),
                    size = DiaryButtonSize.Small,
                    kind = DiaryButtonKind.Weak,
                    leadingIcon = DiaryIcons.SparklesSolid
                )
            }
        )
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = LocalScreenGutter.current)
                .padding(bottom = DiarySpacing.x14 + DiarySpacing.x6)
        ) {
            BasicText(
                stringResource(Res.string.insight_title),
                modifier = Modifier.onSizeChanged { titleHeight = it.height }.semantics { heading() },
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
            // 전체는 넘기거나 고를 기간이 없어 기간 줄 없이 요약을 바로 붙임
            if (period == StatsPeriod.All) {
                Spacer(Modifier.height(DiarySpacing.x3))
            } else {
                Spacer(Modifier.height(DiarySpacing.x1))
                PeriodNav(
                    periodLabel(period, dates),
                    period.previous() != null,
                    period.next(today) != null,
                    onPrevious,
                    onNext,
                    onPick = { picking = true }
                )
            }
            // 빈 한 줄은 위 24를 스스로 둠
            // 새 기간을 읽는 동안 앞 기간 숫자는 숨기고 자리만 유지. 빼면 아래 광고가 올라왔다 내려가며 새로 불려 옴
            var read by remember { mutableStateOf(data) }
            SideEffect { if (data != null) read = data }
            val shown = data ?: read
            // 처음 보는 기간을 잠깐 읽는 동안 하얗게 번쩍이지 않게 천천히 옅어짐
            val reduceMotion = rememberReduceMotion()
            val shade by animateFloatAsState(if (data == null) 0f else 1f, tween(if (reduceMotion) 0 else DiaryMotion.QUICK_MILLIS))
            val hidden =
                if (data == null) {
                    Modifier.fillMaxWidth().alpha(shade).clearAndSetSemantics {}
                } else {
                    Modifier.fillMaxWidth().alpha(shade)
                }
            if (shown?.counts?.total != 0) Spacer(Modifier.height(DiarySpacing.x3))
            // 스켈레톤은 0.3초 뒤에 나타나 짧은 틈에는 안 보임
            Box {
                when {
                    shown == null -> Unit

                    shown.counts.total == 0 -> Column(hidden) {
                        DiaryEmptyLine(stringResource(Res.string.insight_empty), stringResource(Res.string.insight_write), onWrite)
                    }

                    else -> Column(hidden) { Content(period, shown.counts, shown.days, shown.pixels, today, onMood, onPeriod) }
                }
                when {
                    data != null -> Unit
                    failed -> LoadFailedLine(onRetry)
                    else -> InsightSkeleton(period.kind)
                }
            }
            if (shown != null && shown.counts.total > 0) {
                // 기분 막대 줄은 위아래가 비어 있어 같은 24로 보이게 줄임
                Spacer(Modifier.height(if (shown.counts.top.isEmpty()) DiarySpacing.x6 else DiarySpacing.x3))
                adSlot()
            }
        }
    }
    if (picking) {
        InsightPeriodSheet(
            period,
            today,
            firstYear,
            onPick = {
                picking = false
                onJump(it)
            },
            onDismiss = { picking = false }
        )
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

    // 전체는 기간 줄을 그리지 않음
    StatsPeriod.All -> ""
}

/** 기간 이름을 누르면 기간 고르기. 전체 기간처럼 고를 곳이 없으면 [onPick]이 null */
@Composable
private fun PeriodNav(
    label: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit
) {
    val colors = DiaryTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 넘길 곳이 없으면 자리는 두고 흐리게
        // 누르는 칸 안쪽 여백만큼 밀어 화살표를 위아래 내용과 같은 선에 맞춤
        DiaryIconButton(
            DiaryIcons.ChevronLeft,
            stringResource(Res.string.insight_previous),
            onPrevious,
            Modifier.offset(x = -DiarySpacing.x3),
            enabled = hasPrevious
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // 이름과 아래 화살표를 한 덩어리로 가운데에. 누르는 면은 달력 시트 제목과 같음
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .heightIn(min = DiarySpacing.minTouchTarget)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(Res.string.insight_period_sheet_title),
                        onClick = onPick
                    )
                    .padding(horizontal = DiarySpacing.x2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodLabel(label)
            }
        }
        DiaryIconButton(
            DiaryIcons.ChevronRight,
            stringResource(Res.string.insight_next),
            onNext,
            Modifier.offset(x = DiarySpacing.x3),
            enabled = hasNext
        )
    }
}

/**
 * 글자가 커져 넘치면 기간이 가려지지 않게 두 줄까지, 줄 길이는 고르게
 * 아래 화살표는 글줄 끝에 붙여 두 줄이어도 글 바로 뒤에 옴
 */
@Composable
private fun PeriodLabel(label: String) {
    val colors = DiaryTheme.colors
    val iconSize = with(LocalDensity.current) { (DiaryIconSize.i16 + DiarySpacing.x1).toSp() }
    val text =
        buildAnnotatedString {
            append(label)
            appendInlineContent(CHEVRON_ID, " ")
        }
    val inline =
        mapOf(
            CHEVRON_ID to
                InlineTextContent(Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.TextCenter)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
                    }
                }
        )
    BasicText(
        text,
        style = DiaryTheme.typography.headline.copy(
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            lineBreak = CenteredLineBreak
        ),
        inlineContent = inline,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun Content(
    period: StatsPeriod,
    counts: MoodCounts,
    days: List<DayMood>,
    pixels: Map<LocalDate, Mood?>,
    today: LocalDate,
    onMood: (Mood) -> Unit,
    onPeriod: (DateRange) -> Unit
) {
    val colors = DiaryTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        // 가장 많았던 기분 이름 앞에 글자 크기만 한 얼굴을 붙여 문장 안에서 먼저 읽히게
        // 굵기나 색으로 나누면 굵기가 하나뿐인 글꼴에서 강조가 사라지거나 문장이 흐려짐
        val title = DiaryTheme.typography.title2
        BasicText(
            summary(period, counts, today),
            style = title.copy(color = colors.textPrimary),
            inlineContent = moodFaces(counts.top.toList(), title.lineHeight)
        )
        BasicText(countLine(counts), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
    }
    Spacer(Modifier.height(DiarySpacing.x6))
    // 기분을 누를 수 있다는 걸 처음 한 번 알림
    // 주별 기분 칸과 자주 고른 기분 줄이 모두 눌려, 처음 만나는 주별 기분 위에 두고 두 곳에 맞는 문구로
    // 안내가 닫히면 자리가 남지 않게 아래 여백은 안내에 붙임
    ContextTipBar(ContextTip.INSIGHT_OPEN, stringResource(Res.string.tip_insight_open), Modifier.padding(bottom = DiarySpacing.x6))
    MoodFlow(period, days, rememberDiaryDateFormat(), onPeriod)
    Spacer(Modifier.height(DiarySpacing.x6))
    if (period is StatsPeriod.Year) {
        SectionTitle(stringResource(Res.string.insight_section_days))
        Spacer(Modifier.height(DiarySpacing.x1))
        PhraseText(stringResource(Res.string.insight_map_hint), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        Spacer(Modifier.height(DiarySpacing.x3))
        YearMap(period.year, pixels, today)
        Spacer(Modifier.height(DiarySpacing.x6))
    }
    // 쓴 적 없는 기분은 줄을 차지하지 않게. 주 보기에서는 0인 줄이 대부분
    val rows = counts.sorted().filter { (_, count) -> count > 0 }
    if (rows.isEmpty()) return
    SectionTitle(stringResource(Res.string.insight_section_moods))
    val most = counts.counts.values.maxOrNull() ?: 0
    // 이름·숫자 칸은 언어와 일기 수로 길이가 달라 가장 긴 글에 맞춰 줄마다 같은 폭
    val names = rows.map { (mood, _) -> moodName(mood) }
    val amounts = rows.map { (_, count) -> moodAmount(count, percentOf(count, counts.total)) }
    // 글자가 아주 커져도 막대 자리가 남게 이름 칸 최대 폭 제한
    val nameWidth = widestText(names, DiaryTheme.typography.caption).coerceAtMost(DiarySpacing.x14 * 2)
    val amountWidth = widestText(amounts, DiaryTheme.typography.captionStrong)
    rows.forEachIndexed { index, (mood, count) ->
        MoodBar(mood, count, most, names[index], nameWidth, amounts[index], amountWidth, onMood)
    }
}

/** 돌아보기 묶음 제목. 기분 흐름, 연간 지도, 기분 막대가 같이 씀 */
@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier.semantics { heading() },
        style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.textSecondary)
    )
}

/** 기분 이름은 모두 받침으로 끝나 문구에 조사 "이"를 그대로 둠 */
@Composable
private fun summary(period: StatsPeriod, counts: MoodCounts, today: LocalDate): AnnotatedString {
    val top = counts.top
    if (top.isEmpty()) return AnnotatedString(stringResource(Res.string.insight_no_moods))
    var names = withFace(0, moodName(top.first()))
    top.drop(1).forEachIndexed { index, mood ->
        names =
            stringResource(Res.string.insight_mood_join, names, withFace(index + 1, moodName(mood)))
    }
    return stringResource(summaryResource(period, today), names).withFaces()
}

private const val CHEVRON_ID = "period-chevron"
private const val FACE_START = '\uE002'
private const val FACE_END = '\uE003'

/** 리소스 자리에 넣을 기분 이름 앞에 얼굴 자리 표시. 어순이 달라도 얼굴이 이름에 붙음 */
private fun withFace(index: Int, name: String): String = "$FACE_START$index$FACE_END$name"

/** 얼굴 자리 표시를 글줄 안 그림 자리로 */
private fun String.withFaces(): AnnotatedString = buildAnnotatedString {
    var rest = this@withFaces
    while (true) {
        val start = rest.indexOf(FACE_START)
        val end = if (start < 0) -1 else rest.indexOf(FACE_END, start)
        if (start < 0 || end < 0) {
            append(rest)
            break
        }
        append(rest.substring(0, start))
        val index = rest.substring(start + 1, end)
        // 대체 글자가 비면 Compose가 예외를 던져 빈칸 하나로 둠(화면 읽기·복사용)
        appendInlineContent(faceId(index), " ")
        rest = rest.substring(end + 1)
    }
}

private fun faceId(index: Any) = "mood-face-$index"

/** 글줄 높이만 한 기분 얼굴. 뒤 글자와 조금 띄움 */
@Composable
private fun moodFaces(moods: List<Mood>, lineHeight: TextUnit): Map<String, InlineTextContent> {
    val size = with(LocalDensity.current) { lineHeight.toDp() } * FACE_SCALE
    return moods.mapIndexed { index, mood ->
        faceId(index) to
            InlineTextContent(
                Placeholder(lineHeight * (FACE_SCALE + FACE_GAP), lineHeight * FACE_SCALE, PlaceholderVerticalAlign.TextCenter)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { MoodFace(mood, size, contentDescription = null) }
            }
    }.toMap()
}

/** 얼굴은 글줄 높이의 0.86배(20/28 글자에서 24), 뒤 글자와 사이는 그 0.14배 */
private const val FACE_SCALE = 0.86f
private const val FACE_GAP = 0.14f

private fun summaryResource(period: StatsPeriod, today: LocalDate): StringResource = when {
    period == StatsPeriod.All -> Res.string.insight_top_all
    period.next(today) != null -> Res.string.insight_top_past
    period is StatsPeriod.Week -> Res.string.insight_top_week
    period is StatsPeriod.Month -> Res.string.insight_top_month
    else -> Res.string.insight_top_year
}

@Composable
private fun countLine(counts: MoodCounts): String {
    val entries = pluralStringResource(Res.plurals.insight_entry_count, counts.total, counts.total.grouped())
    if (counts.noMood == 0) return entries
    return stringResource(
        Res.string.insight_summary_counts,
        entries,
        pluralStringResource(Res.plurals.insight_no_mood_count, counts.noMood, counts.noMood.grouped())
    )
}

/** 기분 흐름. 칸마다 가장 많은 기분의 얼굴. 색만 있는 띠는 기분이 여러 가지라 읽히지 않음 */
@Composable
private fun MoodFlow(period: StatsPeriod, days: List<DayMood>, dates: DiaryDateFormat, onPeriod: (DateRange) -> Unit) {
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
    Column {
        SectionTitle(title)
        Spacer(Modifier.height(DiarySpacing.x3))
        // 줄 사이를 얼굴·이름 사이보다 넓혀 이름이 어느 얼굴 것인지 헷갈리지 않게
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            val buckets = remember(period, days) { moodBuckets(period, days) }
            val columns = if (period is StatsPeriod.Year || period == StatsPeriod.All) WIDE_COLUMNS else buckets.size
            for (row in buckets.chunked(columns)) {
                Row(Modifier.fillMaxWidth()) {
                    for (bucket in row) {
                        // 일기가 있는 칸을 누르면 그 기간 일기를 찾기로 모아 봄
                        // 칸 얼굴은 기간 요약이라 기분으로 거르지 않고 그때 쓴 일기를 모두 보임
                        val press = remember { MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .diaryPress(press, colors.textPrimary, DiaryShapes.r12)
                                .clickable(
                                    enabled = bucket.written,
                                    interactionSource = press,
                                    indication = null,
                                    role = Role.Button
                                ) { onPeriod(bucket.range) }
                                .padding(vertical = DiarySpacing.x1),
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
                    // 해가 적어도 칸 폭은 같게
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * 고른 기분이 없는 칸. 기분을 보는 줄이라 일기를 썼는지는 구분하지 않음
 * 쓴 날은 달력에, 기분 없는 일기 수는 위 요약 줄에 있음
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

/**
 * 연간 지도: 달마다 한 줄, 하루 한 칸(Daylio Year in Pixels와 같은 꼴)
 * 칸은 정사각형, 달 번호는 칸보다 커 세 달마다. 앞으로 올 날은 비움
 */
@Composable
private fun YearMap(year: Int, pixels: Map<LocalDate, Mood?>, today: LocalDate) {
    val colors = DiaryTheme.colors
    val description = stringResource(Res.string.insight_year_map, year)
    val moodColors = Mood.entries.associateWith { moodColor(it) }
    val labelWidth = DiaryInsightSize.mapLabel * LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.semantics { contentDescription = description }) {
        val cell = (maxWidth - labelWidth - DiaryInsightSize.mapGap * (DAYS_MAX - 1)) / DAYS_MAX
        Column(verticalArrangement = Arrangement.spacedBy(DiaryInsightSize.mapGap)) {
            for (number in 1..MONTHS) {
                Row(Modifier.height(cell), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(labelWidth).wrapContentHeight(unbounded = true)) {
                        if (number % MAP_LABEL_EVERY == 1) {
                            BasicText(
                                number.toString(),
                                style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                                maxLines = 1
                            )
                        }
                    }
                    val month = YearMonth(year, number)
                    Canvas(Modifier.weight(1f).height(cell)) {
                        val gap = DiaryInsightSize.mapGap.toPx()
                        val side = size.height
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
                                drawRoundRect(color, Offset((day - 1) * (side + gap), 0f), Size(side, side), radius)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 없는 기분은 숫자만 */
@Composable
private fun moodAmount(count: Int, percent: Int): String =
    if (count > 0) stringResource(Res.string.insight_count_percent, count.grouped(), percent) else count.grouped()

/** 글이 잘리지 않게 담을 폭. 글자 크기와 고른 글꼴을 따름 */
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
            // 누름 면이 얼굴·숫자에 붙지 않게 양옆으로 12 내어 그리고, 안쪽을 같은 만큼 들여 내용은 제자리
            .diaryTouchOutset(horizontal = DiarySpacing.x3)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clickable(enabled = count > 0, interactionSource = interaction, indication = null, role = Role.Button) { onClick(mood) }
            .padding(horizontal = DiarySpacing.x3),
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
        // 트랙 없음. 웃음·슬픔 파스텔은 sunken과 밝기가 거의 같아 끝이 안 보이고,
        // 막대는 가장 많은 기분을 끝으로 재 트랙이 뜻하는 값도 없음
        Box(Modifier.weight(1f).height(DiaryInsightSize.barHeight)) {
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
internal const val WIDE_COLUMNS = 6
private const val MAP_LABEL_EVERY = 3
private const val DAYS_MAX = 31
