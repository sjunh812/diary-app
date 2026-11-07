package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiarySearchField
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.SkeletonEntryRows
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberToday
import org.sjhstudio.diary.feature.search.resources.Res
import org.sjhstudio.diary.feature.search.resources.search_back
import org.sjhstudio.diary.feature.search.resources.search_chip_favorite
import org.sjhstudio.diary.feature.search.resources.search_chip_mood
import org.sjhstudio.diary.feature.search.resources.search_chip_period
import org.sjhstudio.diary.feature.search.resources.search_chip_photo
import org.sjhstudio.diary.feature.search.resources.search_chip_weather
import org.sjhstudio.diary.feature.search.resources.search_clear
import org.sjhstudio.diary.feature.search.resources.search_empty_body
import org.sjhstudio.diary.feature.search.resources.search_empty_title
import org.sjhstudio.diary.feature.search.resources.search_error_body
import org.sjhstudio.diary.feature.search.resources.search_error_retry
import org.sjhstudio.diary.feature.search.resources.search_error_title
import org.sjhstudio.diary.feature.search.resources.search_hint
import org.sjhstudio.diary.feature.search.resources.search_idle
import org.sjhstudio.diary.feature.search.resources.search_result_count

/** 어느 칩의 시트가 열렸는지. */
private enum class Sheet { Mood, Weather, Period, Range }

/** 조건을 바꾼 뒤 결과가 자리 잡을 때까지 맨 위를 지키는 시간. 그 뒤로는 사용자의 스크롤을 따른다. */
private const val RESULTS_SETTLE_MILLIS = 1_000L

/**
 * 찾기(Figma "찾기 (S1-a)").
 * 넓은 화면에서는 왼쪽 칸이 되고 고른 줄을 [selectedId]로 표시한다.
 * 조건 없이 들어오면 입력 칸에 바로 포커스를 둔다.
 */
@Composable
fun SearchRoute(initial: SearchFilter, selectedId: String?, onBack: () -> Unit, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<SearchViewModel> { parametersOf(initial) }
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    // 상세에 다녀와도 이 화면은 다시 그려진다.
    // 조건이 그대로면 보던 자리에 두려고 마지막 조건을 저장해 둔다.
    var listedFilter by rememberSaveable { mutableIntStateOf(filter.hashCode()) }
    LaunchedEffect(filter) {
        if (filter.hashCode() == listedFilter) return@LaunchedEffect
        listedFilter = filter.hashCode()
        // 찾기는 잠깐 멈춘 뒤에 돌아서 결과가 늦게 온다.
        // 목록은 보던 줄을 따라가려 해서, 새 결과가 와도 맨 위로 되돌린다.
        withTimeoutOrNull(RESULTS_SETTLE_MILLIS) {
            snapshotFlow { results.itemCount to results.itemCount.takeIf { it > 0 }?.let { results.peek(0)?.id } }
                .collect { list.scrollToItem(0) }
        }
    }
    val focus = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focused && initial.isEmpty) focus.requestFocus()
        focused = true
    }
    SearchScreen(
        filter = filter,
        period = period,
        results = results,
        selectedId = selectedId,
        previewStyle = diaryPreviewTextStyle(look.font, look.size) { family },
        list = list,
        focus = focus,
        onQuery = viewModel::setQuery,
        onBack = onBack,
        onOpen = onOpen,
        onSheet = { sheet = Sheet.valueOf(it.name).name },
        onPhoto = viewModel::togglePhoto,
        onFavorites = viewModel::toggleFavorites,
        modifier = modifier
    )
    val today = rememberToday()
    when (sheet) {
        Sheet.Mood.name -> MoodSheet(filter.moods, viewModel::setMoods) { sheet = null }

        Sheet.Weather.name -> WeatherSheet(filter.weathers, viewModel::setWeathers) { sheet = null }

        Sheet.Period.name ->
            PeriodSheet(
                selected = period,
                onPick = {
                    viewModel.setPeriod(it)
                    sheet = null
                },
                onCustom = { sheet = Sheet.Range.name },
                onDismiss = { sheet = null }
            )

        Sheet.Range.name ->
            RangeSheet(
                initial = filter.period,
                today = today,
                onDone = {
                    viewModel.setCustomPeriod(it)
                    sheet = null
                },
                onDismiss = { sheet = null }
            )
    }
}

