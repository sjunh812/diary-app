package org.sjhstudio.diary.core.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus

/** 돌아보기 기간(FR-028). 이전·다음은 같은 종류로 한 칸씩이고 오늘이 든 기간 뒤로는 가지 않는다. */
sealed interface StatsPeriod {
    /** 전체면 null이다. */
    val range: DateRange?

    fun previous(): StatsPeriod?

    /** 오늘이 든 기간이면 null이다. */
    fun next(today: LocalDate): StatsPeriod?

    /** 주는 월요일에 시작한다(달력과 같다). */
    data class Week(val start: LocalDate) : StatsPeriod {
        init {
            require(start.dayOfWeek == DayOfWeek.MONDAY) { "주는 월요일에 시작한다" }
        }

        override val range: DateRange get() = DateRange(start, start.plus(DatePeriod(days = 6)))

        override fun previous(): StatsPeriod = Week(start.minus(DatePeriod(days = 7)))

        override fun next(today: LocalDate): StatsPeriod? = if (today <= range.end) null else Week(start.plus(DatePeriod(days = 7)))

        companion object {
            fun of(date: LocalDate): Week = Week(date.minus(DatePeriod(days = date.dayOfWeek.ordinal)))
        }
    }

    data class Month(val month: YearMonth) : StatsPeriod {
        override val range: DateRange get() = DateRange(month.onDay(1), month.onDay(month.numberOfDays))

        override fun previous(): StatsPeriod = Month(
            if (month.month.number ==
                1
            ) {
                YearMonth(month.year - 1, 12)
            } else {
                YearMonth(month.year, month.month.number - 1)
            }
        )

        override fun next(today: LocalDate): StatsPeriod? {
            if (today <= range.end) return null
            return Month(if (month.month.number == 12) YearMonth(month.year + 1, 1) else YearMonth(month.year, month.month.number + 1))
        }
    }

    data class Year(val year: Int) : StatsPeriod {
        override val range: DateRange get() = DateRange(LocalDate(year, 1, 1), LocalDate(year, 12, 31))

        override fun previous(): StatsPeriod = Year(year - 1)

        override fun next(today: LocalDate): StatsPeriod? = if (today.year <= year) null else Year(year + 1)
    }

    data object All : StatsPeriod {
        override val range: DateRange? = null

        override fun previous(): StatsPeriod? = null

        override fun next(today: LocalDate): StatsPeriod? = null
    }
}
