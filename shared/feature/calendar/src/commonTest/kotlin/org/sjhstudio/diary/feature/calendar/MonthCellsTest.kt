package org.sjhstudio.diary.feature.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import kotlinx.datetime.yearMonth

// 달력은 어느 달이든 6주를 그려 달을 넘겨도 높이가 같음(docs/design.md 달력)
class MonthCellsTest {
    @Test
    fun every_month_has_six_weeks_from_sunday() {
        for (month in listOf(YearMonth(2026, 2), YearMonth(2026, 8), YearMonth(2026, 9), YearMonth(2027, 1))) {
            val cells = monthCells(month)
            assertEquals(42, cells.size)
            assertEquals(DayOfWeek.SUNDAY, cells.first().dayOfWeek)
            assertEquals((1..month.numberOfDays).map { month.onDay(it) }, cells.filter { it.yearMonth == month })
        }
    }

    @Test
    fun empty_cells_are_the_days_around_the_month() {
        // 2026년 8월 1일은 토요일. 앞 칸은 7월 26일부터, 뒤 칸은 9월 5일까지
        val cells = monthCells(YearMonth(2026, 8))
        assertEquals(LocalDate(2026, 7, 26), cells.first())
        assertEquals(LocalDate(2026, 9, 5), cells.last())
        assertTrue(cells.zipWithNext().all { (a, b) -> b.toEpochDays() - a.toEpochDays() == 1L })
    }
}
