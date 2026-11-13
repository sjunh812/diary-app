package org.sjhstudio.diary.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryTabTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBarTextButton
import org.sjhstudio.diary.core.designsystem.component.SnapTitleOnRest
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.component.titleCollapse
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryCalendarSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.core.ui.DiaryTitleButton
import org.sjhstudio.diary.core.ui.EntryMenuEvents
import org.sjhstudio.diary.core.ui.EntryMenuFor
import org.sjhstudio.diary.core.ui.LoadFailedLine
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.NoMoodFace
import org.sjhstudio.diary.core.ui.OnTabReselected
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

/**
 * 달력(Figma "달력 (S4)")
 * 그날 일기는 폰에서 달력 아래, 넓은 화면에서 옆. 주는 일요일 시작
 */
@Composable
fun CalendarRoute(
    twoPane: Boolean,
    onOpen: (String) -> Unit,
    onWriteOn: (LocalDate) -> Unit,
    onEdit: (String) -> Unit,
    onTrash: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<CalendarViewModel>()
    val day by viewModel.day.collectAsStateWithLifecycle()
    val selected = day.date
    val last by viewModel.lastMonth.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    val pager = rememberPagerState(initialPage = viewModel.thisMonth().pageIndex()) { last.pageIndex() + 1 }
    val scope = rememberCoroutineScope()
    val dates = rememberDiaryDateFormat()
    var picking by remember { mutableStateOf(false) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    // 반 넘게 넘어가 달이 정해지는 순간 아래 일기도 그 달로. 멈출 때까지 앞 달 일기가 남지 않게
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { viewModel.showMonth(monthOfPage(it)) } }
    val shown = monthOfPage(pager.currentPage)
    val today by viewModel.todayState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        if (viewModel.refreshToday()) {
            scope.launch {
                val page = viewModel.thisMonth().pageIndex()
                snapshotFlow { pager.pageCount }.first { it > page }
                pager.scrollToPage(page)
            }
        }
        onPauseOrDispose {}
    }
    val onToday = {
        viewModel.select(today)
        scope.launch { pager.animateScrollToPage(today.yearMonth.pageIndex()) }
        Unit
    }
    var titleHeight by remember { mutableIntStateOf(0) }
    val onMove: (Int) -> Unit = { step -> scope.launch { pager.animateScrollToPage(pager.currentPage + step) } }
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
            onMove = onMove,
            onTitleHeight = { titleHeight = it }
        )
    }
    val dayPanel: @Composable () -> Unit = {
        // 날이 바뀌면 짧게 겹쳐 바꿈. 휴대폰 안 일기라 로딩 표시 없이 번쩍이지 않게만
        // 사라지는 쪽은 그날 값을 그대로 그려 새 날 일기가 앞 날짜 아래 보이지 않게
        AnimatedContent(
            day,
            transitionSpec = { fadeIn(tween(DiaryMotion.QUICK_MILLIS)) togetherWith fadeOut(tween(DiaryMotion.EXIT_MILLIS)) },
            contentKey = { it.date },
            label = "day"
        ) { shown ->
            shown.date?.let { date ->
                DayPanel(
                    date = date,
                    rows = shown.rows,
                    failed = shown.failed,
                    onRetry = viewModel::retry,
                    dates = dates,
                    previewStyle = diaryPreviewTextStyle(look.font, look.size) { family },
                    twoPane = twoPane,
                    onOpen = onOpen,
                    onMenu = { menuFor = it },
                    onWrite = { onWriteOn(date) }
                )
            }
        }
    }
    val canvas = Modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding()
    val scroll = rememberScrollState()
    SnapTitleOnRest(scroll, titleHeight)
    OnTabReselected { scroll.animateScrollTo(0) }
    if (twoPane) {
        Row(modifier.then(canvas)) {
            // 창이 넓어지면 왼쪽 칸만 늘고 달력은 그 칸 가운데. 왼쪽으로 치우치지 않게
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = DiaryCalendarSize.pane).fillMaxWidth()) { calendar() }
            }
            Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(DiaryTheme.colors.line))
            Column(
                Modifier
                    .width(DiaryWriteSize.sidePanel)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = DiarySpacing.x6)
            ) {
                dayPanel()
            }
        }
    } else {
        Column(modifier.then(canvas), horizontalAlignment = Alignment.CenterHorizontally) {
            // 큰 달 제목이 바 밑으로 스크롤된 만큼 바에 작은 제목이 나타남(`docs/design.md` 탭 상단 바)
            // 오늘·달 넘기기 단추는 늘 바 오른쪽. 작은 제목은 남는 폭만 써 단추와 겹치지 않음
            DiaryTabTopBar(
                title = dates.yearMonth(shown.onDay(1)),
                collapsed = { titleCollapse(scroll.value, titleHeight) },
                contentMaxWidth = DiaryCalendarSize.pane,
                onTitleClick = { picking = true },
                actions = { MonthNav(shown < last, selected != today, onToday, onMove) }
            )
            // 그날 마지막 줄이 쓰기 버튼에 가리지 않게 아래 여백
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(bottom = DiarySpacing.x14 + DiarySpacing.x6),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = DiaryCalendarSize.pane).fillMaxWidth()) {
                    calendar()
                    Spacer(Modifier.height(DiarySpacing.x3))
                    dayPanel()
                }
            }
        }
    }
    menuFor?.let { id ->
        EntryMenuFor(
            id,
            favorite = day.rows?.firstOrNull { it.id == id }?.isFavorite == true,
            work = viewModel.menu,
            onDismiss = { menuFor = null },
            onEdit = onEdit
        )
    }
    EntryMenuEvents(viewModel.menu, onTrash)
    if (picking) {
        DiaryMonthSheet(
            title = stringResource(Res.string.calendar_month_sheet_title),
            initialYear = shown.year,
            shown = shown,
            years = remember(last.year) { (FIRST_YEAR..last.year).toList() },
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
    moods: (YearMonth) -> StateFlow<Map<LocalDate, DayMood>>,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
    onMove: (Int) -> Unit,
    onTitleHeight: (Int) -> Unit = {}
) {
    val nav: @Composable () -> Unit = { MonthNav(shown < lastMonth, showToday, onToday, onMove) }
    Column(Modifier.padding(horizontal = LocalScreenGutter.current)) {
        if (twoPane) {
            Row(Modifier.fillMaxWidth().padding(top = DiarySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { DiaryTitleButton(dates.yearMonth(shown.onDay(1)), onPick) }
                nav()
            }
        } else {
            // 폰에서는 오늘·달 넘기기 단추가 위 바에 있음
            DiaryTitleButton(dates.yearMonth(shown.onDay(1)), onPick, Modifier.onSizeChanged { onTitleHeight(it.height) })
        }
        Spacer(Modifier.height(DiarySpacing.x4))
        Weekdays(dates)
        Spacer(Modifier.height(DiarySpacing.x1_5))
        HorizontalPager(pager, Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            val month = monthOfPage(page)
            val days by remember(month) { moods(month) }.collectAsStateWithLifecycle()
            MonthGrid(month, days, today, selected, twoPane, dates, onSelect)
        }
    }
}

@Composable
private fun MonthNav(hasNext: Boolean, showToday: Boolean, onToday: () -> Unit, onMove: (Int) -> Unit) {
    val colors = DiaryTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showToday) {
            DiaryTopBarTextButton(stringResource(Res.string.calendar_today), onToday)
        }
        DiaryIconButton(DiaryIcons.ChevronLeft, stringResource(Res.string.calendar_previous_month), { onMove(-1) })
        DiaryIconButton(DiaryIcons.ChevronRight, stringResource(Res.string.calendar_next_month), { onMove(1) }, enabled = hasNext)
    }
}

