package org.sjhstudio.diary.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryCalendarSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.NoMoodFace
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.moodColor
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.feature.calendar.resources.Res
import org.sjhstudio.diary.feature.calendar.resources.calendar_day_count
import org.sjhstudio.diary.feature.calendar.resources.calendar_day_summary
import org.sjhstudio.diary.feature.calendar.resources.calendar_empty
import org.sjhstudio.diary.feature.calendar.resources.calendar_month_sheet_title
import org.sjhstudio.diary.feature.calendar.resources.calendar_next_month
import org.sjhstudio.diary.feature.calendar.resources.calendar_previous_month
import org.sjhstudio.diary.feature.calendar.resources.calendar_today
import org.sjhstudio.diary.feature.calendar.resources.calendar_write_on_day

private const val DAYS_IN_WEEK = 7

/** 달력(Figma "달력 (S4)"). 폰은 달력 아래, 넓은 화면은 옆에 그날 일기가 나온다. 주는 일요일에 시작한다. */
@Composable
fun CalendarRoute(twoPane: Boolean, onOpen: (String) -> Unit, onWriteOn: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<CalendarViewModel>()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val last by viewModel.lastMonth.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    val pager = rememberPagerState(initialPage = viewModel.thisMonth().pageIndex()) { last.pageIndex() + 1 }
    val scope = rememberCoroutineScope()
    val dates = rememberDiaryDateFormat()
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { viewModel.showMonth(monthOfPage(it)) } }
    val shown = monthOfPage(pager.currentPage)
    val today = viewModel.today()
    val onToday = {
        viewModel.select(today)
        scope.launch { pager.animateScrollToPage(today.yearMonth.pageIndex()) }
        Unit
    }
    val calendar: @Composable () -> Unit = {
        CalendarPanel(
            shown = shown,
            pager = pager,
            twoPane = twoPane,
            today = today,
            selected = selected,
            lastMonth = last,
            showToday = selected != today,
            dates = dates,
            moods = viewModel::moods,
            onSelect = viewModel::select,
            onToday = onToday,
            onPick = { picking = true },
            onMove = { step -> scope.launch { pager.animateScrollToPage(pager.currentPage + step) } }
        )
    }
    val day: @Composable () -> Unit = {
        selected?.let { date ->
            DayPanel(date, rows, dates, diaryPreviewTextStyle(look.font, look.size) { family }, twoPane, onOpen) { onWriteOn(date) }
        }
    }
    val canvas = Modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding()
    if (twoPane) {
        Row(modifier.then(canvas)) {
            Column(Modifier.width(DiaryCalendarSize.pane).fillMaxHeight().verticalScroll(rememberScrollState())) { calendar() }
            Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(DiaryTheme.colors.line))
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = DiarySpacing.x6)) { day() }
        }
    } else {
        // 그날 마지막 줄이 쓰기 버튼에 가리지 않게 아래를 비운다.
        Column(modifier.then(canvas).verticalScroll(rememberScrollState()).padding(bottom = DiarySpacing.x14 + DiarySpacing.x6)) {
            calendar()
            Spacer(Modifier.height(DiarySpacing.x3))
            day()
        }
    }
    if (picking) {
        DiaryMonthSheet(
            title = stringResource(Res.string.calendar_month_sheet_title),
            initialYear = shown.year,
            shown = shown,
            hasYear = { it <= last.year },
            enabled = { it <= last },
            onPick = { month ->
                picking = false
                scope.launch { pager.scrollToPage(month.pageIndex()) }
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
internal fun CalendarPanel(
    shown: YearMonth,
    pager: PagerState,
    twoPane: Boolean,
    today: LocalDate,
    selected: LocalDate?,
    lastMonth: YearMonth,
    showToday: Boolean,
    dates: DiaryDateFormat,
    moods: (YearMonth) -> Flow<Map<LocalDate, DayMood>>,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
    onMove: (Int) -> Unit
) {
    val nav: @Composable () -> Unit = { MonthNav(shown < lastMonth, showToday, onToday, onMove) }
    Column(Modifier.padding(horizontal = DiarySpacing.screenCompact)) {
        if (twoPane) {
            Row(Modifier.fillMaxWidth().padding(top = DiarySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { MonthTitle(dates.yearMonth(shown.onDay(1)), onPick) }
                nav()
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = DiarySpacing.x1), contentAlignment = Alignment.CenterEnd) { nav() }
            MonthTitle(dates.yearMonth(shown.onDay(1)), onPick)
        }
        Spacer(Modifier.height(DiarySpacing.x4))
        Weekdays(dates)
        Spacer(Modifier.height(DiarySpacing.x1_5))
        HorizontalPager(pager, Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            val month = monthOfPage(page)
            val days by remember(month) { moods(month) }.collectAsState(emptyMap())
            MonthGrid(month, days, today, selected, twoPane, dates, onSelect)
        }
    }
}

@Composable
private fun MonthTitle(text: String, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .semantics { heading() }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(text, style = DiaryTheme.typography.display.copy(color = colors.textPrimary), maxLines = 1)
        DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i20)
    }
}

@Composable
private fun MonthNav(hasNext: Boolean, showToday: Boolean, onToday: () -> Unit, onMove: (Int) -> Unit) {
    val colors = DiaryTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showToday) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .height(DiarySpacing.minTouchTarget)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onToday)
                    .padding(horizontal = DiarySpacing.x2),
                contentAlignment = Alignment.Center
            ) {
                BasicText(stringResource(Res.string.calendar_today), style = DiaryTheme.typography.label.copy(color = colors.accentText))
            }
        }
        NavArrow(DiaryIcons.ChevronLeft, stringResource(Res.string.calendar_previous_month), colors.textPrimary, true) { onMove(-1) }
        NavArrow(
            DiaryIcons.ChevronRight,
            stringResource(Res.string.calendar_next_month),
            if (hasNext) colors.textPrimary else colors.placeholder,
            hasNext
        ) { onMove(1) }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, description: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun Weekdays(dates: DiaryDateFormat) {
    val colors = DiaryTheme.colors
    Row(Modifier.fillMaxWidth()) {
        // 2026년 11월 1일이 일요일이다. 요일 이름만 언어에 맞게 빌려 온다.
        for (offset in 0 until DAYS_IN_WEEK) {
            val date = LocalDate(WEEK_YEAR, WEEK_MONTH, 1 + offset)
            BasicText(
                dates.weekdayShort(date),
                modifier = Modifier.weight(1f),
                style = DiaryTheme.typography.captionStrong.copy(color = dayColor(date, colors.textSecondary), textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun dayColor(date: LocalDate, otherwise: Color): Color = when (date.dayOfWeek) {
    DayOfWeek.SUNDAY -> DiaryTheme.colors.danger
    DayOfWeek.SATURDAY -> DiaryTheme.colors.ink.blue
    else -> otherwise
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    days: Map<LocalDate, DayMood>,
    today: LocalDate,
    selected: LocalDate?,
    wide: Boolean,
    dates: DiaryDateFormat,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.onDay(1)
    // 일요일을 0으로 센 첫날 앞의 빈칸 수.
    val blanks = first.dayOfWeek.ordinal.let { (it + 1) % DAYS_IN_WEEK }
    val cells: List<LocalDate?> = List(blanks) { null } + (1..month.numberOfDays).map { month.onDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        for (week in cells.chunked(DAYS_IN_WEEK)) {
            // 기기 글자를 키워도 날짜가 잘리지 않게 칸은 내용만큼 자란다.
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (index in 0 until DAYS_IN_WEEK) {
                    val date = week.getOrNull(index)
                    Box(
                        Modifier.weight(1f).heightIn(min = if (wide) DiaryCalendarSize.cellWide else DiaryCalendarSize.cell).fillMaxHeight()
                    ) {
                        if (date != null) DayCell(date, days[date], date == today, date == selected, wide, dates) { onSelect(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    day: DayMood?,
    today: Boolean,
    selected: Boolean,
    wide: Boolean,
    dates: DiaryDateFormat,
    onClick: () -> Unit
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val faceSize = if (wide) DiaryCalendarSize.faceWide else DiaryCalendarSize.face
    val description =
        listOfNotNull(
            dates.dayWithWeekday(date),
            day?.mood?.let { moodName(it) },
            day?.let { pluralStringResource(Res.plurals.calendar_day_count, it.count, it.count) }
        ).joinToString(", ")
    Column(
        Modifier
            .fillMaxSize()
            .clip(DiaryShapes.r12)
            .background(if (selected) colors.selected else colors.canvas)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
            }
            .padding(top = DiarySpacing.x1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        Box(
            Modifier
                .defaultMinSize(DiaryCalendarSize.todayWidth, DiaryCalendarSize.todayHeight)
                .clip(DiaryShapes.full)
                .background(if (today) colors.accent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                date.day.toString(),
                style = DiaryTheme.typography.captionStrong.copy(
                    color = if (today) colors.onAccent else dayColor(date, colors.textPrimary)
                ),
                maxLines = 1
            )
        }
        if (day != null) {
            DayFace(
                day,
                faceSize,
                if (wide) DiaryCalendarSize.stackOffsetWide else DiaryCalendarSize.stackOffset,
                if (selected) colors.selected else colors.canvas
            )
        }
    }
}

/** 그날 마지막 일기의 얼굴. 여러 편이면 앞 일기의 기분 색을 뒤에 겹친다(2026-11-05 사용자 결정). 편수는 고른 날 목록에 있다. */
@Composable
private fun DayFace(day: DayMood, size: Dp, stackOffset: Dp, background: Color) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (day.count > 1) {
            val colors = DiaryTheme.colors
            val previous = day.previousMood?.let { moodColor(it) } ?: colors.sunken
            Box(Modifier.offset(x = stackOffset).size(size).clip(DiaryShapes.full).background(previous))
            Box(
                Modifier
                    .requiredSize(size + DiaryCalendarSize.stackRing * 2)
                    .clip(DiaryShapes.full)
                    .background(background)
            )
        }
        val mood = day.mood
        if (mood != null) MoodFace(mood, size) else NoMoodFace(size)
    }
}

/** 고른 날의 일기. 없으면 그날 쓰기를 권한다. */
@Composable
internal fun DayPanel(
    date: LocalDate,
    rows: List<EntryRow>,
    dates: DiaryDateFormat,
    previewStyle: TextStyle,
    twoPane: Boolean,
    onOpen: (String) -> Unit,
    onWrite: () -> Unit
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val day = dates.dayWithWeekday(date)
    val title =
        if (rows.isEmpty()) {
            day
        } else {
            stringResource(Res.string.calendar_day_summary, day, pluralStringResource(Res.plurals.calendar_day_count, rows.size, rows.size))
        }
    Column {
        BasicText(
            title,
            modifier = Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2),
            style = (if (twoPane) typography.headline else typography.captionStrong).copy(
                color = if (twoPane) colors.textPrimary else colors.textSecondary
            )
        )
        if (rows.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = DiarySpacing.x6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
            ) {
                BasicText(stringResource(Res.string.calendar_empty), style = typography.body.copy(color = colors.textSecondary))
                DiaryButton(
                    stringResource(Res.string.calendar_write_on_day),
                    onWrite,
                    kind = DiaryButtonKind.Weak,
                    size = DiaryButtonSize.Medium
                )
            }
        } else {
            rows.forEachIndexed { index, row ->
                val interaction = remember(row.id) { MutableInteractionSource() }
                DiaryEntryLine(
                    row = row,
                    showDate = false,
                    dividerAbove = index > 0,
                    dateColumn = false,
                    previewStyle = previewStyle,
                    modifier =
                        Modifier
                            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpen(row.id) }
                )
            }
        }
    }
}

private const val WEEK_YEAR = 2026
private const val WEEK_MONTH = 11
