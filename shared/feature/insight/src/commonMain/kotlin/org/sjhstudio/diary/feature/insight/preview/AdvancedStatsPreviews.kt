package org.sjhstudio.diary.feature.insight.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.DayOfWeek
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCompany
import org.sjhstudio.diary.core.model.MoodTrend
import org.sjhstudio.diary.core.model.RecentMood
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.StatsChange
import org.sjhstudio.diary.core.model.StatsSnapshot
import org.sjhstudio.diary.core.model.ToneShare
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.feature.insight.AdvancedStatsScreen

private val Sample =
    AdvancedStats(
        moodEntries = 186,
        change =
            StatsChange(
                StatsSnapshot(ToneShare(70, 160, 41), Mood.MEH, 118, 160, 38_400, 180),
                StatsSnapshot(ToneShare(99, 190, 33), Mood.GOOD, 143, 186, 148_210, 212)
            ),
        recent = RecentMood(MoodTrend.BETTER, ToneShare(12, 27, 8), ToneShare(18, 29, 5)),
        weekdays =
            mapOf(
                DayOfWeek.SUNDAY to ToneShare(18, 25),
                DayOfWeek.MONDAY to ToneShare(9, 24),
                DayOfWeek.TUESDAY to ToneShare(11, 23),
                DayOfWeek.WEDNESDAY to ToneShare(12, 26),
                DayOfWeek.THURSDAY to ToneShare(10, 22),
                DayOfWeek.FRIDAY to ToneShare(14, 24),
                DayOfWeek.SATURDAY to ToneShare(17, 23)
            ),
        dayParts = mapOf(DayPart.EVENING to ToneShare(60, 110), DayPart.MORNING to ToneShare(20, 40)),
        weather = mapOf(Weather.SUNNY to ToneShare(50, 80), Weather.RAIN to ToneShare(9, 40)),
        places = listOf("집" to ToneShare(40, 70), "회사" to ToneShare(15, 40)),
        company =
            listOf(
                MoodCompany(Mood.GOOD, 62, Weather.SUNNY, DayOfWeek.SUNDAY, "집"),
                MoodCompany(Mood.MEH, 41, Weather.PARTLY_CLOUDY, DayOfWeek.WEDNESDAY, "회사"),
                MoodCompany(Mood.TIRED, 23, Weather.CLOUDY, DayOfWeek.MONDAY, "회사")
            ),
        habit = RecordHabit(186, 143, 21, 48_210, 212, DayPart.EVENING)
    )

@Composable
private fun Stats(dark: Boolean, stats: AdvancedStats?, period: AdvancedPeriod = AdvancedPeriod.THIS_YEAR) = DiaryTheme(darkTheme = dark) {
    AdvancedStatsScreen(period, stats, {}, {}, {})
}

@Preview(name = "고급 통계 · 라이트", locale = "ko", widthDp = 390, heightDp = 1500)
@Composable
private fun AdvancedLight() = Stats(false, Sample)

@Preview(name = "고급 통계 · 다크", locale = "ko", widthDp = 390, heightDp = 1500)
@Composable
private fun AdvancedDark() = Stats(true, Sample)

@Preview(name = "고급 통계 · 요즘 기분이 무거움", locale = "ko", widthDp = 390, heightDp = 1500)
@Composable
private fun AdvancedHarder() = Stats(false, Sample.copy(recent = RecentMood(MoodTrend.HARDER, ToneShare(18, 29, 5), ToneShare(10, 27, 13))))

@Preview(name = "고급 통계 · 요즘 기분이 비슷함", locale = "ko", widthDp = 390, heightDp = 1500)
@Composable
private fun AdvancedSteady() = Stats(false, Sample.copy(recent = RecentMood(MoodTrend.STEADY, ToneShare(12, 27, 8), ToneShare(13, 28, 8))))

@Preview(name = "고급 통계 · 일기가 적음", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedFew() = Stats(false, Sample.copy(habit = RecordHabit(3, 3, 2, 120, 0, DayPart.EVENING)))

@Preview(name = "고급 통계 · 기분을 안 고름", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedNoMoods() = Stats(false, Sample.copy(moodEntries = 2, recent = null, company = emptyList()))

@Preview(name = "고급 통계 · 불러오는 중", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedLoading() = Stats(false, null)

@Preview(name = "고급 통계 · 넓은 화면", locale = "ko", widthDp = 1194, heightDp = 834)
@Composable
private fun AdvancedWide() = Stats(false, Sample)
