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
 * 날짜와 시각을 화면 언어의 형식으로 쓴다(`docs/design.md` 여러 언어).
 * "%1$s월"처럼 문자열로 조립하지 않고 플랫폼의 언어별 형식을 쓴다.
 * 달력은 기기 설정과 상관없이 양력이다.
 */
@Stable
interface DiaryDateFormat {
    /** 11월, November, 11月 */
    fun month(date: LocalDate): String

    /** 2026년 11월, November 2026, 2026年11月 */
    fun yearMonth(date: LocalDate): String

    /** 일, Sun, 日 */
    fun weekdayShort(date: LocalDate): String

    /** 오후 3:05, 3:05 PM, 午後3:05. 기기가 24시간 표기면 15:05. */
    fun time(time: LocalTime): String
}

@Composable
fun rememberDiaryDateFormat(): DiaryDateFormat = rememberPlatformDateFormat(diaryLanguage())

@Composable
internal expect fun rememberPlatformDateFormat(language: String): DiaryDateFormat

/** 플랫폼 형식에 넘기는 틀(Unicode 날짜 틀 기호). 순서와 낱말은 플랫폼이 언어에 맞춰 정한다. */
internal object DateSkeleton {
    const val MONTH = "MMMM"
    const val YEAR_MONTH = "yMMMM"
    const val WEEKDAY_SHORT = "EEE"
    const val TIME_12 = "hm"
    const val TIME_24 = "Hm"
}

/**
 * 형식에 쓸 언어 태그.
 * 기기 언어가 화면 언어와 같으면 기기 지역까지 따르고(영국 영어의 날짜 순서), 다르면 화면 언어만 쓴다.
 * 프랑스어 기기에서 영어 화면이 보이면 날짜도 영어다.
 */
internal fun formatLocaleTag(language: String, deviceTag: String): String {
    val device = languageTag(deviceTag)
    return if (device.substringBefore('-').equals(language, ignoreCase = true)) device else language
}

/** iOS 로캘 이름(ja_JP@calendar=japanese)을 언어 태그(ja-JP)로 바꾼다. */
internal fun languageTag(localeName: String): String = localeName.substringBefore('@').replace('_', '-')

/** 그날 0시(UTC)의 밀리초. 형식도 UTC로 읽어서 기기 시간대가 날짜를 바꾸지 않는다. */
internal fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun LocalTime.utcMillis(): Long = LocalDate(1970, 1, 1).atTime(this).toInstant(TimeZone.UTC).toEpochMilliseconds()
