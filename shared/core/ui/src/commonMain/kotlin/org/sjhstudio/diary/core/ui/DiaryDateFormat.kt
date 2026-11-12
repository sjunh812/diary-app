package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage

/**
 * 날짜와 시각을 화면 언어의 형식으로 씀(`docs/design.md` 여러 언어)
 * "%1$s월"처럼 문자열로 조립하지 않고 플랫폼의 언어별 형식을 씀
 * 달력은 기기 설정과 상관없이 양력
 */
@Stable
interface DiaryDateFormat {
    /** 11월, November, 11月 */
    fun month(date: LocalDate): String

    /** 2026년 11월, November 2026, 2026年11月 */
    fun yearMonth(date: LocalDate): String

    /** 11월, Nov, 11月 */
    fun monthShort(date: LocalDate): String

    /** 2026년, 2026, 2026年 */
    fun year(date: LocalDate): String

    /** 11월 1일, November 1, 11月1日 */
    fun monthDay(date: LocalDate): String

    /** 2026년 11월 1일, November 1, 2026, 2026年11月1日 */
    fun fullDate(date: LocalDate): String

    /** 10월 31일 토요일, Saturday, October 31, 10月31日土曜日 */
    fun dayWithWeekday(date: LocalDate): String

    /** 일요일, Sunday, 日曜日 */
    fun weekday(date: LocalDate): String

    /** 일, Sun, 日 */
    fun weekdayShort(date: LocalDate): String

    /** 오후 3:05, 3:05 PM, 午後3:05. 기기가 24시간 표기면 15:05 */
    fun time(time: LocalTime): String

    /**
     * 기기가 24시간 표기를 쓰는지
     * 시각 휠이 오전·오후 열을 뺄지 정함
     */
    val is24Hour: Boolean
}

@Composable
fun rememberDiaryDateFormat(): DiaryDateFormat = rememberPlatformDateFormat(diaryLanguage())

@Composable
internal expect fun rememberPlatformDateFormat(language: String): DiaryDateFormat

/**
 * 플랫폼 형식에 넘기는 틀(Unicode 날짜 틀 기호)
 * 순서와 낱말은 플랫폼이 언어에 맞춰 정함
 */
internal object DateSkeleton {
    const val MONTH = "MMMM"
    const val MONTH_SHORT = "MMM"
    const val YEAR = "y"
    const val MONTH_DAY = "MMMMd"
    const val YEAR_MONTH = "yMMMM"
    const val FULL_DATE = "yMMMMd"
    const val DAY_WITH_WEEKDAY = "MMMMEEEEd"
    const val WEEKDAY = "EEEE"
    const val WEEKDAY_SHORT = "EEE"
    const val TIME_12 = "hm"
    const val TIME_24 = "Hm"
}

/**
 * 형식에 쓸 언어 태그
 * 기기 언어가 화면 언어와 같으면 기기 지역까지 따르고(영국 영어의 날짜 순서), 다르면 화면 언어만 씀
 * 프랑스어 기기에서 영어 화면이 보이면 날짜도 영어
 */
internal fun formatLocaleTag(language: String, deviceTag: String): String {
    val device = languageTag(deviceTag)
    return if (device.substringBefore('-').equals(language, ignoreCase = true)) device else language
}

/**
 * 날짜·시각 안에서 줄이 바뀌지 않게 띄어쓰기를 줄바꿈 없는 공백으로 바꿈
 * 문장에 넣으면 "11월 / 12일"처럼 끊겼음
 */
internal fun String.keepTogether(): String = replace(' ', ' ')

/** iOS 로캘 이름(ja_JP@calendar=japanese)을 언어 태그(ja-JP)로 바꿈 */
internal fun languageTag(localeName: String): String = localeName.substringBefore('@').replace('_', '-')

/**
 * 그날 0시(UTC)의 밀리초
 * 형식도 UTC로 읽어서 기기 시간대가 날짜를 바꾸지 않음
 */
internal fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun LocalTime.utcMillis(): Long = LocalDate(1970, 1, 1).atTime(this).toInstant(TimeZone.UTC).toEpochMilliseconds()
