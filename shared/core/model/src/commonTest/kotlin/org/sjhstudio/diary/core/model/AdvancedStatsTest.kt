package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class AdvancedStatsTest {
    private val start = LocalDate(2026, 1, 1)

    private fun row(
        day: Int,
        hour: Int = 21,
        mood: Mood? = Mood.GOOD,
        weather: Weather? = Weather.SUNNY,
        place: String? = null,
        photos: Int = 0,
        length: Int = 100
    ) = StatsEntry(start.plus(DatePeriod(days = day)), hour, mood, weather, place, photos, length)

    @Test
    fun cool_is_neutral_and_tired_is_hard() {
        assertEquals(MoodTone.NEUTRAL, Mood.COOL.tone)
        assertEquals(MoodTone.GOOD, Mood.LAUGH.tone)
        assertEquals(MoodTone.HARD, Mood.TIRED.tone)
        assertEquals(MoodTone.NEUTRAL, Mood.MEH.tone)
    }

    @Test
    fun share_rounds_to_nearest_percent() {
        assertEquals(67, ToneShare(2, 3).percent)
        assertEquals(0, ToneShare(0, 0).percent)
    }

    @Test
    fun small_groups_stay_hidden_from_the_table() {
        val stats = advancedStats(List(4) { row(it * 7) } + List(5) { row(it * 7 + 1) })

        assertEquals(listOf(start.plus(DatePeriod(days = 1)).dayOfWeek), stats.weekdays.keys.toList())
    }

    @Test
    fun entries_without_mood_do_not_count_as_bad_mood() {
        val stats = advancedStats(List(5) { row(it * 7, mood = Mood.GOOD) } + List(5) { row(it * 7, mood = null) })

        assertEquals(ToneShare(5, 5), stats.weekdays[start.dayOfWeek])
        assertEquals(10, stats.habit.entries)
    }

    @Test
    fun hours_fall_into_their_parts() {
        val rows = listOf(5, 6, 12, 18).flatMap { hour -> List(5) { row(it, hour = hour) } }

        assertEquals(setOf(DayPart.DAWN, DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING), advancedStats(rows).dayParts.keys)
        assertEquals(DayPart.EVENING, advancedStats(List(5) { row(it, hour = 23) }).habit.topPart)
    }

    @Test
    fun places_ignore_blanks_and_keep_the_top_ones() {
        val rows = List(6) { row(it, place = " 집 ") } + List(5) { row(it, place = "회사") } + List(5) { row(it, place = "  ") }

        assertEquals(listOf("집", "회사"), advancedStats(rows).places.map { it.first })
    }

    @Test
    fun longest_run_crosses_month_and_year_ends() {
        val days = listOf(LocalDate(2025, 12, 30), LocalDate(2025, 12, 31), LocalDate(2026, 1, 1), LocalDate(2026, 1, 3))
        val rows = days.map { StatsEntry(it, 9, null, null, null) } + StatsEntry(days[0], 10, null, null, null)

        val habit = advancedStats(rows).habit

        assertEquals(3, habit.longestRun)
        assertEquals(4, habit.daysWritten)
        assertEquals(5, habit.entries)
    }

    @Test
    fun longest_run_does_not_depend_on_week_start_or_leap_day() {
        val days = (0..3).map { LocalDate(2028, 2, 27).plus(DatePeriod(days = it)) }

        assertEquals(4, advancedStats(days.map { StatsEntry(it, 9, null, null, null) }).habit.longestRun)
    }

    @Test
    fun habit_adds_characters_and_photos() {
        val habit = advancedStats(listOf(row(1, photos = 2, length = 30), row(2, photos = 1, length = 12))).habit

        assertEquals(42, habit.characters)
        assertEquals(3, habit.photos)
    }

    @Test
    fun company_is_the_most_common_of_each() {
        val rows =
            listOf(
                row(0, mood = Mood.SAD, weather = Weather.RAIN, place = "집"),
                row(7, mood = Mood.SAD, weather = Weather.RAIN, place = "집"),
                row(1, mood = Mood.SAD, weather = Weather.SNOW, place = "회사"),
                row(1, mood = Mood.GOOD)
            )

        val sad = advancedStats(rows).company.single()

        assertEquals(MoodCompany(Mood.SAD, 3, Weather.RAIN, start.dayOfWeek, "집"), sad)
    }

    @Test
    fun change_needs_enough_entries_on_each_side() {
        val now = List(6) { row(it) }

        val stats = advancedStats(now, listOf(row(40, mood = Mood.SAD)))

        val change = assertNotNull(stats.change)
        assertNull(change.before.moods)
        assertEquals(ToneShare(6, 6), change.now.moods)
        assertEquals(1, change.before.days)
        assertEquals(6, change.now.days)
        assertNull(advancedStats(now).change)
    }

    @Test
    fun recent_period_compares_with_the_thirty_days_before() {
        val today = LocalDate(2026, 3, 31)

        assertEquals(DateRange(LocalDate(2026, 3, 2), today), AdvancedPeriod.RECENT.range(today))
        assertEquals(DateRange(LocalDate(2026, 1, 31), LocalDate(2026, 3, 1)), AdvancedPeriod.RECENT.compare(today))
        assertNull(AdvancedPeriod.ALL.compare(today))
    }

    @Test
    fun year_period_stops_at_the_same_day_last_year_even_on_leap_day() {
        val today = LocalDate(2028, 2, 29)

        assertEquals(DateRange(LocalDate(2027, 1, 1), LocalDate(2027, 2, 28)), AdvancedPeriod.THIS_YEAR.compare(today))
    }

    @Test
    fun no_moods_means_no_mood_talk() {
        val stats = advancedStats(List(50) { row(it, mood = null) })

        assertTrue(stats.enough)
        assertFalse(stats.moodReady)
        assertTrue(stats.weekdays.isEmpty() && stats.company.isEmpty())
        assertEquals(50, stats.habit.daysWritten)
    }

    @Test
    fun nothing_before_means_nothing_to_compare() {
        assertNull(advancedStats(List(10) { row(it) }, emptyList()).change)
    }

    @Test
    fun many_entries_on_one_day_count_as_one_day() {
        val habit = advancedStats(List(30) { row(0) }).habit

        assertEquals(1, habit.daysWritten)
        assertEquals(1, habit.longestRun)
        assertEquals(30, habit.entries)
    }

    /**
     * 일기 앞뒤로 기분이 이어지는 경향까지 넣은 가짜 기록.
     * 조건과 기분 사이에 관계를 일부러 넣을 수 있다.
     */

    private val weatherPool =
        List(9) { Weather.SUNNY } + List(5) { Weather.PARTLY_CLOUDY } + List(3) { Weather.RAIN } + Weather.SNOW + List(2) { Weather.CLOUDY }
    private val placePool = listOf("집", "집", "집", "회사", "회사", "카페", "공원", null, null, null)
    private val hourPool = listOf(3, 9, 9, 14, 14, 20, 20, 20, 21, 22)

    @Test
    fun weekday_table_uses_real_weekdays() {
        val stats = advancedStats(List(5) { row(it * 7 + 3) })

        assertEquals(setOf(DayOfWeek.SUNDAY), stats.weekdays.keys)
    }

    private companion object {
        const val TRIALS = 300
    }

    private val today = LocalDate(2026, 11, 6)

    private fun dated(daysAgo: Int, mood: Mood) = StatsEntry(today.minus(DatePeriod(days = daysAgo)), 21, mood, null, null)

    private fun window(from: Int, good: Int, hard: Int) =
        List(good) { dated(from + it, Mood.GOOD) } + List(hard) { dated(from + good + it, Mood.SAD) }

    @Test
    fun recent_mood_needs_enough_entries_on_both_sides() {
        assertNull(recentMood(window(0, 5, 2) + window(30, 5, 5), today))
        assertNull(recentMood(window(0, 5, 5) + window(30, 5, 2), today))
    }

    @Test
    fun more_good_moods_read_as_better() {
        val recent = assertNotNull(recentMood(window(0, 8, 2) + window(30, 4, 6), today))
        assertEquals(MoodTrend.BETTER, recent.trend)
        assertEquals(40, recent.before.percent)
        assertEquals(80, recent.now.percent)
    }

    @Test
    fun more_hard_moods_read_as_harder() {
        assertEquals(MoodTrend.HARDER, recentMood(window(0, 3, 7) + window(30, 7, 3), today)?.trend)
    }

    @Test
    fun small_shift_stays_steady() {
        assertEquals(MoodTrend.STEADY, recentMood(window(0, 5, 5) + window(30, 5, 5), today)?.trend)
    }

    @Test
    fun hard_moods_going_down_are_named_when_they_moved_most() {
        val meh = List(5) { dated(5 + it, Mood.MEH) }
        val recent = assertNotNull(recentMood(window(0, 4, 1) + meh + window(30, 4, 6), today))
        assertEquals(MoodTrend.BETTER, recent.trend)
        assertTrue(recent.hardMoved)
    }

    @Test
    fun hard_share_counts_only_hard_moods() {
        val rows = listOf(Mood.GOOD, Mood.SAD, Mood.TIRED, Mood.MEH).mapIndexed { i, mood -> dated(i, mood) }
        val share = assertNotNull(advancedStats(rows + rows, rows + rows).change?.now?.moods)
        assertEquals(25, share.percent)
        assertEquals(50, share.hardPercent)
    }

    @Test
    fun change_carries_everything_it_compares() {
        val before = listOf(row(0, length = 10, photos = 1), row(0, length = 20), row(1, length = 5))
        val now = List(6) { row(it, mood = Mood.SAD, length = 100, photos = 2) }
        val change = assertNotNull(advancedStats(now, before).change)
        assertEquals(StatsSnapshot(null, null, 2, 3, 35, 1), change.before)
        assertEquals(6, change.now.days)
        assertEquals(600, change.now.characters)
        assertEquals(12, change.now.photos)
        assertEquals(Mood.SAD, change.now.topMood)
    }

    @Test
    fun counts_below_a_hundred_thousand_stay_full() {
        assertNull(compactCount(99_999, 10_000, 100_000_000))
    }

    @Test
    fun korean_units_are_man_and_eok() {
        assertEquals(CompactCount("10", false), compactCount(100_000, 10_000, 100_000_000))
        assertEquals(CompactCount("123", false), compactCount(1_239_999, 10_000, 100_000_000))
        assertEquals(CompactCount("9999", false), compactCount(99_999_999, 10_000, 100_000_000))
        assertEquals(CompactCount("1", true), compactCount(100_000_000, 10_000, 100_000_000))
        assertEquals(CompactCount("1.2", true), compactCount(123_000_000, 10_000, 100_000_000))
    }

    @Test
    fun english_units_are_thousands_and_millions() {
        assertEquals(CompactCount("100", false), compactCount(100_000, 1_000, 1_000_000))
        assertEquals(CompactCount("2.5", true), compactCount(2_500_000, 1_000, 1_000_000))
    }
}
