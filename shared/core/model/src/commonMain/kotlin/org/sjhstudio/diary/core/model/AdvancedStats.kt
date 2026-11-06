package org.sjhstudio.diary.core.model

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 쓴 시간대. 새벽은 0~5시, 아침은 6~11시, 낮은 12~17시, 저녁은 18~23시다. */
enum class DayPart { DAWN, MORNING, AFTERNOON, EVENING }

/** 기분의 큰 묶음. 점수가 아니라 고급 통계에서 "좋은 기분"을 세는 데만 쓴다. */
enum class MoodTone { GOOD, NEUTRAL, HARD }

val Mood.tone: MoodTone
    get() = when (this) {
        Mood.GOOD, Mood.LAUGH -> MoodTone.GOOD
        Mood.COOL, Mood.MEH -> MoodTone.NEUTRAL
        Mood.ANGRY, Mood.SAD, Mood.BAD, Mood.SICK, Mood.TIRED -> MoodTone.HARD
    }

/** 고급 통계의 기간. [compare]는 견줄 지난 기간이고 전체에는 없다. */
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

        // 작년 이맘때까지만 본다. 2월 29일은 작년에 없어서 달 끝으로 맞춘다.
        THIS_YEAR -> DateRange(LocalDate(today.year - 1, 1, 1), today.minus(DatePeriod(years = 1)))

        ALL -> null
    }

    private companion object {
        const val RECENT_DAYS = 30
    }
}

/** 기분을 고른 일기 [total]편 중 좋은 기분이 [good]편이다. */
data class GoodShare(val good: Int, val total: Int) {
    val rate: Double get() = if (total == 0) 0.0 else good.toDouble() / total

    /** 화면에 쓰는 백분율. */
    val percent: Int get() = if (total == 0) 0 else (good * PERCENT + total / 2) / total

    private companion object {
        const val PERCENT = 100
    }
}

/** 기분을 견주어 볼 조건. */
sealed interface StatsFactor {
    data class Sky(val weather: Weather) : StatsFactor

    data class Weekday(val day: DayOfWeek) : StatsFactor

    data class Part(val part: DayPart) : StatsFactor

    data class Place(val name: String) : StatsFactor

    data object WithPhotos : StatsFactor

    data object LongEntries : StatsFactor
}

/** 조건이 맞을 때와 아닐 때 좋은 기분이 뚜렷하게 다른 곳. 까닭이라고 말할 수는 없는 함께 나타난 차이다. */
data class MoodGap(val factor: StatsFactor, val here: GoodShare, val others: GoodShare) {
    val more: Boolean get() = here.rate > others.rate
}

/** 지난 기간과 견준 값. 일기가 모자란 쪽은 null이다. */
data class StatsChange(val goodBefore: GoodShare?, val goodNow: GoodShare?, val daysBefore: Int, val daysNow: Int)

/** 한 기분과 가장 자주 함께한 것. */
data class MoodCompany(val mood: Mood, val entries: Int, val weather: Weather?, val weekday: DayOfWeek?, val place: String?)

/** 기록 습관. 기분과 상관없이 쓴 만큼 센다. */
data class RecordHabit(
    val entries: Int,
    val daysWritten: Int,
    val longestRun: Int,
    val characters: Long,
    val photos: Int,
    val topPart: DayPart?
)

