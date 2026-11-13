package org.sjhstudio.diary.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.EntryMenuWork
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 고른 날과 그날 일기
 * [rows]: null이면 읽는 중, 빈 목록이면 쓴 글이 없는 날. [failed]면 다시 시도를 보임
 * 일기가 들어 있어 [toString]에서 뺌(헌법 II)
 */
data class CalendarDay(val date: LocalDate?, val rows: List<EntryRow>?, val failed: Boolean = false) {
    override fun toString(): String = "CalendarDay"
}

/**
 * 달력(FR-022~FR-024)
 * 고른 날은 하나. 달을 넘기면 이번 달은 오늘, 다른 달은 1일을 고름
 */
class CalendarViewModel(
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller,
    private val today: () -> LocalDate = { currentDate() },
    private val saved: SavedStateHandle
) : ViewModel() {
    // 시스템이 앱을 끄고 다시 띄워도 고른 날 유지
    private val _selected =
        MutableStateFlow<LocalDate?>(saved.get<String>(SELECTED_KEY)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today())

    /** 일기 줄 길게 누르기 메뉴(홈과 같음) */
    val menu = EntryMenuWork(viewModelScope, entries)

    init {
        viewModelScope.launchSafely { _selected.collect { date -> date?.let { saved[SELECTED_KEY] = it.toString() } } }
    }

    val selected: StateFlow<LocalDate?> = _selected.asStateFlow()

    fun retry() {
        attempt.value++
    }

    private val _todayState = MutableStateFlow(today())

    /** 화면이 보는 오늘. 앱이 돌아올 때 [refreshToday]가 바꿈 */
    val todayState: StateFlow<LocalDate> = _todayState.asStateFlow()

    /** 다시 시도마다 올라 고른 날을 새로 읽음 */
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val readDay: Flow<CalendarDay> =
        combine(_selected, attempt) { date, _ -> date }.flatMapLatest { date ->
            if (date == null) {
                flowOf(CalendarDay(null, emptyList()))
            } else {
                entries.day(date)
                    .map { rows ->
                        dayRows[date] = rows
                        CalendarDay(date, rows)
                    }
                    // 읽지 못하면 읽는 중으로 남기지 않고 다시 시도를 보임
                    .catch { if (it is Exception) emit(CalendarDay(date, null, failed = true)) else throw it }
            }
        }

    /**
     * 날짜와 그날 일기를 한 값으로 묶어 지난 날 일기가 새 날 아래 보이지 않게 함
     * 날을 바꾸면 미리 읽은 일기를 바로 보이고 다시 읽은 값으로 바꿈. 미리 읽은 게 없을 때만 잠깐 읽는 중
     */
    val day: StateFlow<CalendarDay> =
        combine(_selected, readDay) { date, read -> read.takeIf { it.date == date } ?: CalendarDay(date, date?.let(::knownRows)) }
            .stateInSafely(viewModelScope, WhileUiSubscribed, CalendarDay(_selected.value, null))

    private val monthMoods = HashMap<YearMonth, StateFlow<Map<LocalDate, DayMood>>>()

    /**
     * 날마다 읽어 둔 일기
     * 없으면 날을 누를 때마다 목록이 비었다 채워지며 깜빡임
     */
    private val dayRows = HashMap<LocalDate, List<EntryRow>>()

    /** 다 읽은 달. 이 달에서 기분 지도에 없는 날은 일기 없음 */
    private val readMonths = HashSet<YearMonth>()

    /** 미리 아는 그날 일기. 모르면 null */
    private fun knownRows(date: LocalDate): List<EntryRow>? =
        dayRows[date] ?: if (date.yearMonth in readMonths && date !in monthMoods[date.yearMonth]?.value.orEmpty()) emptyList() else null

    /** 일기가 있는 날을 미리 읽어 둠. 한 달 일기는 많지 않음 */
    private fun prefetch(days: Collection<LocalDate>) {
        viewModelScope.launchSafely {
            for (date in days) if (date !in dayRows) dayRows[date] = entries.day(date).first()
        }
    }

    /** 넘길 수 있는 마지막 달. 이번 달과 일기가 있는 마지막 달 중 늦은 쪽(옛 앱은 미래 날짜도 허용) */
    val lastMonth: StateFlow<YearMonth> =
        combine(entries.months(), _todayState) { months, date -> maxOf(months.maxOrNull() ?: date.yearMonth, date.yearMonth) }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, thisMonth())

    val look: StateFlow<BodyLook> =
        settings.bodyLookIn(viewModelScope, fonts::installedPath)

    fun today(): LocalDate = today.invoke()

    fun thisMonth(): YearMonth = today().yearMonth

    /** 달의 기분. 읽은 달은 값을 들고 있어 달·탭을 옮겼다 와도 얼굴이 사라졌다 나타나지 않음 */
    fun moods(month: YearMonth): StateFlow<Map<LocalDate, DayMood>> = monthMoods.getOrPut(month) {
        entries
            .month(month)
            .onEach { map ->
                readMonths += month
                // 이 달 일기가 바뀌어 미리 읽은 것을 버리고 다시 읽음
                dayRows.keys.removeAll { it.yearMonth == month }
                prefetch(map.keys)
            }
            .stateInSafely(viewModelScope, WhileUiSubscribed, emptyMap())
    }

    fun select(date: LocalDate) {
        _selected.value = date
    }

    /**
     * 달을 넘겼을 때. 그 달 날을 이미 골랐으면 그대로
     * 아니면 이번 달은 오늘, 다른 달은 1일(Apple 캘린더와 같음)
     */
    fun showMonth(month: YearMonth) {
        if (_selected.value?.yearMonth == month) return
        _selected.value = if (month == thisMonth()) today() else month.onDay(1)
    }

    /**
     * 앱이 돌아올 때 오늘을 다시 읽음
     * 어제의 오늘을 보던 중이면 새 오늘로 옮기고, 고른 다른 날은 그대로
     * @return 달까지 바뀌어 달력을 넘겨야 하면 true
     */
    fun refreshToday(): Boolean {
        val before = _todayState.value
        val now = today()
        _todayState.value = now
        val followed = now != before && _selected.value == before
        if (followed) _selected.value = now
        return followed && now.yearMonth != before.yearMonth
    }
}

/** 달력 넘기기의 쪽 번호. 1970년 1월이 0쪽 */
internal fun YearMonth.pageIndex(): Int = (year - FIRST_YEAR) * MONTHS + (month.number - 1)

internal fun monthOfPage(page: Int): YearMonth = YearMonth(FIRST_YEAR + page / MONTHS, page % MONTHS + 1)

internal const val FIRST_YEAR = 1970
private const val MONTHS = 12

private const val SELECTED_KEY = "calendar_selected"
