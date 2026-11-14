package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.model.EntryRow

class EntryMarksTest {
    private fun row(date: LocalDate) = EntryRow(
        id = date.toString(),
        date = date,
        time = LocalTime(9, 0),
        preview = "",
        mood = null,
        weather = null,
        placeName = null,
        isFavorite = false,
        photos = emptyList()
    )

    @Test
    fun the_first_row_starts_a_month_and_a_day() {
        assertEquals(EntryMarks(newMonth = true, newDay = true), entryMarks(0, row(LocalDate(2026, 11, 3)), previous = null))
    }

    @Test
    fun the_same_day_draws_nothing_and_a_new_month_draws_both() {
        val today = row(LocalDate(2026, 11, 3))
        assertEquals(EntryMarks(newMonth = false, newDay = false), entryMarks(1, today, today))
        assertEquals(EntryMarks(newMonth = true, newDay = true), entryMarks(1, row(LocalDate(2026, 10, 31)), today))
    }

    /** 앞 줄을 아직 못 읽었으면 그렸다 지우지 않게 기다림 */
    @Test
    fun an_unread_previous_row_draws_nothing_yet() {
        assertEquals(EntryMarks(newMonth = false, newDay = false), entryMarks(5, row(LocalDate(2026, 11, 3)), previous = null))
    }
}
