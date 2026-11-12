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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.mostFrequent
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/** 기간 탭 */
enum class InsightKind { Week, Month, Year, All }

/** 한 기간을 읽은 값 */
data class InsightData(val counts: MoodCounts, val days: List<DayMood>, val pixels: Map<LocalDate, Mood?>)

/**
 * 기간과 그 기간의 숫자. [data]가 null이면 읽는 중, [failed]면 다시 시도를 보임
 * 기분 기록이 들어 있어 [toString]에서 뺌(헌법 II)
 */
data class InsightState(val period: StatsPeriod, val data: InsightData?, val failed: Boolean = false) {
    override fun toString(): String = "InsightState"
}

/** 돌아보기(FR-028~FR-031). 기간을 칸으로 나눈 기분 흐름과, 년은 하루 한 칸 지도도 함께 읽음 */
class InsightViewModel(
    private val stats: StatsRepository,
    private val today: () -> LocalDate = { currentDate() },
    plans: PlanRepository,
    private val saved: SavedStateHandle,
    private val tips: ContextTips,
    entries: EntryRepository
) : ViewModel() {
    /** 고급 통계를 볼 수 있는지. 요금제가 바뀌면 바로 따름 */
    val advancedOpen: StateFlow<Boolean> =
        plans.confirmed.filterNotNull()
            .map { it.advancedStats }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, false)

    // 시스템이 앱을 끄고 다시 띄워도 보던 기간 유지
    private val _period = MutableStateFlow(saved.get<String>(PERIOD_KEY)?.let(::decodePeriod) ?: StatsPeriod.Month(today().yearMonth))
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    init {
        viewModelScope.launchSafely { _period.collect { saved[PERIOD_KEY] = encodePeriod(it) } }
    }

    private val _todayState = MutableStateFlow(today())

    /** 화면이 보는 오늘. 앱이 돌아올 때 [refreshToday]가 바꿈 */
    val todayState: StateFlow<LocalDate> = _todayState.asStateFlow()

    /** 읽어 둔 기간. 탭마다 비웠다 그리면 번쩍여, 있는 값을 바로 보이고 새 값으로 바꿈 */
    private val cache = mutableMapOf<StatsPeriod, InsightData>()

    /** 다시 시도마다 올라 고른 기간을 새로 읽음 */
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val readPeriod: Flow<InsightState> =
        combine(_period, attempt) { period, _ -> period }.flatMapLatest { period ->
            read(period)
                .map { data ->
                    cache[period] = data
                    InsightState(period, data)
                }
                // 읽기 실패 시 읽는 중으로 남기지 않고 다시 시도를 보임
                .catch { if (it is Exception) emit(InsightState(period, null, failed = true)) else throw it }
        }

    private fun read(period: StatsPeriod): Flow<InsightData> {
        val pixels = if (period is StatsPeriod.Year) stats.yearPixels(period.year) else flowOf(emptyMap())
        return combine(stats.moodCounts(period), stats.moodFlow(period), pixels) { counts, days, map -> InsightData(counts, days, map) }
    }

    /**
     * 기간과 그 기간의 숫자를 한 값으로 묶음
     * 기간을 바꾸면 읽어 둔 값을 바로, 없으면 읽는 중. 앞 기간 숫자는 내놓지 않음
     * 다른 탭에 다녀와 다시 읽을 때는 같은 기간이라 보던 숫자 유지
     */
    val state: StateFlow<InsightState> =
        combine(_period, readPeriod) { period, read -> read.takeIf { it.period == period } ?: InsightState(period, cache[period]) }
            .stateInSafely(viewModelScope, WhileUiSubscribed, InsightState(_period.value, null))

    init {
        // 다른 탭도 미리 읽어 처음 눌러도 바로 보이게
        viewModelScope.launchSafely {
            InsightKind.entries.map { it.periodOf(today()) }.filter { it != _period.value }.forEach { period ->
                orElseOnFailure(Unit) { cache.getOrPut(period) { read(period).first() } }
            }
        }
    }

    fun today(): LocalDate = today.invoke()

    /**
     * 첫 일기의 해. 기간 고르기가 이 해부터 보임. 일기가 없으면 null
     * 전체 기분 목록 대신 가벼운 달 목록으로 셈(일기가 바뀔 때마다 다시 읽음)
     */
    val firstYear: StateFlow<Int?> =
        entries.months()
            .map { months -> months.minOfOrNull { it.year } }
            .stateInSafely(viewModelScope, WhileUiSubscribed, null)

    /**
     * 칸이나 기분 막대를 눌러 보면 누를 수 있다는 안내는 할 일을 다 함
     * ×를 누르지 않아도 다시 띄우지 않음(Apple TipKit `actionPerformed`)
     * 화면이 찾기로 넘어가도 끝까지 쓰게 화면 모델에서 저장
     */
    fun retry() {
        attempt.value++
    }

    fun tried() {
        val tips = tips ?: return
        viewModelScope.launchSafely { tips.done(ContextTip.INSIGHT_OPEN) }
    }

    fun select(kind: InsightKind) {
        _period.value = kind.periodOf(today())
    }

    /**
     * 앱이 돌아올 때 오늘을 다시 읽음
     * 이번 주·이번 달을 보던 중이면 새 기간으로 옮기고, 지난 기간을 보던 중이면 그대로
     */
    fun refreshToday() {
        val before = _todayState.value
        val now = today()
        if (now == before) return
        _todayState.value = now
        val kind = _period.value.kind
        if (_period.value == kind.periodOf(before)) _period.value = kind.periodOf(now)
    }

    /** 기간 고르기에서 고른 기간으로. 오늘 뒤 기간은 받지 않음 */
    fun jump(period: StatsPeriod) {
        val start = period.range?.start
        if (start == null || start <= today()) _period.value = period
    }

    fun previous() {
        _period.value.previous()?.let { _period.value = it }
    }

    fun next() {
        _period.value.next(today())?.let { _period.value = it }
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

/** 많은 기분부터. 같은 수면 기분 순서 */
internal fun MoodCounts.sorted(): List<Pair<Mood, Int>> = Mood.ordered.map { it to (counts[it] ?: 0) }.sortedByDescending { it.second }

/**
 * 기분 흐름 한 칸
 * [mood]: 그 칸에서 가장 많은 기분, 고른 기분이 없으면 null.
 * [range]: 칸이 덮는 날짜. 누르면 이 기간 일기를 찾기로 모아 봄
 * [written]: 그 칸에 일기가 한 편이라도 있는지. 없으면 누를 수 없음
 */
internal data class MoodBucket(val slot: BucketSlot, val mood: Mood?, val range: DateRange, val written: Boolean)

internal sealed interface BucketSlot {
    data class Day(val date: LocalDate) : BucketSlot

    data class Week(val number: Int) : BucketSlot

    data class Month(val date: LocalDate) : BucketSlot

    data class Year(val date: LocalDate) : BucketSlot
}

/**
 * 기간을 칸으로 나눔: 주는 날마다, 달은 주마다(달력처럼 일요일 시작), 년은 달마다, 전체는 해마다
 * 칸의 기분은 그 칸 일기의 기분을 모두 세어 정하고, 같으면 나중 기분
 */
internal fun moodBuckets(period: StatsPeriod, days: List<DayMood>): List<MoodBucket> = when (period) {
    is StatsPeriod.Week -> {
        val start = period.start
        List(DAYS_IN_WEEK) { offset ->
            val date = start.plus(DatePeriod(days = offset))
            bucket(BucketSlot.Day(date), DateRange(date, date), days.filter { it.date == date })
        }
    }

    is StatsPeriod.Month -> {
        val blanks = (period.month.onDay(1).dayOfWeek.ordinal + 1) % DAYS_IN_WEEK
        val weeks = (blanks + period.month.numberOfDays - 1) / DAYS_IN_WEEK + 1
        val byWeek = days.groupBy { (blanks + it.date.day - 1) / DAYS_IN_WEEK }
        List(weeks) { week ->
            // 달의 첫 주와 마지막 주는 그 달 안의 날만 덮음(화면도 그 달만 보임)
            val first = (week * DAYS_IN_WEEK - blanks + 1).coerceAtLeast(1)
            val last = (first + DAYS_IN_WEEK - 1 - if (week == 0) blanks else 0).coerceAtMost(period.month.numberOfDays)
            val range = DateRange(period.month.onDay(first), period.month.onDay(last))
            bucket(BucketSlot.Week(week + 1), range, byWeek[week].orEmpty())
        }
    }

    is StatsPeriod.Year -> {
        val byMonth = days.groupBy { it.date.month.number }
        (1..MONTHS).map { month ->
            val yearMonth = YearMonth(period.year, month)
            val range = DateRange(yearMonth.onDay(1), yearMonth.onDay(yearMonth.numberOfDays))
            bucket(BucketSlot.Month(yearMonth.onDay(1)), range, byMonth[month].orEmpty())
        }
    }

    StatsPeriod.All -> {
        val byYear = days.groupBy { it.date.year }
        // 일기 없는 해도 빈 칸으로 두어 흐름이 끊긴 게 보이게
        val years = if (byYear.isEmpty()) IntRange.EMPTY else byYear.keys.min()..byYear.keys.max()
        years.map { year ->
            val range = DateRange(LocalDate(year, 1, 1), LocalDate(year, MONTHS, LAST_DAY_OF_DECEMBER))
            bucket(BucketSlot.Year(LocalDate(year, 1, 1)), range, byYear[year].orEmpty())
        }
    }
}

private fun bucket(slot: BucketSlot, range: DateRange, days: List<DayMood>): MoodBucket =
    MoodBucket(slot, days.sortedBy { it.date }.flatMap { it.moods }.mostFrequent(), range, days.any { it.count > 0 })

private const val DAYS_IN_WEEK = 7
private const val MONTHS = 12
private const val LAST_DAY_OF_DECEMBER = 31

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
