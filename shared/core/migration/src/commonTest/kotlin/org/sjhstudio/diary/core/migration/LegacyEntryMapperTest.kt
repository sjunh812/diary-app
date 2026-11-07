package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

// contracts/legacy-mapping.md "일기 줄" 표(스펙 001 T033).
class LegacyEntryMapperTest {
    private val seoul = TimeZone.of("Asia/Seoul")
    private val mapper = LegacyEntryMapper(migratedOn = LocalDate(2026, 11, 1), timeZone = seoul, newId = { "new-id" })

    private fun note(
        createDate: String? = "2024-03-01 21:30:00",
        modifyDate: String? = "2024-03-01 21:30:00",
        contents: String? = "오늘",
        mood: Long? = -1,
        weather: Long? = -1,
        address: String? = "",
        star: Long? = 0,
        locationX: String? = "",
        locationY: String? = ""
    ) = LegacyNote(7, createDate, modifyDate, contents, mood, weather, address, star, locationX, locationY, picture = "")

    @Test
    fun date_and_time_come_from_the_first_sixteen_characters() {
        val mapped = mapper.map(note(createDate = "2024-03-01 10:00:05"))
        assertEquals(LocalDate(2024, 3, 1), mapped.entry.date)
        assertEquals(LocalTime(10, 0), mapped.entry.time)
        assertEquals(LocalDate(2024, 3, 1).atTime(10, 0).toInstant(seoul), mapped.entry.createdAt)
        assertFalse(mapped.dateUnparsed)
        assertFalse(mapped.dateDiffers)
        assertEquals("2024-03-01 10:00:05", mapped.entry.legacyCreateDate)
        assertEquals(7, mapped.entry.legacyId)
    }

    @Test
    fun milliseconds_in_the_seconds_field_keep_the_chosen_day_but_count_as_differs() {
        val mapped = mapper.map(note(createDate = "2024-03-01 23:59:457"))
        assertEquals(LocalDate(2024, 3, 1), mapped.entry.date)
        assertEquals(LocalTime(23, 59), mapped.entry.time)
        assertTrue(mapped.dateDiffers)
        assertFalse(mapped.dateUnparsed)
    }

    @Test
    fun unreadable_date_falls_back_to_modify_date_then_to_migration_day() {
        val fromModify = mapper.map(note(createDate = "어제 밤", modifyDate = "2024-05-05 08:00:00"))
        assertEquals(LocalDate(2024, 5, 5), fromModify.entry.date)
        assertEquals(LocalTime(0, 0), fromModify.entry.time)
        assertTrue(fromModify.dateUnparsed)

        val fromToday = mapper.map(note(createDate = null, modifyDate = null))
        assertEquals(LocalDate(2026, 11, 1), fromToday.entry.date)
        assertTrue(fromToday.dateUnparsed)

        val impossible = mapper.map(note(createDate = "2024-02-30 10:00:00", modifyDate = null))
        assertEquals(LocalDate(2026, 11, 1), impossible.entry.date)
        assertTrue(impossible.dateUnparsed)
    }

    @Test
    fun bad_time_becomes_midnight_and_counts_as_unparsed() {
        val mapped = mapper.map(note(createDate = "2024-03-01 25:61:00"))
        assertEquals(LocalTime(0, 0), mapped.entry.time)
        assertTrue(mapped.dateUnparsed)
    }

    @Test
    fun modify_date_sets_updated_at_or_falls_back_to_created_at() {
        val edited = mapper.map(note(modifyDate = "2024-03-02 09:15:00"))
        assertEquals(LocalDate(2024, 3, 2).atTime(9, 15).toInstant(seoul), edited.entry.updatedAt)
        val broken = mapper.map(note(modifyDate = "모름"))
        assertEquals(broken.entry.createdAt, broken.entry.updatedAt)
    }

    @Test
    fun mood_and_weather_out_of_range_become_null() {
        assertEquals(Mood.ANGRY, mapper.map(note(mood = 0)).entry.mood)
        assertEquals(Mood.TIRED, mapper.map(note(mood = 8)).entry.mood)
        assertNull(mapper.map(note(mood = 9)).entry.mood)
        assertNull(mapper.map(note(mood = null)).entry.mood)
        assertEquals(Weather.SUNNY, mapper.map(note(weather = 0)).entry.weather)
        assertEquals(Weather.SNOW, mapper.map(note(weather = 6)).entry.weather)
        assertNull(mapper.map(note(weather = 7)).entry.weather)
        assertNull(mapper.map(note(weather = -1)).entry.weather)
    }

    @Test
    fun place_star_location_and_contents() {
        assertNull(mapper.map(note(address = "   ")).entry.placeName)
        assertEquals(" 서울 마포구 ", mapper.map(note(address = " 서울 마포구 ")).entry.placeName)
        assertTrue(mapper.map(note(star = 1)).entry.isFavorite)
        assertFalse(mapper.map(note(star = null)).entry.isFavorite)
        assertFalse(mapper.map(note(star = 2)).entry.isFavorite)
        assertNull(mapper.map(note(locationX = "", locationY = null)).entry.legacyLocation)
        assertEquals("37.5|", mapper.map(note(locationX = "37.5", locationY = null)).entry.legacyLocation)
        val empty = mapper.map(note(contents = null)).entry
        assertEquals("", empty.plainText)
        assertEquals("", empty.bodyHtml)
    }
}