@Composable
internal fun SearchScreen(
    filter: SearchFilter,
    period: SearchPeriod?,
    results: LazyPagingItems<EntryRow>,
    selectedId: String?,
    previewStyle: TextStyle,
    list: LazyListState,
    focus: FocusRequester,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onSheet: (SheetRequest) -> Unit,
    onPhoto: () -> Unit,
    onFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Box(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = DiarySpacing.x1, end = LocalScreenGutter.current, top = DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiaryIconButton(DiaryIcons.ChevronLeft, stringResource(Res.string.search_back), onBack)
                DiarySearchField(
                    value = filter.query,
                    onValueChange = onQuery,
                    hint = stringResource(Res.string.search_hint),
                    clearLabel = stringResource(Res.string.search_clear),
                    modifier = Modifier.weight(1f).focusRequester(focus)
                )
            }
            FilterChips(filter, period, onSheet, onPhoto, onFavorites)
            // 없는 글자를 이어 칠 때 뼈대가 깜빡이지 않게, 없었으면 다 읽을 때까지 없음을 둔다.
            val refresh = results.loadState.refresh
            var wasEmpty by remember { mutableStateOf(false) }
            LaunchedEffect(refresh, filter.isEmpty) {
                if (filter.isEmpty) {
                    wasEmpty = false
                } else if (refresh is LoadState.NotLoading) {
                    wasEmpty = results.itemCount == 0
                }
            }
            when {
                filter.isEmpty ->
                    DiaryNotice(
                        stringResource(Res.string.search_idle),
                        Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2),
                        icon = DiaryIcons.Search
                    )

                results.itemCount > 0 -> Results(results, filter.query, selectedId, previewStyle, list, onOpen)

                refresh is LoadState.Error -> LoadFailed(results::retry)

                refresh is LoadState.NotLoading || wasEmpty -> Empty()

                else -> SkeletonEntryRows(Modifier.padding(top = DiarySpacing.x4))
            }
        }
    }
}

/** [SearchScreen]이 여는 시트. */
internal enum class SheetRequest { Mood, Weather, Period }

@Composable
private fun FilterChips(
    filter: SearchFilter,
    period: SearchPeriod?,
    onSheet: (SheetRequest) -> Unit,
    onPhoto: () -> Unit,
    onFavorites: () -> Unit
) {
    LazyRow(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        item {
            DiaryChip(
                stringResource(Res.string.search_chip_mood),
                filter.moods.isNotEmpty(),
                { onSheet(SheetRequest.Mood) },
                leadingIcon = DiaryIcons.Smile,
                trailingIcon = DiaryIcons.ChevronDown
            )
        }
        item {
            DiaryChip(
                stringResource(Res.string.search_chip_weather),
                filter.weathers.isNotEmpty(),
                { onSheet(SheetRequest.Weather) },
                leadingIcon = DiaryIcons.WeatherSunny,
                trailingIcon = DiaryIcons.ChevronDown
            )
        }
        item {
            DiaryChip(
                period?.let { periodLabel(it) } ?: stringResource(Res.string.search_chip_period),
                period != null,
                { onSheet(SheetRequest.Period) },
                leadingIcon = DiaryIcons.TabCalendar,
                trailingIcon = DiaryIcons.ChevronDown
            )
        }
        item { DiaryChip(stringResource(Res.string.search_chip_photo), filter.hasPhoto, onPhoto, leadingIcon = DiaryIcons.Image) }
        item {
            DiaryChip(
                stringResource(Res.string.search_chip_favorite),
                filter.favoritesOnly,
                onFavorites,
                leadingIcon = DiaryIcons.StarOutline
            )
        }
    }
}

@Composable
private fun Empty() {
    DiaryEmptyArea {
        DiaryEmptyState(
            DiaryIcons.Search,
            stringResource(Res.string.search_empty_title),
            description = stringResource(Res.string.search_empty_body)
        )
    }
}

/**
 * 결과를 읽지 못했을 때.
 * 뼈대를 계속 두지 않고 까닭과 다시 시도를 보인다.
 */
@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    DiaryEmptyArea {
        DiaryEmptyState(
            DiaryIcons.CircleAlert,
            stringResource(Res.string.search_error_title),
            description = stringResource(Res.string.search_error_body),
            actionLabel = stringResource(Res.string.search_error_retry),
            onAction = onRetry
        )
    }
}

/**
 * 달이 바뀌면 달 머리를 넣는다.
 * 결과는 최신순이다.
 */
@Composable
private fun Results(
    rows: LazyPagingItems<EntryRow>,
    query: String,
    selectedId: String?,
    previewStyle: TextStyle,
    list: LazyListState,
    onOpen: (String) -> Unit
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val today = rememberToday()
    // 마지막 줄이 시스템 내비게이션에 가리지 않게 아래를 비운다.
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DiarySpacing.x6
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = bottom)) {
        item {
            BasicText(
                pluralStringResource(Res.plurals.search_result_count, rows.itemCount, rows.itemCount),
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x1),
                style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary)
            )
        }
        items(count = rows.itemCount, key = rows.itemKey { it.id }, contentType = { "entry" }) { index ->
            // 아직 읽지 않은 줄은 자리만 잡는다.
            // 앞 줄은 읽기를 부르지 않게 엿보기만 한다.
            val row = rows[index]
            if (row == null) {
                Spacer(Modifier.fillMaxWidth().height(DiarySpacing.x14 + DiarySpacing.x14))
                return@items
            }
            val previous = if (index > 0) rows.peek(index - 1) else null
            val newMonth =
                index == 0 || (previous != null && (previous.date.year != row.date.year || previous.date.month != row.date.month))
            Column {
                if (newMonth) DiaryMonthHeader(if (row.date.year == today.year) dates.month(row.date) else dates.yearMonth(row.date))
                val interaction = remember { MutableInteractionSource() }
                DiaryEntryLine(
                    row = row,
                    showDate = previous?.date != row.date,
                    dividerAbove = !newMonth,
                    previewStyle = previewStyle,
                    highlight = query.trim(),
                    modifier =
                        Modifier
                            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                            .background(if (row.id == selectedId) colors.selected else colors.canvas)
                            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpen(row.id) }
                )
            }
        }
    }
}
