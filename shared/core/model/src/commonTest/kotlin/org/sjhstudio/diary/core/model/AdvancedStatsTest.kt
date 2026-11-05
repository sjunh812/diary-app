package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class AdvancedStatsTest {
    private fun row(day: Int, hour: Int = 21, mood: Mood? = Mood.GOOD, weather: Weather? = null, place: String? = null) =
        StatsEntry(LocalDate(2026, 11, day), hour, mood, weather, place)

    @Test
    fun counts_days_not_entries_for_days_written() {
        val stats = advancedStats(listOf(row(1), row(1), row(2)))

        assertEquals(3, stats.entries)
        assertEquals(2, stats.daysWritten)
    }

    @Test
    fun finds_the_most_common_mood_of_a_weekday() {
        // 2026-11-01은 일요일이다.
        val stats = advancedStats(listOf(row(1, mood = Mood.SAD), row(8, mood = Mood.GOOD), row(15, mood = Mood.GOOD)))

        assertEquals(Mood.GOOD, stats.weekdays[DayOfWeek.SUNDAY]?.mood)
        assertEquals(3, stats.weekdays[DayOfWeek.SUNDAY]?.entries)
    }

    @Test
    fun entries_without_mood_still_count_but_have_no_top() {
        val stats = advancedStats(listOf(row(1, mood = null)))

        assertNull(stats.weekdays[DayOfWeek.SUNDAY]?.mood)
        assertEquals(1, stats.weekdays[DayOfWeek.SUNDAY]?.entries)
    }

    @Test
    fun splits_the_day_at_the_edges() {
        val stats = advancedStats(listOf(row(1, hour = 5), row(2, hour = 6), row(3, hour = 12), row(4, hour = 18), row(5, hour = 23)))

        assertEquals(1, stats.dayParts[DayPart.DAWN]?.entries)
        assertEquals(1, stats.dayParts[DayPart.MORNING]?.entries)
        assertEquals(1, stats.dayParts[DayPart.AFTERNOON]?.entries)
        assertEquals(2, stats.dayParts[DayPart.EVENING]?.entries)
    }

    @Test
    fun ranks_places_and_ignores_blanks() {
        val rows = listOf("집", "집", "회사", " ", null, "카페", "공원").mapIndexed { index, place -> row(index + 1, place = place) }

        val places = advancedStats(rows).places

        assertEquals("집" to 2, places.first())
        assertEquals(3, places.size)
    }

    @Test
    fun needs_a_week_of_entries() {
        assertFalse(advancedStats(List(6) { row(it + 1) }).enough)
        assertTrue(advancedStats(List(7) { row(it + 1) }).enough)
    }
}
