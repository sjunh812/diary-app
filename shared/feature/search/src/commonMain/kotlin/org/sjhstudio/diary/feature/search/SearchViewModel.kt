package org.sjhstudio.diary.feature.search

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

/**
 * 기간 칩의 고르는 값.
 * [Custom]은 직접 고른 범위이고 돌아보기에서 넘어온 기간도 여기다.
 */
enum class SearchPeriod { Week, Month, Year, Custom }

/**
 * 찾기(FR-025~FR-027).
 * 상세에 다녀와도 이 모델이 살아 있어 입력과 조건이 그대로다.
 * 글자는 칠 때마다 찾지 않고 잠깐 멈춘 뒤에 찾는다.
 * 결과는 홈처럼 쪽으로 읽어서 1만 편이 걸려도 보는 만큼만 메모리에 둔다.
 */
class SearchViewModel(
    private val entries: EntryRepository,
    initial: SearchFilter,
    settings: SettingsRepository,
    fonts: FontInstaller,
    private val today: () -> LocalDate = { currentDate() }
) : ViewModel() {
    private val _filter = MutableStateFlow(initial)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _period = MutableStateFlow(if (initial.period != null) SearchPeriod.Custom else null)
    val period: StateFlow<SearchPeriod?> = _period.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: Flow<PagingData<EntryRow>> =
        _filter
            .debounce(DEBOUNCE_MILLIS)
            .flatMapLatest { filter -> if (filter.isEmpty) flowOf(PagingData.empty(WAITING)) else entries.search(filter) }
            .cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { it.bodyLook(fonts::installedPath) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    fun setMoods(moods: Set<Mood>) = _filter.update { it.copy(moods = moods) }

    fun setWeathers(weathers: Set<Weather>) = _filter.update { it.copy(weathers = weathers) }

    fun togglePhoto() = _filter.update { it.copy(hasPhoto = !it.hasPhoto) }

    fun toggleFavorites() = _filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    /**
     * 이번 주·이번 달·올해.
     * 범위는 고른 날의 오늘을 기준으로 정한다.
     */
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
        _period.value = period
        _filter.update { it.copy(period = range) }
    }

    fun setCustomPeriod(range: DateRange) {
        _period.value = SearchPeriod.Custom
        _filter.update { it.copy(period = range) }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 200L

        // 아직 찾지 않았을 때.
        // "찾지 못했어요"가 비치지 않게 읽는 중으로 둔다.
        val WAITING =
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true)
            )
    }
}
