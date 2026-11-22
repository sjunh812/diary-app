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

    /** 첫날이 든 기간까지. 사이 빈 기간도 넘김 */
    @Test
    fun periods_do_not_go_before_the_first_day() {
        val first = LocalDate(2026, 3, 1)
        assertNull(StatsPeriod.Month(YearMonth(2026, 3)).previous(first))
        assertEquals(StatsPeriod.Month(YearMonth(2026, 3)), StatsPeriod.Month(YearMonth(2026, 4)).previous(first))
        assertNull(StatsPeriod.Year(2026).previous(first))
        assertEquals(StatsPeriod.Year(2026), StatsPeriod.Year(2027).previous(first))
        // 2026-03-01은 일요일이라 그 주가 첫 주
        assertNull(StatsPeriod.Week(first).previous(first))
        assertEquals(StatsPeriod.Week(first), StatsPeriod.Week(LocalDate(2026, 3, 8)).previous(first))
    }

    /** 첫날이 주 가운데면 그날이 든 주까지 */
    @Test
    fun the_first_week_holds_the_first_day() {
        val first = LocalDate(2026, 4, 1)
        assertEquals(StatsPeriod.Week(LocalDate(2026, 3, 29)), StatsPeriod.Week(LocalDate(2026, 4, 5)).previous(first))
        assertNull(StatsPeriod.Week(LocalDate(2026, 3, 29)).previous(first))
    }

    /** 첫날보다 이른 기간을 보던 중이면 앞으로 가지 않음 */
    @Test
    fun a_period_before_the_first_day_has_no_previous() {
        val first = LocalDate(2026, 3, 1)
        assertNull(StatsPeriod.Month(YearMonth(2025, 1)).previous(first))
        assertNull(StatsPeriod.Year(2024).previous(first))
        assertNull(StatsPeriod.Week(LocalDate(2025, 1, 5)).previous(first))
    }

    @Test
    fun months_cross_years() {
        assertEquals(StatsPeriod.Month(YearMonth(2025, 12)), StatsPeriod.Month(YearMonth(2026, 1)).previous(LocalDate(2025, 1, 1)))
        assertEquals(StatsPeriod.Month(YearMonth(2026, 1)), StatsPeriod.Month(YearMonth(2025, 12)).next(today))
    }

    @Test
    fun february_range_ends_on_its_last_day() {
        assertEquals(LocalDate(2028, 2, 29), StatsPeriod.Month(YearMonth(2028, 2)).range.end)
    }

    @Test
    fun whole_time_has_no_range_or_neighbors() {
        assertNull(StatsPeriod.All.range)
        assertNull(StatsPeriod.All.previous(today))
        assertNull(StatsPeriod.All.next(today))
    }
}
