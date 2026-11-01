package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.Foundation.NSLocale

class DiaryDateFormatIosTest {
    // 2026-11-01은 일요일이다.
    private val date = LocalDate(2026, 11, 1)
    private val time = LocalTime(15, 5)

    private fun format(tag: String, is24Hour: Boolean = false) = IosDateFormat(NSLocale(localeIdentifier = tag), is24Hour)

    // 오전·오후 앞뒤에 좁은 공백(U+202F)을 쓰는 판이 있어 보통 공백으로 맞춰 견준다.
    private fun String.plain() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun korean() {
        val f = format("ko-KR")
        assertEquals("11월", f.month(date))
        assertEquals("2026년 11월", f.yearMonth(date))
        assertEquals("2026년 11월 1일", f.fullDate(date))
        assertEquals("일", f.weekdayShort(date))
        assertEquals("오후 3:05", f.time(time).plain())
    }

    @Test
    fun english() {
        val f = format("en-US")
        assertEquals("November", f.month(date))
        assertEquals("November 2026", f.yearMonth(date))
        assertEquals("November 1, 2026", f.fullDate(date))
        assertEquals("Sun", f.weekdayShort(date))
        assertEquals("3:05 PM", f.time(time).plain())
    }

    @Test
    fun japanese() {
        val f = format("ja-JP")
        assertEquals("11月", f.month(date))
        assertEquals("2026年11月", f.yearMonth(date))
        assertEquals("2026年11月1日", f.fullDate(date))
        assertEquals("日", f.weekdayShort(date))
        assertEquals("午後3:05", f.time(time).plain())
    }

    @Test
    fun twentyFourHourFollowsDevice() {
        assertEquals("15:05", format("ko-KR", is24Hour = true).time(time))
        assertEquals("15:05", format("en-US", is24Hour = true).time(time))
        assertEquals("15:05", format("ja-JP", is24Hour = true).time(time))
    }
}
