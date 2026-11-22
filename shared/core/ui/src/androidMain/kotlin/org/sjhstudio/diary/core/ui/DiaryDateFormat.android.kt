package org.sjhstudio.diary.core.ui

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
internal actual fun rememberPlatformDateFormat(language: String): DiaryDateFormat {
    val context = LocalContext.current
    // 기기 언어나 앱별 언어가 바뀌면 구성이 바뀌어 다시 만듦
    val device = LocalConfiguration.current.locales[0].toLanguageTag()
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(language, device, is24Hour) {
        AndroidDateFormat(Locale.forLanguageTag(formatLocaleTag(language, device)), is24Hour)
    }
}

private class AndroidDateFormat(locale: Locale, override val is24Hour: Boolean) : DiaryDateFormat {
    private val monthFormat = formatter(locale, DateSkeleton.MONTH)
    private val monthShortFormat = formatter(locale, DateSkeleton.MONTH_SHORT)
    private val yearFormat = formatter(locale, DateSkeleton.YEAR)
    private val monthDayFormat = formatter(locale, DateSkeleton.MONTH_DAY)
    private val yearMonthFormat = formatter(locale, DateSkeleton.YEAR_MONTH)
    private val fullDateFormat = formatter(locale, DateSkeleton.FULL_DATE)
    private val rangeFormat = intervalFormatter(locale, DateSkeleton.FULL_DATE)
    private val monthDayRangeFormat = intervalFormatter(locale, DateSkeleton.MONTH_DAY)
    private val dayWeekdayFormat = formatter(locale, DateSkeleton.DAY_WITH_WEEKDAY)
    private val weekdayFullFormat = formatter(locale, DateSkeleton.WEEKDAY)
    private val weekdayFormat = formatter(locale, DateSkeleton.WEEKDAY_SHORT)
    private val timeFormat = formatter(locale, if (is24Hour) DateSkeleton.TIME_24 else DateSkeleton.TIME_12)

    override fun month(date: LocalDate): String = monthFormat.format(Date(date.utcMillis())).keepTogether()

    override fun yearMonth(date: LocalDate): String = yearMonthFormat.format(Date(date.utcMillis())).keepTogether()

    override fun monthShort(date: LocalDate): String = monthShortFormat.format(Date(date.utcMillis())).keepTogether()

    override fun weekday(date: LocalDate): String = weekdayFullFormat.format(Date(date.utcMillis())).keepTogether()

    override fun year(date: LocalDate): String = yearFormat.format(Date(date.utcMillis())).keepTogether()

    override fun monthDay(date: LocalDate): String = monthDayFormat.format(Date(date.utcMillis())).keepTogether()

    override fun fullDate(date: LocalDate): String = fullDateFormat.format(Date(date.utcMillis())).keepTogether()

    override fun dateRange(start: LocalDate, end: LocalDate, withYear: Boolean): String =
        (if (withYear) rangeFormat else monthDayRangeFormat).format(
            DateInterval(start.utcMillis(), end.utcMillis()),
            StringBuffer(),
            FieldPosition(0)
        ).toString().keepRangeTogether()

    override fun dayWithWeekday(date: LocalDate): String = dayWeekdayFormat.format(Date(date.utcMillis())).keepTogether()

    override fun weekdayShort(date: LocalDate): String = weekdayFormat.format(Date(date.utcMillis())).keepTogether()

    override fun time(time: LocalTime): String = timeFormat.format(Date(time.utcMillis())).keepTogether()
}

private fun formatter(locale: Locale, skeleton: String): SimpleDateFormat {
    val utc = TimeZone.getTimeZone("UTC")
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).apply {
        calendar = GregorianCalendar(utc, locale)
        timeZone = utc
    }
}

// 기기 달력이 일본 연호·불기여도 양력으로
private fun intervalFormatter(locale: Locale, skeleton: String): DateIntervalFormat =
    DateIntervalFormat.getInstance(skeleton, ULocale.forLocale(locale).setKeywordValue("calendar", "gregorian")).apply {
        timeZone = IcuTimeZone.getTimeZone("UTC")
    }
