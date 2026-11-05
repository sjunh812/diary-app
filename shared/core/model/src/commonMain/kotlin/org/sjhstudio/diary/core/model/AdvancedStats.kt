package org.sjhstudio.diary.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** 쓴 시간대. 새벽은 0~5시, 아침은 6~11시, 낮은 12~17시, 저녁은 18~23시다. */
enum class DayPart { DAWN, MORNING, AFTERNOON, EVENING }

/** 한 칸의 가장 많은 기분. 같으면 먼저 나온 기분이고, 기분을 고른 일기가 없으면 null이다. */
data class TopMood(val mood: Mood?, val entries: Int)

/** 고급 통계(스펙 006 US5). 기존 통계와 겹치지 않는 분석만 담는다. */
data class AdvancedStats(
    val entries: Int,
    val daysWritten: Int,
    val weekdays: Map<DayOfWeek, TopMood>,
    val dayParts: Map<DayPart, TopMood>,
    val weather: Map<Weather, TopMood>,
    val places: List<Pair<String, Int>>
) {
    /** 뜻이 있을 만큼 쌓였는지 본다. */
    val enough: Boolean get() = entries >= MIN_ENTRIES

    companion object {
        const val MIN_ENTRIES = 7
        const val TOP_PLACES = 3
    }
}

/** 통계에 쓰는 일기 한 편의 값. 본문은 담지 않는다. */
data class StatsEntry(val date: LocalDate, val hour: Int, val mood: Mood?, val weather: Weather?, val place: String?)

fun advancedStats(rows: List<StatsEntry>): AdvancedStats {
    fun top(group: List<StatsEntry>): TopMood {
        val mood = group.mapNotNull { it.mood }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        return TopMood(mood, group.size)
    }
    return AdvancedStats(
        entries = rows.size,
        daysWritten = rows.map { it.date }.distinct().size,
        weekdays = rows.groupBy { it.date.dayOfWeek }.mapValues { top(it.value) },
        dayParts = rows.groupBy { dayPart(it.hour) }.mapValues { top(it.value) },
        weather = rows.filter { it.weather != null }.groupBy { it.weather!! }.mapValues { top(it.value) },
        places =
            rows
                .mapNotNull { row -> row.place?.trim()?.takeIf { it.isNotEmpty() } }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(AdvancedStats.TOP_PLACES)
                .map { it.key to it.value }
    )
}

private fun dayPart(hour: Int): DayPart = when (hour) {
    in 0..5 -> DayPart.DAWN
    in 6..11 -> DayPart.MORNING
    in 12..17 -> DayPart.AFTERNOON
    else -> DayPart.EVENING
}
