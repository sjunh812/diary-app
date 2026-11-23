package org.sjhstudio.diary.feature.insight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.ui.DiaryDateFormat

// 돌아보기 기간 이름. 주는 올해면 연도를 빼고 지난해 주는 붙임
class InsightPeriodLabelTest {
    private val today = LocalDate(2026, 1, 10)

    @Test
    fun a_week_this_year_drops_the_year() {
        assertEquals("2026-01-04~2026-01-10 연도 없음", periodLabel(StatsPeriod.Week(LocalDate(2026, 1, 4)), Labels, today))
    }

    @Test
    fun a_week_from_last_year_keeps_the_year() {
        assertEquals("2025-11-30~2025-12-06 연도", periodLabel(StatsPeriod.Week(LocalDate(2025, 11, 30)), Labels, today))
    }

    @Test
    fun a_week_across_the_new_year_keeps_the_year() {
        assertEquals("2025-12-28~2026-01-03 연도", periodLabel(StatsPeriod.Week(LocalDate(2025, 12, 28)), Labels, today))
    }

    private object Labels : DiaryDateFormat {
        override fun dateRange(start: LocalDate, end: LocalDate, withYear: Boolean) = "$start~$end ${if (withYear) "연도" else "연도 없음"}"

        override fun month(date: LocalDate) = error("쓰지 않음")

        override fun yearMonth(date: LocalDate) = error("쓰지 않음")

        override fun monthShort(date: LocalDate) = error("쓰지 않음")

        override fun year(date: LocalDate) = error("쓰지 않음")

        override fun monthDay(date: LocalDate) = error("쓰지 않음")

        override fun fullDate(date: LocalDate) = error("쓰지 않음")

        override fun dayWithWeekday(date: LocalDate) = error("쓰지 않음")

        override fun weekday(date: LocalDate) = error("쓰지 않음")

        override fun weekdayShort(date: LocalDate) = error("쓰지 않음")

        override fun time(time: LocalTime) = error("쓰지 않음")

        override val is24Hour = false
    }
}
