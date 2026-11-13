package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number

class DiaryDateFormatTest {
    @Test
    fun deviceRegionIsKeptWhenLanguageMatches() {
        assertEquals("en-GB", formatLocaleTag("en", "en-GB"))
        assertEquals("ko-KR", formatLocaleTag("ko", "ko_KR"))
        assertEquals("ja-JP", formatLocaleTag("ja", "ja_JP@calendar=japanese"))
    }

    @Test
    fun screenLanguageWinsWhenDeviceLanguageIsNotSupported() {
        // 프랑스어 기기는 영어 화면이 보이고 날짜도 영어
        assertEquals("en", formatLocaleTag("en", "fr-FR"))
        assertEquals("en", formatLocaleTag("en", "zh-Hant-TW"))
    }

    // 문장 안에서 "11월 / 12일"처럼 끊기지 않게 함
    @Test
    fun dateKeepsItsPartsOnOneLine() {
        assertEquals("11월 12일 오후 7:48", "11월 12일 오후 7:48".keepTogether())
    }

    private object Labels : DiaryDateFormat {
        override fun month(date: LocalDate) = "${date.month.number}월"

        override fun yearMonth(date: LocalDate) = "${date.year}년 ${date.month.number}월"

        override fun monthShort(date: LocalDate) = month(date)

        override fun year(date: LocalDate) = "${date.year}년"

        override fun monthDay(date: LocalDate) = month(date)

        override fun fullDate(date: LocalDate) = yearMonth(date)

        override fun dayWithWeekday(date: LocalDate) = month(date)

        override fun weekday(date: LocalDate) = ""

        override fun weekdayShort(date: LocalDate) = ""

        override fun time(time: LocalTime) = ""

        override val is24Hour = false
    }

    @Test
    fun aMonthOfThisYearShowsOnlyTheMonth() {
        assertEquals("10월", Labels.monthHeader(LocalDate(2026, 10, 3), 2026, LocalDate(2026, 11, 1)))
        assertEquals("10월", Labels.monthHeader(LocalDate(2026, 10, 3), 2026, previous = null))
    }

    @Test
    fun anotherYearShowsTheYear() {
        assertEquals("2025년 12월", Labels.monthHeader(LocalDate(2025, 12, 30), 2026, LocalDate(2026, 1, 2)))
    }

    // 내년 날짜로 쓴 일기 밑에 올해 일기가 이어지면 어느 해인지 적음
    @Test
    fun thisYearAfterNextYearShowsTheYear() {
        assertEquals("2026년 12월", Labels.monthHeader(LocalDate(2026, 12, 31), 2026, LocalDate(2027, 1, 1)))
    }
}
