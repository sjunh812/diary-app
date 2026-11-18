package org.sjhstudio.diary.feature.insight

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.ToneShare
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_hard_time

// 상황별 기분 결론 줄. 1등이 하나일 때만 말함
@OptIn(ExperimentalTestApi::class)
class ToneSummaryUiTest {
    private fun stats(weekdays: Map<DayOfWeek, ToneShare>) = AdvancedStats(
        moodEntries = 140,
        change = null,
        weekdays = weekdays,
        dayParts = emptyMap(),
        weather = emptyMap(),
        places = emptyList(),
        company = emptyList(),
        habit = RecordHabit(140, 140, 3, 120, 0, DayPart.EVENING)
    )

    private val week = DayOfWeek.entries.associateWith { ToneShare(10, 20, 5) }

    @Test
    fun names_the_row_with_most_negative_moods() = runComposeUiTest {
        var expected = ""
        setContent {
            expected = stringResource(Res.string.insight_adv_when_hard_time, stringResource(Res.string.insight_adv_wd_wed) + "⁠")
            DiaryTheme(darkTheme = false) { MoodTimes(stats(week + (DayOfWeek.WEDNESDAY to ToneShare(4, 20, 12)))) }
        }
        waitForIdle()
        onNodeWithText(expected).assertExists()
    }

    @Test
    fun tie_says_nothing() = runComposeUiTest {
        var tied = ""
        setContent {
            tied = stringResource(Res.string.insight_adv_when_hard_time, stringResource(Res.string.insight_adv_wd_mon) + "⁠")
            DiaryTheme(darkTheme = false) { MoodTimes(stats(week)) }
        }
        waitForIdle()
        onNodeWithText(tied).assertDoesNotExist()
    }
}
