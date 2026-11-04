package org.sjhstudio.diary.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook

/**
 * 달력(FR-022~FR-024). 달을 넘기면 그 달에서 마지막으로 일기를 쓴 날을 고른다.
 * 이번 달로 오면 오늘을 고른다. 일기가 없는 달은 아무 날도 고르지 않는다.
 */
class CalendarViewModel(
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller,
    private val today: () -> LocalDate = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
) : ViewModel() {
    private val _selected = MutableStateFlow<LocalDate?>(today())
    val selected: StateFlow<LocalDate?> = _selected.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: StateFlow<List<EntryRow>> =
        _selected
            .flatMapLatest { date -> date?.let(entries::day) ?: flowOf(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    /** 넘길 수 있는 마지막 달. 이번 달과 일기가 있는 마지막 달 가운데 늦은 쪽이다(옛 앱은 미래 날짜도 허용했다). */
    val lastMonth: StateFlow<YearMonth> =
        entries.months()
            .map { months -> maxOf(months.maxOrNull() ?: thisMonth(), thisMonth()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, thisMonth())

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { BodyLook(it.font, it.bodyTextSize, fonts.installedPath(it.font.id)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    fun today(): LocalDate = today.invoke()

    fun thisMonth(): YearMonth = today().yearMonth

    fun moods(month: YearMonth): Flow<Map<LocalDate, DayMood>> = entries.month(month)

    fun select(date: LocalDate) {
        _selected.value = date
    }

    /** 달을 넘겼을 때. 이미 그 달의 날을 고른 상태면 그대로 둔다. */
    fun showMonth(month: YearMonth) {
        if (_selected.value?.yearMonth == month) return
        if (month == thisMonth()) {
            _selected.value = today()
            return
        }
        viewModelScope.launch { _selected.value = entries.month(month).first().keys.maxOrNull() }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}

/** 달력 넘기기의 쪽 번호. 1970년 1월이 0쪽이다. */
internal fun YearMonth.pageIndex(): Int = (year - FIRST_YEAR) * MONTHS + (month.number - 1)

internal fun monthOfPage(page: Int): YearMonth = YearMonth(FIRST_YEAR + page / MONTHS, page % MONTHS + 1)

private const val FIRST_YEAR = 1970
private const val MONTHS = 12
