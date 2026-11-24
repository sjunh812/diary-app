package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateIntervalFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCalendar
import platform.Foundation.NSTimeZone
import platform.Foundation.componentsFromLocaleIdentifier
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeIdentifier
import platform.Foundation.localeIdentifierFromComponents
import platform.Foundation.timeZoneForSecondsFromGMT

@Composable
internal actual fun rememberPlatformDateFormat(language: String): DiaryDateFormat {
    val current = NSLocale.currentLocale
    val device = current.localeIdentifier
    return remember(language, device) {
        val is24Hour = usesTwentyFourHour(current)
        formats.getOrPut(Triple(language, device, is24Hour)) { platformFormat(language, device, is24Hour) }
    }
}

// 목록 줄마다 형식 열두 개를 새로 만들지 않게 같은 언어·지역·시각 표기면 하나를 같이 씀. 화면 스레드에서만 씀
private val formats = mutableMapOf<Triple<String, String, Boolean>, IosDateFormat>()

private fun platformFormat(language: String, device: String, is24Hour: Boolean): IosDateFormat {
    val tag = formatLocaleTag(language, device)
    // 기기 언어와 같으면 기기 지역 설정까지 따름
    // 달력만 그레고리력으로. 기기 달력이 일본 연호·불기면 틀에 연호(G)가 끼어 "西暦", "AD"가 붙음
    val base = if (tag == languageTag(device)) device else tag
    val parts = NSLocale.componentsFromLocaleIdentifier(base) + (NSLocaleCalendar to NSCalendarIdentifierGregorian)
    val locale = NSLocale(localeIdentifier = NSLocale.localeIdentifierFromComponents(parts))
    return IosDateFormat(locale, is24Hour)
}

/**
 * 기기가 24시간 표기를 쓰는지
 * 이 언어의 기본 시각 틀에 오전·오후(a)가 없으면 24시간
 */
internal fun usesTwentyFourHour(locale: NSLocale): Boolean = NSDateFormatter.dateFormatFromTemplate("j", 0u, locale)?.contains('a') == false

internal class IosDateFormat(private val locale: NSLocale, override val is24Hour: Boolean) : DiaryDateFormat {
    private val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)
    private val gregorian = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)?.also { it.timeZone = utc }
    private val monthFormat = formatter(DateSkeleton.MONTH)
    private val monthShortFormat = formatter(DateSkeleton.MONTH_SHORT)
    private val yearFormat = formatter(DateSkeleton.YEAR)
    private val monthDayFormat = formatter(DateSkeleton.MONTH_DAY)
    private val yearMonthFormat = formatter(DateSkeleton.YEAR_MONTH)
    private val fullDateFormat = formatter(DateSkeleton.FULL_DATE)
    private val dayWeekdayFormat = formatter(DateSkeleton.DAY_WITH_WEEKDAY)
    private val weekdayFullFormat = formatter(DateSkeleton.WEEKDAY)
    private val weekdayFormat = formatter(DateSkeleton.WEEKDAY_SHORT)
    private val timeFormat = formatter(if (is24Hour) DateSkeleton.TIME_24 else DateSkeleton.TIME_12)
    private val rangeFormat = intervalFormatter(DateSkeleton.FULL_DATE)
    private val monthDayRangeFormat = intervalFormatter(DateSkeleton.MONTH_DAY)

    override fun month(date: LocalDate): String = monthFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun yearMonth(date: LocalDate): String = yearMonthFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun monthShort(date: LocalDate): String = monthShortFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun weekday(date: LocalDate): String = weekdayFullFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun year(date: LocalDate): String = yearFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun monthDay(date: LocalDate): String = monthDayFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun fullDate(date: LocalDate): String = fullDateFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun dateRange(start: LocalDate, end: LocalDate, withYear: Boolean): String =
        (if (withYear) rangeFormat else monthDayRangeFormat).stringFromDate(
            nsDate(start.utcMillis()),
            nsDate(end.utcMillis())
        ).keepRangeTogether()

    override fun dayWithWeekday(date: LocalDate): String = dayWeekdayFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun weekdayShort(date: LocalDate): String = weekdayFormat.stringFromDate(nsDate(date.utcMillis())).keepTogether()

    override fun time(time: LocalTime): String = timeFormat.stringFromDate(nsDate(time.utcMillis())).keepTogether()

    private fun formatter(template: String) = NSDateFormatter().also {
        it.locale = locale
        gregorian?.let { calendar -> it.calendar = calendar }
        it.timeZone = utc
        it.setLocalizedDateFormatFromTemplate(template)
    }

    private fun intervalFormatter(template: String) = NSDateIntervalFormatter().also {
        it.locale = locale
        gregorian?.let { calendar -> it.calendar = calendar }
        it.timeZone = utc
        it.dateTemplate = template
    }

    private fun nsDate(millis: Long) = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
}