@Composable
private fun Weekdays(dates: DiaryDateFormat) {
    val colors = DiaryTheme.colors
    Row(Modifier.fillMaxWidth()) {
        // 2026년 11월 1일이 일요일. 요일 이름만 언어에 맞게 빌려 옴
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
    // 일요일을 0으로 센 첫날 앞 빈칸 수
    val blanks = first.dayOfWeek.ordinal.let { (it + 1) % DAYS_IN_WEEK }
    val cells: List<LocalDate?> = List(blanks) { null } + (1..month.numberOfDays).map { month.onDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        for (week in cells.chunked(DAYS_IN_WEEK)) {
            // 기기 글자를 키워도 날짜가 잘리지 않게 칸이 내용만큼 자람
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
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
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
                // 고른 칸은 selected 바탕이라 일요일 색 대비가 부족함(4.2:1)
                style = DiaryTheme.typography.captionStrong.copy(
                    color =
                        when {
                            today -> colors.onAccent
                            selected -> colors.textPrimary
                            else -> dayColor(date, colors.textPrimary)
                        }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

/**
 * 그날 마지막 일기의 얼굴
 * 여러 편이면 앞 일기 기분 색을 뒤에 겹침. 편수는 고른 날 목록에서 보임
 */
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

/**
 * 고른 날의 일기. 없으면 그날 쓰기를 권함
 * [rows]가 null이면 읽는 중이라 날짜만 두고 아래는 비움. 곧 끝나는 일이라 스켈레톤 없음
 */
@Composable
internal fun DayPanel(
    date: LocalDate,
    rows: List<EntryRow>?,
    dates: DiaryDateFormat,
    previewStyle: TextStyle,
    twoPane: Boolean,
    onOpen: (String) -> Unit,
    onMenu: (String) -> Unit,
    onWrite: () -> Unit,
    failed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val day = dates.dayWithWeekday(date)
    val title =
        if (rows.isNullOrEmpty()) {
            day
        } else {
            stringResource(Res.string.calendar_day_summary, day, pluralStringResource(Res.plurals.calendar_day_count, rows.size, rows.size))
        }
    Column {
        BasicText(
            title,
            // 폰은 격자와 떼고 첫 일기 줄에 붙임(Figma S4: 위 16, 아래 4)
            modifier =
                Modifier.padding(
                    start = LocalScreenGutter.current,
                    end = LocalScreenGutter.current,
                    top = if (twoPane) DiarySpacing.x2 else DiarySpacing.x4,
                    bottom = if (twoPane) DiarySpacing.x2 else DiarySpacing.x1
                ),
            style = (if (twoPane) typography.headline else typography.captionStrong).copy(
                color = if (twoPane) colors.textPrimary else colors.textSecondary
            )
        )
        if (rows == null) {
            if (failed) LoadFailedLine(onRetry, Modifier.padding(horizontal = LocalScreenGutter.current))
            return@Column
        }
        if (rows.isEmpty()) {
            // 달력 아래 한 칸이라 화면 전체용 빈 상태 대신 쓴 글이 없다는 것만 조용히 알림
            DiaryEmptyLine(
                stringResource(Res.string.calendar_empty),
                stringResource(Res.string.calendar_write_on_day),
                onWrite,
                Modifier.padding(horizontal = LocalScreenGutter.current)
            )
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
                            .combinedClickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button,
                                onLongClick = { onMenu(row.id) },
                                onClick = { onOpen(row.id) }
                            )
                )
            }
        }
    }
}

private const val WEEK_YEAR = 2026
private const val WEEK_MONTH = 11
