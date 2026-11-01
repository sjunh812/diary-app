package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeIdentifier
import platform.Foundation.timeZoneForSecondsFromGMT

@Composable
internal actual fun rememberPlatformDateFormat(language: String): DiaryDateFormat {
    val current = NSLocale.currentLocale
    val device = current.localeIdentifier
    return remember(language, device) {
        val tag = formatLocaleTag(language, device)
        // 기기 언어와 같으면 기기 설정이 담긴 지금 로캘을 그대로 쓴다.
        val locale = if (tag == languageTag(device)) current else NSLocale(localeIdentifier = tag)
        IosDateFormat(locale, is24Hour = usesTwentyFourHour(current))
    }
}

/** 기기가 24시간 표기를 쓰는지. 이 언어의 기본 시각 틀에 오전·오후(a)가 없으면 24시간이다. */
internal fun usesTwentyFourHour(locale: NSLocale): Boolean = NSDateFormatter.dateFormatFromTemplate("j", 0u, locale)?.contains('a') == false

internal class IosDateFormat(private val locale: NSLocale, is24Hour: Boolean) : DiaryDateFormat {
    private val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)
    private val gregorian = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)?.also { it.timeZone = utc }
    private val monthFormat = formatter(DateSkeleton.MONTH)
    private val yearMonthFormat = formatter(DateSkeleton.YEAR_MONTH)
    private val fullDateFormat = formatter(DateSkeleton.FULL_DATE)
    private val weekdayFormat = formatter(DateSkeleton.WEEKDAY_SHORT)
    private val timeFormat = formatter(if (is24Hour) DateSkeleton.TIME_24 else DateSkeleton.TIME_12)

    override fun month(date: LocalDate): String = monthFormat.stringFromDate(nsDate(date.utcMillis()))

    override fun yearMonth(date: LocalDate): String = yearMonthFormat.stringFromDate(nsDate(date.utcMillis()))

    override fun fullDate(date: LocalDate): String = fullDateFormat.stringFromDate(nsDate(date.utcMillis()))

    override fun weekdayShort(date: LocalDate): String = weekdayFormat.stringFromDate(nsDate(date.utcMillis()))

    override fun time(time: LocalTime): String = timeFormat.stringFromDate(nsDate(time.utcMillis()))

    private fun formatter(template: String) = NSDateFormatter().also {
        it.locale = locale
        gregorian?.let { calendar -> it.calendar = calendar }
        it.timeZone = utc
        it.setLocalizedDateFormatFromTemplate(template)
    }

    private fun nsDate(millis: Long) = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
}
