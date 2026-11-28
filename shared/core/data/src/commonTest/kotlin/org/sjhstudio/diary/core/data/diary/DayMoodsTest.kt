package org.sjhstudio.diary.core.data.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.database.DayMoodRow
import org.sjhstudio.diary.core.model.Mood

class DayMoodsTest {
    private val day = LocalDate(2026, 11, 28)

    private fun of(vararg moods: String?) = toDayMoods(moods.map { DayMoodRow(day.toString(), it) }).getValue(day)

    @Test
    fun mood_comes_first_when_the_last_entry_has_none() {
        val d = of("good", null)
        assertEquals(Mood.GOOD, d.mood)
        assertNull(d.previousMood)
        assertEquals(2, d.count)
    }

    @Test
    fun back_is_the_latest_other_mood() {
        val d = of("sad", null, "angry", null)
        assertEquals(Mood.ANGRY, d.mood)
        assertEquals(Mood.SAD, d.previousMood)
    }

    @Test
    fun one_mood_with_only_text_entries_shows_one_face() {
        assertNull(of(null, "good", null).previousMood)
    }

    @Test
    fun day_without_moods_keeps_no_face() {
        val d = of(null, null)
        assertNull(d.mood)
        assertNull(d.previousMood)
    }

    @Test
    fun unknown_mood_id_counts_as_no_mood() {
        assertEquals(Mood.GOOD, of("good", "from-a-newer-app").mood)
    }
}