/** 고급 통계(스펙 006 US5). 기존 통계와 겹치지 않는 분석만 담는다. */
data class AdvancedStats(
    val moodEntries: Int,
    val gaps: List<MoodGap>,
    val change: StatsChange?,
    val weekdays: Map<DayOfWeek, GoodShare>,
    val dayParts: Map<DayPart, GoodShare>,
    val weather: Map<Weather, GoodShare>,
    val places: List<Pair<String, GoodShare>>,
    val company: List<MoodCompany>,
    val habit: RecordHabit
) {
    /** 뜻이 있을 만큼 쌓였는지 본다. */
    val enough: Boolean get() = habit.entries >= MIN_ENTRIES

    /** 기분을 고른 일기가 견줄 만큼 있는지 본다. 없으면 기분 칸은 말하지 않고 기록 습관만 보인다. */
    val moodReady: Boolean get() = moodEntries >= MIN_ENTRIES

    companion object {
        const val MIN_ENTRIES = 7

        /** 묶음별 표에 보이는 최소 편수. 이보다 적으면 비율이 뜻이 없다. */
        const val MIN_SHARE = 5
        const val MIN_COMPANY = 3
        const val TOP_PLACES = 5
        const val MAX_GAPS = 3

        /** 차이를 말하려면 양쪽 모두 이만큼은 있어야 한다. */
        const val MIN_GROUP = 8

        /** 이보다 작은 차이는 맞아도 쓸모가 없다. */
        const val MIN_GAP = 0.15

        /** 아무 관계가 없는 기록에서 하나라도 잘못 말할 확률의 상한. 모의 시험 근거는 스펙 006 research. */
        const val FALSE_ALARM = 0.05
    }
}

/** 통계에 쓰는 일기 한 편의 값. 본문은 담지 않는다. */
data class StatsEntry(
    val date: LocalDate,
    val hour: Int,
    val mood: Mood?,
    val weather: Weather?,
    val place: String?,
    val photos: Int = 0,
    val length: Int = 0
)

/** [rows]는 고른 기간의 일기이고 [before]는 견줄 기간의 일기다. 견줄 기간이 없으면 null이다. */
fun advancedStats(rows: List<StatsEntry>, before: List<StatsEntry>? = null): AdvancedStats {
    val moods = rows.filter { it.mood != null }
    return AdvancedStats(
        moodEntries = moods.size,
        gaps = moodGaps(moods),
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
                .sortedWith(compareByDescending<Map.Entry<String, GoodShare>> { it.value.total }.thenBy { it.key })
                .take(AdvancedStats.TOP_PLACES)
                .map { it.key to it.value },
        company = company(moods),
        habit = habit(rows)
    )
}

private fun StatsEntry.placeName(): String? = place?.trim()?.takeIf { it.isNotEmpty() }

private fun List<StatsEntry>.share() = GoodShare(count { it.mood?.tone == MoodTone.GOOD }, size)

private fun <K> Map<K, List<StatsEntry>>.shares(): Map<K, GoodShare> =
    mapValues { it.value.share() }.filterValues { it.total >= AdvancedStats.MIN_SHARE }

