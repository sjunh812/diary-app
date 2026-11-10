package org.sjhstudio.diary.core.model

import kotlin.math.abs
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 쓴 시간대. 새벽 0~5시, 아침 6~11시, 낮 12~17시, 저녁 18~23시 */
enum class DayPart { DAWN, MORNING, AFTERNOON, EVENING }

/** 기분의 큰 묶음. 점수가 아니라 고급 통계에서 "좋은 기분"을 셀 때만 씀 */
enum class MoodTone { GOOD, NEUTRAL, HARD }

val Mood.tone: MoodTone
    get() = when (this) {
        Mood.GOOD, Mood.LAUGH -> MoodTone.GOOD
        Mood.COOL, Mood.MEH -> MoodTone.NEUTRAL
        Mood.ANGRY, Mood.SAD, Mood.BAD, Mood.SICK, Mood.TIRED -> MoodTone.HARD
    }

/** 고급 통계 기간. [compare]는 견줄 지난 기간이고 전체에는 없음 */
enum class AdvancedPeriod {
    RECENT,
    THIS_YEAR,
    ALL;

    fun range(today: LocalDate): DateRange? = when (this) {
        RECENT -> DateRange(today.minus(DatePeriod(days = RECENT_DAYS - 1)), today)
        THIS_YEAR -> DateRange(LocalDate(today.year, 1, 1), today)
        ALL -> null
    }

    fun compare(today: LocalDate): DateRange? = when (this) {
        RECENT -> {
            val end = today.minus(DatePeriod(days = RECENT_DAYS))
            DateRange(end.minus(DatePeriod(days = RECENT_DAYS - 1)), end)
        }

        // 작년 이맘때까지만. 2월 29일은 작년에 없어 달 끝으로 맞춤
        THIS_YEAR -> DateRange(LocalDate(today.year - 1, 1, 1), today.minus(DatePeriod(years = 1)))

        ALL -> null
    }

    private companion object {
        const val RECENT_DAYS = 30
    }
}

/** 기분을 고른 일기 [total]편 중 좋은 기분 [good]편, 힘든 기분 [hard]편 */
data class ToneShare(val good: Int, val total: Int, val hard: Int = 0) {
    val rate: Double get() = rateOf(good)
    val hardRate: Double get() = rateOf(hard)

    /** 화면에 쓰는 백분율 */
    val percent: Int get() = percentOf(good, total)

    // 둘을 나란히 보여 반올림해도 합이 100을 넘지 않게
    val hardPercent: Int get() = minOf(percentOf(hard, total), if (good + hard <= total) 100 - percent else 100)

    private fun rateOf(count: Int) = if (total == 0) 0.0 else count.toDouble() / total

    private companion object {
        const val PERCENT = 100
    }
}

/** 한 기간의 값. 기분을 고른 일기가 부족하면 [moods]와 [topMood]는 null */
data class StatsSnapshot(val moods: ToneShare?, val topMood: Mood?, val days: Int, val entries: Int, val characters: Long, val photos: Int)

/** 지난 기간과 견준 값 */
data class StatsChange(val before: StatsSnapshot, val now: StatsSnapshot)

/** 요즘 기분의 흐름 */
enum class MoodTrend { BETTER, HARDER, STEADY }

/**
 * 최근 30일과 그 전 30일의 기분. 까닭은 말하지 않고 일어난 변화만 한 줄로.
 * 좋은 기분이 늘거나 힘든 기분이 줄면 나아진 것으로 보고, 더 많이 움직인 쪽을 [hardMoved]로 알림.
 */
data class RecentMood(val trend: MoodTrend, val before: ToneShare, val now: ToneShare) {
    val hardMoved: Boolean get() = abs(now.hardRate - before.hardRate) > abs(now.rate - before.rate)
}

/** 한 기분과 가장 자주 함께한 것 */
data class MoodCompany(val mood: Mood, val entries: Int, val weather: Weather?, val weekday: DayOfWeek?, val place: String?)

