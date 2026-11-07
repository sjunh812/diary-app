package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class StatsPeriodTest {
    private val today = LocalDate(2026, 11, 4)

    @Test
    fun week_starts_on_sunday_like_the_calendar() {
        assertEquals(LocalDate(2026, 11, 1), StatsPeriod.Week.of(today).start)
        assertEquals(LocalDate(2026, 11, 7), StatsPeriod.Week.of(today).range.end)
        assertEquals(LocalDate(2026, 11, 1), StatsPeriod.Week.of(LocalDate(2026, 11, 1)).start)
        assertFailsWith<IllegalArgumentException> { StatsPeriod.Week(LocalDate(2026, 11, 2)) }
    }

    @Test
    fun periods_do_not_go_past_today() {
        assertNull(StatsPeriod.Week.of(today).next(today))
        assertNull(StatsPeriod.Month(YearMonth(2026, 11)).next(today))
        assertNull(StatsPeriod.Year(2026).next(today))
        assertEquals(StatsPeriod.Month(YearMonth(2026, 11)), StatsPeriod.Month(YearMonth(2026, 10)).next(today))
    }

    @Test
    fun months_cross_years() {
        assertEquals(StatsPeriod.Month(YearMonth(2025, 12)), StatsPeriod.Month(YearMonth(2026, 1)).previous())
        assertEquals(StatsPeriod.Month(YearMonth(2026, 1)), StatsPeriod.Month(YearMonth(2025, 12)).next(today))
    }

    @Test
    fun february_range_ends_on_its_last_day() {
        assertEquals(LocalDate(2028, 2, 29), StatsPeriod.Month(YearMonth(2028, 2)).range.end)
    }

    @Test
    fun whole_time_has_no_range_or_neighbors() {
        assertNull(StatsPeriod.All.range)
        assertNull(StatsPeriod.All.previous())
        assertNull(StatsPeriod.All.next(today))
    }
}
