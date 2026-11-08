package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.mostFrequent

/** 기간 탭. */
enum class InsightKind { Week, Month, Year, All }

/** 한 기간을 읽은 값. */
data class InsightData(val counts: MoodCounts, val days: List<DayMood>, val pixels: Map<LocalDate, Mood?>)

/**
 * 기간과 그 기간의 숫자.
 * [data]가 null이면 아직 읽는 중이다.
 * 기분 기록이 들어 있어 [toString]에 담지 않는다(헌법 II).
 */
data class InsightState(val period: StatsPeriod, val data: InsightData?) {
    override fun toString(): String = "InsightState"
}

/**
 * 돌아보기(FR-028~FR-031).
 * 기간을 칸으로 나눈 기분 흐름과, 년은 하루 한 칸 지도를 함께 읽는다.
 */
class InsightViewModel(
    private val stats: StatsRepository,
    private val today: () -> LocalDate = { currentDate() },
    plans: PlanRepository? = null,
    private val saved: SavedStateHandle
) : ViewModel() {
    /**
     * 고급 통계를 볼 수 있는지.
     * 요금제를 바꾸면 바로 따른다.
     */
    val advancedOpen: StateFlow<Boolean> =
        (plans?.plan ?: flowOf(Plan.FREE))
            .map { it.advancedStats }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 시스템이 앱을 끄고 다시 띄워도 보던 기간이 남는다.
    private val _period = MutableStateFlow(saved.get<String>(PERIOD_KEY)?.let(::decodePeriod) ?: StatsPeriod.Month(today().yearMonth))
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    init {
        viewModelScope.launch { _period.collect { saved[PERIOD_KEY] = encodePeriod(it) } }
    }

    private val _todayState = MutableStateFlow(today())

    /** 화면이 보는 오늘. 앱이 돌아올 때 [refreshToday]가 바꾼다. */
    val todayState: StateFlow<LocalDate> = _todayState.asStateFlow()

    /**
     * 읽어 둔 기간.
     * 탭을 누를 때마다 비웠다 그리면 화면이 번쩍여서, 있는 값을 바로 보이고 새 값으로 바꾼다.
     */
    private val cache = mutableMapOf<StatsPeriod, InsightData>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val readPeriod: Flow<InsightState> =
        _period.flatMapLatest { period ->
            read(period).map { data ->
                cache[period] = data
                InsightState(period, data)
            }
        }

    private fun read(period: StatsPeriod): Flow<InsightData> {
        val pixels = if (period is StatsPeriod.Year) stats.yearPixels(period.year) else flowOf(emptyMap())
        return combine(stats.moodCounts(period), stats.moodFlow(period), pixels) { counts, days, map -> InsightData(counts, days, map) }
    }

    /**
     * 기간과 그 기간의 숫자를 한 값으로 묶는다.
     * 기간을 바꾸면 읽어 둔 값을 바로 내놓고, 없으면 읽는 중이다. 앞 기간 숫자는 내놓지 않는다.
     * 다른 탭에 다녀와 다시 읽을 때는 같은 기간이라 보던 숫자를 그대로 둔다.
     */
    val state: StateFlow<InsightState> =
        combine(_period, readPeriod) { period, read -> read.takeIf { it.period == period } ?: InsightState(period, cache[period]) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), InsightState(_period.value, null))

    init {
        // 다른 탭도 미리 읽어 처음 눌러도 바로 보인다.
        viewModelScope.launch {
            InsightKind.entries.map { it.periodOf(today()) }.filter { it != _period.value }.forEach { period ->
                cache.getOrPut(period) { read(period).first() }
            }
        }
    }

    fun today(): LocalDate = today.invoke()

    fun select(kind: InsightKind) {
        _period.value = kind.periodOf(today())
    }

    /**
     * 앱이 돌아올 때 오늘을 다시 읽는다.
     * 이번 주나 이번 달을 보고 있었으면 새 기간으로 옮기고, 지난 기간을 보던 사람은 그대로 둔다.
     */
    fun refreshToday() {
        val before = _todayState.value
        val now = today()
        if (now == before) return
        _todayState.value = now
        val kind = _period.value.kind
        if (_period.value == kind.periodOf(before)) _period.value = kind.periodOf(now)
    }

    fun previous() {
        _period.value.previous()?.let { _period.value = it }
    }

    fun next() {
        _period.value.next(today())?.let { _period.value = it }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}

