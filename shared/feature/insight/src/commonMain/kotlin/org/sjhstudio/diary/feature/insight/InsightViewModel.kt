package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod

/** 기간 탭. */
enum class InsightKind { Week, Month, Year, All }

/** 돌아보기(FR-028~FR-031). 기간을 칸으로 나눈 기분 흐름과, 년은 하루 한 칸 지도를 함께 읽는다. */
class InsightViewModel(
    private val stats: StatsRepository,
    private val today: () -> LocalDate = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
) : ViewModel() {
    private val _period = MutableStateFlow<StatsPeriod>(StatsPeriod.Month(today().yearMonth))
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val counts: StateFlow<MoodCounts?> =
        _period.flatMapLatest(stats::moodCounts).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val days: StateFlow<List<DayMood>> =
        _period
            .flatMapLatest { if (it == StatsPeriod.All) flowOf(emptyList()) else stats.moodFlow(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pixels: StateFlow<Map<LocalDate, Mood?>> =
        _period
            .flatMapLatest { if (it is StatsPeriod.Year) stats.yearPixels(it.year) else flowOf(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyMap())

    fun today(): LocalDate = today.invoke()

    fun select(kind: InsightKind) {
        val date = today()
        _period.value =
            when (kind) {
                InsightKind.Week -> StatsPeriod.Week.of(date)
                InsightKind.Month -> StatsPeriod.Month(date.yearMonth)
                InsightKind.Year -> StatsPeriod.Year(date.year)
                InsightKind.All -> StatsPeriod.All
            }
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

internal val StatsPeriod.kind: InsightKind
    get() = when (this) {
        is StatsPeriod.Week -> InsightKind.Week
        is StatsPeriod.Month -> InsightKind.Month
        is StatsPeriod.Year -> InsightKind.Year
        StatsPeriod.All -> InsightKind.All
    }

/** 많은 기분부터. 같은 수면 기분 순서를 따른다. */
internal fun MoodCounts.sorted(): List<Pair<Mood, Int>> = Mood.entries.map { it to (counts[it] ?: 0) }.sortedByDescending { it.second }

/** 기분 흐름 한 칸. [mood]는 그 칸의 날들에서 가장 많은 기분이고, 일기는 있는데 기분이 없으면 null이다. */
internal data class MoodBucket(val slot: BucketSlot, val mood: Mood?, val written: Boolean)

internal sealed interface BucketSlot {
    data class Day(val date: LocalDate) : BucketSlot

    data class Week(val number: Int) : BucketSlot

    data class Month(val date: LocalDate) : BucketSlot
}

/**
 * 기간을 칸으로 나눈다. 주는 날마다, 달은 주마다(달력처럼 일요일 시작), 년은 달마다다.
 * 칸의 기분은 날마다의 마지막 기분을 세어 정한다. 같으면 나중 날의 기분을 쓴다.
 */
internal fun moodBuckets(period: StatsPeriod, days: List<DayMood>): List<MoodBucket> = when (period) {
    is StatsPeriod.Week -> {
        val start = period.start
        List(DAYS_IN_WEEK) { offset ->
            val date = start.plus(DatePeriod(days = offset))
            val day = days.firstOrNull { it.date == date }
            MoodBucket(BucketSlot.Day(date), day?.mood, day != null)
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

    StatsPeriod.All -> emptyList()
}

private fun bucket(slot: BucketSlot, days: List<DayMood>): MoodBucket {
    val counts = days.mapNotNull { it.mood }.groupingBy { it }.eachCount()
    val most = counts.values.maxOrNull()
    val mood = days.sortedBy { it.date }.lastOrNull { it.mood != null && counts[it.mood] == most }?.mood
    return MoodBucket(slot, mood, days.isNotEmpty())
}

private const val DAYS_IN_WEEK = 7
private const val MONTHS = 12
