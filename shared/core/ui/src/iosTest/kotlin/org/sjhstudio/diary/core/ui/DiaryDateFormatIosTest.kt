package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.Foundation.NSLocale

class DiaryDateFormatIosTest {
    // 2026-11-01은 일요일
    private val date = LocalDate(2026, 11, 1)
    private val time = LocalTime(15, 5)

    private fun format(tag: String, is24Hour: Boolean = false) = IosDateFormat(NSLocale(localeIdentifier = tag), is24Hour)

    // 오전·오후 앞뒤에 좁은 공백(U+202F)을 쓰는 버전이 있어 보통 공백으로 맞춰 비교
    // 기간의 띄어쓰기는 운영체제 버전마다 달라 빼고, 줄바꿈 자리(U+200B)도 빼고 비교
    private fun String.bare() = filterNot { it.isWhitespace() || it == '\u200B' || it == '\u2060' }

    private fun String.plain() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun korean() {
        val f = format("ko-KR")
        assertEquals("11월", f.month(date).plain())
        assertEquals("2026년 11월", f.yearMonth(date).plain())
        assertEquals("2026년 11월 1일", f.fullDate(date).plain())
        assertEquals("일", f.weekdayShort(date).plain())
        assertEquals("오후 3:05", f.time(time).plain())
    }

    @Test
    fun english() {
        val f = format("en-US")
        assertEquals("November", f.month(date).plain())
        assertEquals("November 2026", f.yearMonth(date).plain())
        assertEquals("November 1, 2026", f.fullDate(date).plain())
        assertEquals("Sun", f.weekdayShort(date).plain())
        assertEquals("3:05 PM", f.time(time).plain())
    }

    @Test
    fun japanese() {
        val f = format("ja-JP")
        assertEquals("11月", f.month(date).plain())
        assertEquals("2026年11月", f.yearMonth(date).plain())
        assertEquals("2026年11月1日", f.fullDate(date).plain())
        assertEquals("日", f.weekdayShort(date).plain())
        assertEquals("午後3:05", f.time(time).plain())
    }

    /** 기간은 겹치는 해·달을 한 번만, 해가 바뀌면 양쪽에 */
    @Test
    fun rangesWriteSharedPartsOnce() {
        val week = LocalDate(2026, 11, 22) to LocalDate(2026, 11, 28)
        val months = LocalDate(2026, 11, 29) to LocalDate(2026, 12, 5)
        val years = LocalDate(2025, 12, 28) to LocalDate(2026, 1, 3)
        fun DiaryDateFormat.ranges() = listOf(week, months, years).map { (start, end) ->
            dateRange(start, end).bare()
        }
        assertEquals(listOf("2026년11월22일~28일", "2026년11월29일~12월5일", "2025년12월28일~2026년1월3일"), format("ko-KR").ranges())
        assertEquals(
            listOf("November22–28,2026", "November29–December5,2026", "December28,2025–January3,2026"),
            format("en-US").ranges()
        )
        assertEquals(listOf("2026年11月22日～28日", "2026年11月29日～12月5日", "2025年12月28日～2026年1月3日"), format("ja-JP").ranges())
    }

    /** 올해 기간은 연도를 빼도 해가 바뀌는 기간은 양쪽에 연도 */
    @Test
    fun rangesWithoutYearKeepItAcrossYears() {
        fun DiaryDateFormat.range(start: LocalDate, end: LocalDate) = dateRange(start, end, withYear = false).bare()
        val ko = format("ko-KR")
        assertEquals("11월22일~28일", ko.range(LocalDate(2026, 11, 22), LocalDate(2026, 11, 28)))
        assertEquals("2025년12월28일~2026년1월3일", ko.range(LocalDate(2025, 12, 28), LocalDate(2026, 1, 3)))
        assertEquals("November29–December5", format("en-US").range(LocalDate(2026, 11, 29), LocalDate(2026, 12, 5)))
        assertEquals("11月22日～28日", format("ja-JP").range(LocalDate(2026, 11, 22), LocalDate(2026, 11, 28)))
    }

    @Test
    fun twentyFourHourFollowsDevice() {
        assertEquals("15:05", format("ko-KR", is24Hour = true).time(time))
        assertEquals("15:05", format("en-US", is24Hour = true).time(time))
        assertEquals("15:05", format("ja-JP", is24Hour = true).time(time))
    }
}
