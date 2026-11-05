package org.sjhstudio.diary.core.model

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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
        assertEquals(67, GoodShare(2, 3).percent)
        assertEquals(0, GoodShare(0, 0).percent)
    }

    @Test
    fun small_groups_stay_hidden_from_the_table() {
        val stats = advancedStats(List(4) { row(it * 7) } + List(5) { row(it * 7 + 1) })

        assertEquals(listOf(start.plus(DatePeriod(days = 1)).dayOfWeek), stats.weekdays.keys.toList())
    }

    @Test
    fun entries_without_mood_do_not_count_as_bad_mood() {
        val stats = advancedStats(List(5) { row(it * 7, mood = Mood.GOOD) } + List(5) { row(it * 7, mood = null) })

        assertEquals(GoodShare(5, 5), stats.weekdays[start.dayOfWeek])
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
        assertNull(change.goodBefore)
        assertEquals(GoodShare(6, 6), change.goodNow)
        assertEquals(1, change.daysBefore)
        assertEquals(6, change.daysNow)
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
    fun clear_gap_is_found_with_its_numbers() {
        val rainy = List(30) { row(it, weather = Weather.RAIN, mood = if (it < 6) Mood.GOOD else Mood.SAD) }
        val others = List(30) { row(it, weather = Weather.SUNNY, mood = if (it < 22) Mood.GOOD else Mood.SAD) }

        val gaps = advancedStats(rainy + others).gaps

        val gap = gaps.first { it.factor == StatsFactor.Sky(Weather.RAIN) }
        assertFalse(gap.more)
        assertEquals(GoodShare(6, 30), gap.here)
        assertEquals(GoodShare(22, 30), gap.others)
    }

    @Test
    fun small_groups_never_make_a_gap() {
        val rows = List(7) { row(it, weather = Weather.RAIN, mood = Mood.SAD) } + List(40) { row(it, weather = Weather.SUNNY) }

        assertTrue(advancedStats(rows).gaps.none { it.factor == StatsFactor.Sky(Weather.RAIN) })
    }

    @Test
    fun at_most_three_gaps_biggest_first() {
        val rows = weatherRows()

        val gaps = advancedStats(rows).gaps

        assertTrue(gaps.size <= AdvancedStats.MAX_GAPS)
        assertEquals(gaps.sortedByDescending { abs(it.here.rate - it.others.rate) }, gaps)
    }

    private fun weatherRows() = Weather.entries.flatMapIndexed { index, weather ->
        List(40) { row(it, weather = weather, mood = if (it < index * 6) Mood.GOOD else Mood.SAD) }
    }

    @Test
    fun quantile_matches_known_values() {
        assertTrue(abs(normalQuantile(0.025) - 1.96) < 0.01)
        assertTrue(abs(normalQuantile(0.00125) - 3.02) < 0.01)
    }

    /** 일기 앞뒤로 기분이 이어지는 경향까지 넣은 가짜 기록. 조건과 기분 사이에 관계를 일부러 넣을 수 있다. */
    private fun fake(random: Random, size: Int, rainEffect: Double): List<StatsEntry> {
        val base = listOf(0.3, 0.45, 0.6).random(random)
        var previous = random.nextDouble() < base
        return List(size) { day ->
            val weather = weatherPool.random(random)
            val place = placePool.random(random)
            val chance = if (weather == Weather.RAIN) base + rainEffect else base
            val good = if (random.nextDouble() < 0.3) previous else random.nextDouble() < chance
            previous = good
            StatsEntry(
                start.plus(DatePeriod(days = day)),
                hourPool.random(random),
                if (good) Mood.GOOD else Mood.SAD,
                weather,
                place,
                if (random.nextDouble() < 0.3) 1 else 0,
                random.nextInt(20, 400)
            )
        }
    }

    private val weatherPool =
        List(9) { Weather.SUNNY } + List(5) { Weather.PARTLY_CLOUDY } + List(3) { Weather.RAIN } + Weather.SNOW + List(2) { Weather.CLOUDY }
    private val placePool = listOf("집", "집", "집", "회사", "회사", "카페", "공원", null, null, null)
    private val hourPool = listOf(3, 9, 9, 14, 14, 20, 20, 20, 21, 22)

    @Test
    fun unrelated_diaries_almost_never_show_a_gap() {
        for (size in listOf(30, 90, 365, 1000)) {
            val wrong = (0 until TRIALS).count { seed -> advancedStats(fake(Random(seed * 31 + size), size, 0.0)).gaps.isNotEmpty() }

            assertTrue(wrong <= TRIALS * 7 / 100, "일기 ${size}편에서 $TRIALS 번 중 $wrong 번 잘못 말함")
        }
    }

    @Test
    fun strong_relation_shows_in_a_long_diary() {
        val rain = StatsFactor.Sky(Weather.RAIN)
        val found =
            (0 until TRIALS).count { seed ->
                advancedStats(fake(Random(seed), 1000, -0.3)).gaps.any { it.factor == rain && !it.more }
            }

        assertTrue(found >= TRIALS * 7 / 10, "$TRIALS 번 중 $found 번 찾음")
    }

    @Test
    fun weekday_table_uses_real_weekdays() {
        val stats = advancedStats(List(5) { row(it * 7 + 3) })

        assertEquals(setOf(DayOfWeek.SUNDAY), stats.weekdays.keys)
    }

    private companion object {
        const val TRIALS = 300
    }
}