/** 기록 습관. 기분과 상관없이 쓴 만큼 셈 */
data class RecordHabit(
    val entries: Int,
    val daysWritten: Int,
    val longestRun: Int,
    val characters: Long,
    val photos: Int,
    val topPart: DayPart?
)

/** 고급 통계(스펙 006 US5). 기존 통계와 겹치지 않는 분석만 담음 */
data class AdvancedStats(
    val moodEntries: Int,
    val change: StatsChange?,
    val weekdays: Map<DayOfWeek, ToneShare>,
    val dayParts: Map<DayPart, ToneShare>,
    val weather: Map<Weather, ToneShare>,
    val places: List<Pair<String, ToneShare>>,
    val company: List<MoodCompany>,
    val habit: RecordHabit,
    val recent: RecentMood? = null
) {
    /** 뜻이 있을 만큼 쌓였는지 */
    val enough: Boolean get() = habit.entries >= MIN_ENTRIES

    /** 기분을 고른 일기가 견줄 만큼 있는지. 없으면 기분 칸은 말하지 않고 기록 습관만 보임 */
    val moodReady: Boolean get() = moodEntries >= MIN_ENTRIES

    companion object {
        const val MIN_ENTRIES = 7

        /** 묶음별 표에 보이는 최소 편수. 이보다 적으면 비율이 뜻이 없음 */
        const val MIN_SHARE = 5
        const val MIN_COMPANY = 3
        const val TOP_PLACES = 5

        /** 요즘 기분을 말하려면 두 기간 모두 있어야 하는 편수 */
        const val MIN_TREND = 8

        /** 이보다 작은 변화는 일기 몇 편으로도 뒤집혀 비슷하다고 말함 */
        const val MIN_SHIFT = 0.15
    }
}

/** 통계에 쓰는 일기 한 편의 값. 본문은 담지 않음 */
data class StatsEntry(
    val date: LocalDate,
    val hour: Int,
    val mood: Mood?,
    val weather: Weather?,
    val place: String?,
    val photos: Int = 0,
    val length: Int = 0
)

/** [rows]는 고른 기간, [before]는 견줄 기간의 일기. 견줄 기간이 없으면 null */
fun advancedStats(rows: List<StatsEntry>, before: List<StatsEntry>? = null): AdvancedStats {
    val moods = rows.filter { it.mood != null }
    return AdvancedStats(
        moodEntries = moods.size,
        change = before?.takeIf { it.isNotEmpty() }?.let { change(rows, it) },
        weekdays = moods.groupBy { it.date.dayOfWeek }.shares(),
        dayParts = moods.groupBy { dayPart(it.hour) }.shares(),
        weather = moods.filter { it.weather != null }.groupBy { it.weather!! }.shares(),
        places =
            moods
                .filter { it.placeName() != null }
                .groupBy { it.placeName()!! }
                .shares()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, ToneShare>> { it.value.total }.thenBy { it.key })
                .take(AdvancedStats.TOP_PLACES)
                .map { it.key to it.value },
        company = company(moods),
        habit = habit(rows)
    )
}

private fun StatsEntry.placeName(): String? = place?.trim()?.takeIf { it.isNotEmpty() }

private fun List<StatsEntry>.share() = ToneShare(count { it.mood?.tone == MoodTone.GOOD }, size, count { it.mood?.tone == MoodTone.HARD })

private fun <K> Map<K, List<StatsEntry>>.shares(): Map<K, ToneShare> =
    mapValues { it.value.share() }.filterValues { it.total >= AdvancedStats.MIN_SHARE }

private fun change(rows: List<StatsEntry>, before: List<StatsEntry>) = StatsChange(before.snapshot(), rows.snapshot())

private fun List<StatsEntry>.snapshot(): StatsSnapshot {
    val moods = filter { it.mood != null }
    val enough = moods.size >= AdvancedStats.MIN_SHARE
    return StatsSnapshot(
        moods = moods.share().takeIf { enough },
        topMood = if (enough) moods.mapNotNull { it.mood }.mostCommon() else null,
        days = map { it.date }.distinct().size,
        entries = size,
        characters = sumOf { it.length.toLong() },
        photos = sumOf { it.photos }
    )
}