private fun change(rows: List<StatsEntry>, before: List<StatsEntry>): StatsChange {
    fun List<StatsEntry>.good() = filter { it.mood != null }.share().takeIf { it.total >= AdvancedStats.MIN_SHARE }
    return StatsChange(before.good(), rows.good(), before.map { it.date }.distinct().size, rows.map { it.date }.distinct().size)
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

/** 하루도 빠지지 않고 이어 쓴 가장 긴 날수. [days]는 겹치지 않고 정렬되어 있다. */
private fun longestRun(days: List<LocalDate>): Int {
    var longest = 0
    var run = 0
    days.forEachIndexed { index, day ->
        run = if (index > 0 && days[index - 1].plus(DatePeriod(days = 1)) == day) run + 1 else 1
        longest = maxOf(longest, run)
    }
    return longest
}

/**
 * 조건별로 좋은 기분의 비율을 나머지 일기와 견준다(두 비율 차이 검정).
 * 조건을 여럿 보면 우연히 다른 곳이 나오기 쉬워서 본 조건 수만큼 기준을 높인다(본페로니 보정).
 * 그래서 관계가 없는 기록이면 스무 번에 한 번도 말하지 않지만, 일기가 적으면 있는 차이도 잘 찾지 못한다.
 *
 * 같은 묶음(날씨, 요일처럼 한 일기에 하나만 있는 것) 안에서는 찾은 조건을 빼고 다시 견준다.
 * 비 오는 날이 나쁘면 나머지 날씨는 그것만으로 좋아 보이기 때문이다.
 */
internal fun moodGaps(moods: List<StatsEntry>): List<MoodGap> {
    val longFrom = moods.map { it.length }.sorted().let { if (it.isEmpty()) 0 else it[it.size * LONG_QUARTILE / QUARTERS] }
    val groups = HashMap<StatsFactor, MutableList<StatsEntry>>()
    for (row in moods) {
        for (factor in factors(row, longFrom)) groups.getOrPut(factor) { ArrayList() }.add(row)
    }
    val tested = groups.count { (_, group) -> group.size >= AdvancedStats.MIN_GROUP && moods.size - group.size >= AdvancedStats.MIN_GROUP }
    val limit = normalQuantile(AdvancedStats.FALSE_ALARM / tested.coerceAtLeast(1) / 2)
    return groups.entries
        .groupBy { (factor, _) -> factor::class }
        .values
        .flatMap { family -> gapsInFamily(moods, family.associate { it.key to it.value }, limit) }
        .sortedByDescending { abs(it.here.rate - it.others.rate) }
        .take(AdvancedStats.MAX_GAPS)
}

/** 묶음 안에서 가장 큰 차이부터 하나씩 빼며 찾는다. */
private fun gapsInFamily(moods: List<StatsEntry>, family: Map<StatsFactor, List<StatsEntry>>, limit: Double): List<MoodGap> {
    val found = ArrayList<MoodGap>()
    val left = family.toMutableMap()
    var pool = moods.size to moods.count { it.mood?.tone == MoodTone.GOOD }
    while (true) {
        val best =
            left
                .mapNotNull { (factor, group) ->
                    val here = group.share()
                    MoodGap(factor, here, GoodShare(pool.second - here.good, pool.first - here.total))
                }.filter { it.isClear(limit) }
                // 둘뿐이면 같은 차이가 두 번 나온다. 그때는 좋은 기분이 적은 쪽으로 말한다.
                .maxWithOrNull(compareBy<MoodGap> { abs(it.here.rate - it.others.rate) }.thenBy { -it.here.rate }) ?: return found
        found += best
        left.remove(best.factor)
        pool = pool.first - best.here.total to pool.second - best.here.good
    }
}

private fun MoodGap.isClear(limit: Double): Boolean = here.total >= AdvancedStats.MIN_GROUP &&
    others.total >= AdvancedStats.MIN_GROUP &&
    abs(here.rate - others.rate) >= AdvancedStats.MIN_GAP &&
    abs(zScore(this)) >= limit

private fun factors(row: StatsEntry, longFrom: Int): List<StatsFactor> = buildList {
    add(StatsFactor.Weekday(row.date.dayOfWeek))
    add(StatsFactor.Part(dayPart(row.hour)))
    row.weather?.let { add(StatsFactor.Sky(it)) }
    row.placeName()?.let { add(StatsFactor.Place(it)) }
    if (row.photos > 0) add(StatsFactor.WithPhotos)
    if (row.length > 0 && row.length >= longFrom) add(StatsFactor.LongEntries)
}

private fun zScore(gap: MoodGap): Double {
    val pooled = (gap.here.good + gap.others.good).toDouble() / (gap.here.total + gap.others.total)
    val error = sqrt(pooled * (1 - pooled) * (1.0 / gap.here.total + 1.0 / gap.others.total))
    return if (error == 0.0) 0.0 else (gap.here.rate - gap.others.rate) / error
}

/** 표준정규분포에서 오른쪽 넓이가 [tail]인 지점(Abramowitz·Stegun 26.2.23). 0.5보다 작은 [tail]만 쓴다. */
internal fun normalQuantile(tail: Double): Double {
    val t = sqrt(-2 * ln(tail))
    return t - (C0 + C1 * t + C2 * t * t) / (1 + D1 * t + D2 * t * t + D3 * t * t * t)
}

private fun dayPart(hour: Int): DayPart = when (hour) {
    in 0..5 -> DayPart.DAWN
    in 6..11 -> DayPart.MORNING
    in 12..17 -> DayPart.AFTERNOON
    else -> DayPart.EVENING
}

private const val QUARTERS = 4
private const val LONG_QUARTILE = 3
private const val C0 = 2.515517
private const val C1 = 0.802853
private const val C2 = 0.010328
private const val D1 = 1.432788
private const val D2 = 0.189269
private const val D3 = 0.001308
