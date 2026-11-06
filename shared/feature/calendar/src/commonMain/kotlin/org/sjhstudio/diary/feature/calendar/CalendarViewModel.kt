package org.sjhstudio.diary.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.bodyLook

/**
 * 고른 날과 그날 일기.
 * [rows]가 null이면 아직 읽는 중이고, 빈 목록이면 쓴 글이 없는 날이다.
 * 일기가 들어 있어 [toString]에 담지 않는다(헌법 II).
 */
data class CalendarDay(val date: LocalDate?, val rows: List<EntryRow>?) {
    override fun toString(): String = "CalendarDay"
}

/**
 * 달력(FR-022~FR-024).
 * 달을 넘기면 그 달에서 마지막으로 일기를 쓴 날을 고른다.
 * 이번 달로 오면 오늘을 고른다.
 * 일기가 없는 달은 아무 날도 고르지 않는다.
 */
class CalendarViewModel(
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller,
    private val today: () -> LocalDate = { currentDate() }
) : ViewModel() {
    private val _selected = MutableStateFlow<LocalDate?>(today())
    val selected: StateFlow<LocalDate?> = _selected.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val readDay: Flow<CalendarDay> =
        _selected.flatMapLatest { date ->
            if (date == null) flowOf(CalendarDay(null, emptyList())) else entries.day(date).map { CalendarDay(date, it) }
        }

    /**
     * 날짜와 그 날짜의 일기를 한 값으로 묶는다.
     * 날을 바꾸면 새 일기가 올 때까지 읽는 중이고, 지난 날 일기는 보이지 않는다.
     * 다른 탭에 다녀와 다시 읽을 때는 같은 날이라 보던 일기를 그대로 둔다.
     */
    val day: StateFlow<CalendarDay> =
        combine(_selected, readDay) { date, read -> read.takeIf { it.date == date } ?: CalendarDay(date, null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), CalendarDay(_selected.value, null))

    private val monthMoods = HashMap<YearMonth, StateFlow<Map<LocalDate, DayMood>>>()

    /**
     * 넘길 수 있는 마지막 달.
     * 이번 달과 일기가 있는 마지막 달 가운데 늦은 쪽이다(옛 앱은 미래 날짜도 허용했다).
     */
    val lastMonth: StateFlow<YearMonth> =
        entries.months()
            .map { months -> maxOf(months.maxOrNull() ?: thisMonth(), thisMonth()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, thisMonth())

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { it.bodyLook(fonts::installedPath) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    fun today(): LocalDate = today.invoke()

    fun thisMonth(): YearMonth = today().yearMonth

    /**
     * 달의 기분.
     * 읽은 달은 값을 들고 있어, 달을 넘겨 다녀오거나 탭을 옮겼다 와도 얼굴이 사라졌다 나타나지 않는다.
     */
    fun moods(month: YearMonth): StateFlow<Map<LocalDate, DayMood>> = monthMoods.getOrPut(month) {
        entries.month(month).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyMap())
    }

    fun select(date: LocalDate) {
        _selected.value = date
    }

    /**
     * 달을 넘겼을 때.
     * 이미 그 달의 날을 고른 상태면 그대로 둔다.
     */
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

/**
 * 달력 넘기기의 쪽 번호.
 * 1970년 1월이 0쪽이다.
 */
internal fun YearMonth.pageIndex(): Int = (year - FIRST_YEAR) * MONTHS + (month.number - 1)

internal fun monthOfPage(page: Int): YearMonth = YearMonth(FIRST_YEAR + page / MONTHS, page % MONTHS + 1)

private const val FIRST_YEAR = 1970
private const val MONTHS = 12