/** [rows]에서 최근 30일과 그 전 30일을 견줌. 어느 쪽이든 일기가 부족하면 말하지 않음 */
fun recentMood(rows: List<StatsEntry>, today: LocalDate): RecentMood? {
    val now = AdvancedPeriod.RECENT.range(today) ?: return null
    val before = AdvancedPeriod.RECENT.compare(today) ?: return null
    val moods = rows.filter { it.mood != null }
    val nowShare = moods.filter { it.date in now.start..now.end }.share()
    val beforeShare = moods.filter { it.date in before.start..before.end }.share()
    if (nowShare.total < AdvancedStats.MIN_TREND || beforeShare.total < AdvancedStats.MIN_TREND) return null
    val shift = (nowShare.rate - nowShare.hardRate) - (beforeShare.rate - beforeShare.hardRate)
    val trend = when {
        shift >= AdvancedStats.MIN_SHIFT -> MoodTrend.BETTER
        shift <= -AdvancedStats.MIN_SHIFT -> MoodTrend.HARDER
        else -> MoodTrend.STEADY
    }
    return RecentMood(trend, beforeShare, nowShare)
}

/**
 * 큰 수를 짧게 쓸 때의 값.
 * [small]과 [big]은 언어마다 다른 묶음 단위(한국어·일본어는 만·억, 영어는 천·백만).
 */
data class CompactCount(val digits: String, val big: Boolean)

/**
 * [FULL_LIMIT]보다 작으면 그대로 다 적어 null.
 * 그 위는 작은 단위는 정수로, 큰 단위는 소수 한 자리까지.
 */
fun compactCount(value: Long, small: Long, big: Long): CompactCount? = when {
    value < FULL_LIMIT -> null

    value < big -> CompactCount((value / small).toString(), big = false)

    else -> {
        val tenths = value / (big / TENTHS)
        val whole = tenths / TENTHS
        val rest = tenths % TENTHS
        CompactCount(if (rest == 0L || whole >= TENTHS) whole.toString() else "$whole.$rest", big = true)
    }
}

private fun company(moods: List<StatsEntry>): List<MoodCompany> = moods
    .groupBy { it.mood!! }
    .filterValues { it.size >= AdvancedStats.MIN_COMPANY }
    .map { (mood, group) ->
        MoodCompany(
            mood,
            group.size,
            group.mapNotNull { it.weather }.mostCommon(),
            group.map { it.date.dayOfWeek }.mostCommon(),
            group.mapNotNull { it.placeName() }.mostCommon()
        )
    }.sortedByDescending { it.entries }

private fun <T> List<T>.mostCommon(): T? = groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

private fun habit(rows: List<StatsEntry>): RecordHabit {
    val days = rows.map { it.date }.distinct().sorted()
    return RecordHabit(
        entries = rows.size,
        daysWritten = days.size,
        longestRun = longestRun(days),
        characters = rows.sumOf { it.length.toLong() },
        photos = rows.sumOf { it.photos },
        topPart = rows.map { dayPart(it.hour) }.mostCommon()
    )
}

/** 하루도 빠짐없이 이어 쓴 가장 긴 날수. [days]는 겹치지 않고 정렬된 상태 */
private fun longestRun(days: List<LocalDate>): Int {
    var longest = 0
    var run = 0
    days.forEachIndexed { index, day ->
        run = if (index > 0 && days[index - 1].plus(DatePeriod(days = 1)) == day) run + 1 else 1
        longest = maxOf(longest, run)
    }
    return longest
}

private fun dayPart(hour: Int): DayPart = when (hour) {
    in 0..5 -> DayPart.DAWN
    in 6..11 -> DayPart.MORNING
    in 12..17 -> DayPart.AFTERNOON
    else -> DayPart.EVENING
}

private const val FULL_LIMIT = 100_000L
private const val TENTHS = 10L
