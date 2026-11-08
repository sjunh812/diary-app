package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

// 공유한 글이 일기처럼 읽힌다. 날짜와 시각, 기분·날씨·장소, 빈 줄, 본문 차례다.
class EntryShareTest {
    private val dates =
        object : DiaryDateFormat {
            override fun month(date: LocalDate) = ""
            override fun yearMonth(date: LocalDate) = ""
            override fun monthShort(date: LocalDate) = ""
            override fun year(date: LocalDate) = ""
            override fun monthDay(date: LocalDate) = ""
            override fun fullDate(date: LocalDate) = "2026년 11월 4일"
            override fun dayWithWeekday(date: LocalDate) = ""
            override fun weekday(date: LocalDate) = "수요일"
            override fun weekdayShort(date: LocalDate) = ""
            override fun time(time: LocalTime) = "오후 9:09"
            override val is24Hour = false
        }
    private val moods = Mood.entries.associateWith { it.name }
    private val weathers = Weather.entries.associateWith { it.name }

    private fun entry(mood: Mood? = null, weather: Weather? = null, place: String? = null, text: String = "회의가 많았다.") = Entry(
        id = "a",
        date = LocalDate(2026, 11, 4),
        time = LocalTime(21, 9),
        bodyHtml = "",
        plainText = text,
        mood = mood,
        weather = weather,
        placeName = place,
        isFavorite = false,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0)
    )

    private fun share(entry: Entry) = entryShareText(entry, dates, moods, weathers, "%1\$s %2\$s %3\$s")

    @Test
    fun everything_reads_like_a_diary() = assertEquals(
        "2026년 11월 4일 수요일 오후 9:09\n${Mood.entries.first().name} · ${Weather.entries.first().name} · 망원동\n\n회의가 많았다.",
        share(entry(Mood.entries.first(), Weather.entries.first(), "망원동"))
    )

    @Test
    fun missing_details_leave_no_empty_line() = assertEquals("2026년 11월 4일 수요일 오후 9:09\n\n회의가 많았다.", share(entry()))

    @Test
    fun only_a_place_still_shows() = assertEquals("2026년 11월 4일 수요일 오후 9:09\n망원동\n\n회의가 많았다.", share(entry(place = "망원동")))

    @Test
    fun an_empty_body_ends_after_the_details() = assertEquals(
        "2026년 11월 4일 수요일 오후 9:09\n${Mood.entries.first().name}",
        share(entry(Mood.entries.first(), text = "  "))
    )
}
