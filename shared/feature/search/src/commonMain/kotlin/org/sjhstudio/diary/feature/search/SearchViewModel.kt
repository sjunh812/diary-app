package org.sjhstudio.diary.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.bodyLook
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/** 기간 칩의 고르는 값. [Custom]은 직접 고른 범위, 돌아보기에서 넘어온 기간도 여기 */
enum class SearchPeriod { Week, Month, Year, Custom }

/**
 * 찾기(FR-025~FR-027). 상세에 다녀와도 이 모델이 살아 있어 입력과 조건 유지
 * 글자는 칠 때마다 찾지 않고 잠깐 멈춘 뒤 찾음
 * 결과는 홈처럼 쪽으로 읽어 1만 편이 걸려도 보는 만큼만 메모리에 둠
 */
class SearchViewModel(
    private val entries: EntryRepository,
    initial: SearchFilter,
    settings: SettingsRepository,
    fonts: FontInstaller,
    private val today: () -> LocalDate = { currentDate() },
    private val saved: SavedStateHandle
) : ViewModel() {
    // 시스템이 앱을 끄고 다시 띄워도 치던 글과 고른 조건 유지
    private val _filter = MutableStateFlow(saved.get<String>(FILTER_KEY)?.let(::decodeFilter) ?: initial)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    // 기간 종류도 함께 남김. 범위만 남기면 다시 떴을 때 결과는 걸러져 있는데 칩은 고르지 않은 모양이 됨
    private val _period =
        MutableStateFlow(
            saved.get<String>(PERIOD_KEY)?.let { name -> SearchPeriod.entries.firstOrNull { it.name == name } }
                ?: SearchPeriod.Custom.takeIf { _filter.value.period != null }
        )
    val period: StateFlow<SearchPeriod?> = _period.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: Flow<PagingData<EntryRow>> =
        _filter
            .debounce(DEBOUNCE_MILLIS)
            .flatMapLatest { filter -> if (filter.isEmpty) flowOf(PagingData.empty(WAITING)) else entries.search(filter) }
            .cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.bodyLookIn(viewModelScope, fonts::installedPath)

    init {
        viewModelScope.launchSafely { _filter.collect { saved[FILTER_KEY] = encodeFilter(it) } }
    }

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    fun setMoods(moods: Set<Mood>) = _filter.update { it.copy(moods = moods) }

    fun setWeathers(weathers: Set<Weather>) = _filter.update { it.copy(weathers = weathers) }

    fun togglePhoto() = _filter.update { it.copy(hasPhoto = !it.hasPhoto) }

    fun toggleFavorites() = _filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    /** 고른 조건만 모두 지움. 쓴 낱말은 그대로 */
    fun clearConditions() {
        choose(null)
        _filter.update { SearchFilter(query = it.query) }
    }

    /** 이번 주·이번 달·올해. 범위는 고른 날의 오늘 기준 */
    fun setPeriod(period: SearchPeriod?) {
        val date = today()
        val range =
            when (period) {
                SearchPeriod.Week -> StatsPeriod.Week.of(date).range
                SearchPeriod.Month -> StatsPeriod.Month(date.yearMonth).range
                SearchPeriod.Year -> StatsPeriod.Year(date.year).range
                SearchPeriod.Custom -> _filter.value.period
                null -> null
            }
        choose(period)
        _filter.update { it.copy(period = range) }
    }

    fun setCustomPeriod(range: DateRange) {
        choose(SearchPeriod.Custom)
        _filter.update { it.copy(period = range) }
    }

    private fun choose(period: SearchPeriod?) {
        _period.value = period
        saved[PERIOD_KEY] = period?.name
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 200L

        // 아직 찾지 않았을 때. "찾지 못했어요"가 비치지 않게 읽는 중으로
        val WAITING =
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true)
            )
    }
}

private const val FILTER_KEY = "search_filter"
private const val PERIOD_KEY = "search_period"
private const val SEP = "\u001F"

/** 저장 상태에 넣게 글자 하나로 접음. 기분과 날씨는 이름, 기간은 날짜 */
internal fun encodeFilter(filter: SearchFilter): String = listOf(
    filter.query,
    filter.moods.joinToString(",") { it.name },
    filter.weathers.joinToString(",") { it.name },
    filter.hasPhoto.toString(),
    filter.favoritesOnly.toString(),
    filter.period?.start?.toString().orEmpty(),
    filter.period?.end?.toString().orEmpty()
).joinToString(SEP)

/** 앱이 바뀌어 읽을 수 없는 값이면 처음 조건으로 */
internal fun decodeFilter(text: String): SearchFilter? = runCatching {
    // 접은 순서 그대로 하나씩 꺼냄
    val part = text.split(SEP).iterator()
    fun <T : Enum<T>> names(value: String, all: Array<T>) =
        value.split(",").filter { it.isNotEmpty() }.mapNotNullTo(HashSet()) { name -> all.firstOrNull { it.name == name } }
    SearchFilter(
        query = part.next(),
        moods = names(part.next(), Mood.entries.toTypedArray()),
        weathers = names(part.next(), Weather.entries.toTypedArray()),
        hasPhoto = part.next().toBoolean(),
        favoritesOnly = part.next().toBoolean(),
        period = period(part.next(), part.next())
    )
}.getOrNull()

private fun period(start: String, end: String): DateRange? =
    if (start.isEmpty()) null else DateRange(LocalDate.parse(start), LocalDate.parse(end))
