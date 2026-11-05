package org.sjhstudio.diary.feature.insight.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.DayOfWeek
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.GoodShare
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCompany
import org.sjhstudio.diary.core.model.MoodGap
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.StatsChange
import org.sjhstudio.diary.core.model.StatsFactor
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.feature.insight.AdvancedStatsScreen

private val Sample =
    AdvancedStats(
        gaps =
            listOf(
                MoodGap(StatsFactor.Sky(Weather.RAIN), GoodShare(9, 40), GoodShare(78, 150)),
                MoodGap(StatsFactor.LongEntries, GoodShare(38, 52), GoodShare(57, 138))
            ),
        change = StatsChange(GoodShare(70, 160), GoodShare(99, 190), 118, 143),
        weekdays =
            mapOf(
                DayOfWeek.SUNDAY to GoodShare(18, 25),
                DayOfWeek.MONDAY to GoodShare(9, 24),
                DayOfWeek.TUESDAY to GoodShare(11, 23),
                DayOfWeek.WEDNESDAY to GoodShare(12, 26),
                DayOfWeek.THURSDAY to GoodShare(10, 22),
                DayOfWeek.FRIDAY to GoodShare(14, 24),
                DayOfWeek.SATURDAY to GoodShare(17, 23)
            ),
        dayParts = mapOf(DayPart.EVENING to GoodShare(60, 110), DayPart.MORNING to GoodShare(20, 40)),
        weather = mapOf(Weather.SUNNY to GoodShare(50, 80), Weather.RAIN to GoodShare(9, 40)),
        places = listOf("집" to GoodShare(40, 70), "회사" to GoodShare(15, 40)),
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

@Preview(name = "고급 통계 · 뚜렷한 차이 없음", locale = "ko", widthDp = 390, heightDp = 1500)
@Composable
private fun AdvancedNoGap() = Stats(false, Sample.copy(gaps = emptyList()))

@Preview(name = "고급 통계 · 일기가 적음", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedFew() = Stats(false, Sample.copy(habit = RecordHabit(3, 3, 2, 120, 0, DayPart.EVENING)))

@Preview(name = "고급 통계 · 불러오는 중", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedLoading() = Stats(false, null)

@Preview(name = "고급 통계 · 넓은 화면", locale = "ko", widthDp = 1194, heightDp = 834)
@Composable
private fun AdvancedWide() = Stats(false, Sample)