private fun InsightKind.periodOf(date: LocalDate): StatsPeriod = when (this) {
    InsightKind.Week -> StatsPeriod.Week.of(date)
    InsightKind.Month -> StatsPeriod.Month(date.yearMonth)
    InsightKind.Year -> StatsPeriod.Year(date.year)
    InsightKind.All -> StatsPeriod.All
}

internal val StatsPeriod.kind: InsightKind
    get() = when (this) {
        is StatsPeriod.Week -> InsightKind.Week
        is StatsPeriod.Month -> InsightKind.Month
        is StatsPeriod.Year -> InsightKind.Year
        StatsPeriod.All -> InsightKind.All
    }

/**
 * 많은 기분부터.
 * 같은 수면 기분 순서를 따른다.
 */
internal fun MoodCounts.sorted(): List<Pair<Mood, Int>> = Mood.ordered.map { it to (counts[it] ?: 0) }.sortedByDescending { it.second }

/**
 * 기분 흐름 한 칸.
 * [mood]는 그 칸의 날들에서 가장 많은 기분이고, 고른 기분이 없으면 null이다.
 */
internal data class MoodBucket(val slot: BucketSlot, val mood: Mood?)

internal sealed interface BucketSlot {
    data class Day(val date: LocalDate) : BucketSlot

    data class Week(val number: Int) : BucketSlot

    data class Month(val date: LocalDate) : BucketSlot

    data class Year(val date: LocalDate) : BucketSlot
}

/**
 * 기간을 칸으로 나눈다.
 * 주는 날마다, 달은 주마다(달력처럼 일요일 시작), 년은 달마다, 전체는 해마다다.
 * 칸의 기분은 그 칸 일기의 기분을 모두 세어 정한다.
 * 같으면 나중 기분을 쓴다.
 */
internal fun moodBuckets(period: StatsPeriod, days: List<DayMood>): List<MoodBucket> = when (period) {
    is StatsPeriod.Week -> {
        val start = period.start
        List(DAYS_IN_WEEK) { offset ->
            val date = start.plus(DatePeriod(days = offset))
            bucket(BucketSlot.Day(date), days.filter { it.date == date })
        }
    }

    is StatsPeriod.Month -> {
        val blanks = (period.month.onDay(1).dayOfWeek.ordinal + 1) % DAYS_IN_WEEK
        val weeks = (blanks + period.month.numberOfDays - 1) / DAYS_IN_WEEK + 1
        val byWeek = days.groupBy { (blanks + it.date.day - 1) / DAYS_IN_WEEK }
        List(weeks) { week -> bucket(BucketSlot.Week(week + 1), byWeek[week].orEmpty()) }
    }

    is StatsPeriod.Year -> {
        val byMonth = days.groupBy { it.date.month.number }
        (1..MONTHS).map { month -> bucket(BucketSlot.Month(LocalDate(period.year, month, 1)), byMonth[month].orEmpty()) }
    }

    StatsPeriod.All -> {
        val byYear = days.groupBy { it.date.year }
        // 일기가 없는 해도 빈 칸으로 두어 흐름이 끊긴 것이 보이게 한다.
        val years = if (byYear.isEmpty()) IntRange.EMPTY else byYear.keys.min()..byYear.keys.max()
        years.map { year -> bucket(BucketSlot.Year(LocalDate(year, 1, 1)), byYear[year].orEmpty()) }
    }
}

private fun bucket(slot: BucketSlot, days: List<DayMood>): MoodBucket =
    MoodBucket(slot, days.sortedBy { it.date }.flatMap { it.moods }.mostFrequent())

private const val DAYS_IN_WEEK = 7
private const val MONTHS = 12

private const val PERIOD_KEY = "insight_period"

internal fun encodePeriod(period: StatsPeriod): String = when (period) {
    is StatsPeriod.Week -> "w:${period.start}"
    is StatsPeriod.Month -> "m:${period.month}"
    is StatsPeriod.Year -> "y:${period.year}"
    StatsPeriod.All -> "all"
}

internal fun decodePeriod(text: String): StatsPeriod? = runCatching {
    val value = text.substringAfter(':')
    when (text.substringBefore(':')) {
        "w" -> StatsPeriod.Week(LocalDate.parse(value))
        "m" -> StatsPeriod.Month(YearMonth.parse(value))
        "y" -> StatsPeriod.Year(value.toInt())
        "all" -> StatsPeriod.All
        else -> null
    }
}.getOrNull()
