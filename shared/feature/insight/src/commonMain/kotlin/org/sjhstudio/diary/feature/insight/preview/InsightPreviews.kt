package org.sjhstudio.diary.feature.insight.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.feature.insight.InsightScreen

private val Today = LocalDate(2026, 10, 31)
private val Counts = MoodCounts(Mood.entries.associateWith { 1 } + mapOf(Mood.GOOD to 6, Mood.TIRED to 3), 1)
private val Days = listOf(DayMood(LocalDate(2026, 10, 1), Mood.COOL, 1), DayMood(Today, Mood.GOOD, 2))

@Composable
private fun Insight(dark: Boolean, period: StatsPeriod, counts: MoodCounts) = DiaryTheme(darkTheme = dark) {
    InsightScreen(
        period = period,
        counts = counts,
        days = Days,
        pixels = Days.associate { it.date to it.mood },
        today = Today,
        onKind = {},
        onPrevious = {},
        onNext = {},
        onMood = {}
    )
}

@Preview(name = "돌아보기 · 월 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MonthLight() = Insight(dark = false, StatsPeriod.Month(YearMonth(2026, 10)), Counts)

@Preview(name = "돌아보기 · 월 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MonthDark() = Insight(dark = true, StatsPeriod.Month(YearMonth(2026, 10)), Counts)

@Preview(name = "돌아보기 · 년", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun YearLight() = Insight(dark = false, StatsPeriod.Year(2026), Counts)

@Preview(name = "돌아보기 · 빈 기간", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun EmptyLight() = Insight(dark = false, StatsPeriod.Month(YearMonth(2026, 3)), MoodCounts(emptyMap(), 0))
