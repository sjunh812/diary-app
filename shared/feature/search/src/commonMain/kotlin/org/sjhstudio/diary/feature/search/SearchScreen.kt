package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySearchField
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.diaryPreviewTextStyle
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
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
import org.sjhstudio.diary.feature.search.resources.search_hint
import org.sjhstudio.diary.feature.search.resources.search_result_count

/** 어느 칩의 시트가 열렸는지. */
private enum class Sheet { Mood, Weather, Period, Range }

/**
 * 찾기(Figma "찾기 (S1-a)"). 넓은 화면에서는 왼쪽 칸이 되고 고른 줄을 [selectedId]로 표시한다.
 * 조건 없이 들어오면 입력 칸에 바로 포커스를 둔다.
 */
@Composable
fun SearchRoute(initial: SearchFilter, selectedId: String?, onBack: () -> Unit, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<SearchViewModel> { parametersOf(initial) }
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val family = rememberFileFontFamily(look.fontPath)
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
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
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
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
    results: List<EntryRow>?,
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
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = DiarySpacing.x1, end = DiarySpacing.x4, top = DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(DiarySpacing.minTouchTarget)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.full)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.ChevronLeft, stringResource(Res.string.search_back), colors.textPrimary)
            }
            DiarySearchField(
                value = filter.query,
                onValueChange = onQuery,
                hint = stringResource(Res.string.search_hint),
                clearLabel = stringResource(Res.string.search_clear),
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
        }
        FilterChips(filter, period, onSheet, onPhoto, onFavorites)
        val rows = results
        when {
            rows == null -> Unit
            rows.isEmpty() -> Empty()
            else -> Results(rows, filter.query, selectedId, previewStyle, list, onOpen)
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
        contentPadding = PaddingValues(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2),
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
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2, Alignment.CenterVertically)
    ) {
        BasicText(
            stringResource(Res.string.search_empty_title),
            style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        BasicText(
            stringResource(Res.string.search_empty_body),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
        )
    }
}

/** 달이 바뀌면 달 머리를 넣는다. 결과는 최신순이다. */
@Composable
private fun Results(
    rows: List<EntryRow>,
    query: String,
    selectedId: String?,
    previewStyle: TextStyle,
    list: LazyListState,
    onOpen: (String) -> Unit
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val today: LocalDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    LazyColumn(Modifier.fillMaxSize(), state = list) {
        item {
            BasicText(
                pluralStringResource(Res.plurals.search_result_count, rows.size, rows.size),
                modifier = Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x1),
                style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary)
            )
        }
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
            val previous = rows.getOrNull(index - 1)
            val newMonth = previous == null || previous.date.year != row.date.year || previous.date.month != row.date.month
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
