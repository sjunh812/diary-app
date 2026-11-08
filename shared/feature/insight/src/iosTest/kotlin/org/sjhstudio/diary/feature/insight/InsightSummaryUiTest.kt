package org.sjhstudio.diary.feature.insight

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod

// 요약 문장에 기분 얼굴을 글줄 안에 넣는다. 얼굴 자리 하나라도 잘못 만들면 돌아보기를 여는 순간 앱이 꺼진다.
@OptIn(ExperimentalTestApi::class)
class InsightSummaryUiTest {
    private fun show(counts: MoodCounts, dark: Boolean = false) = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = dark) {
                InsightScreen(
                    period = StatsPeriod.Month(YearMonth(2026, 11)),
                    data = InsightData(counts, emptyList(), emptyMap()),
                    today = LocalDate(2026, 11, 8),
                    onKind = {},
                    onPrevious = {},
                    onNext = {},
                    onMood = {}
                )
            }
        }
        waitForIdle()
        onNode(hasText("이번 달", substring = true).or(hasText("this month", substring = true, ignoreCase = true)), useUnmergedTree = true)
    }

    @Test
    fun one_top_mood_draws_its_face_without_crashing() {
        show(MoodCounts(mapOf(Mood.GOOD to 3, Mood.TIRED to 1), noMood = 0))
    }

    @Test
    fun tied_top_moods_draw_every_face_without_crashing() {
        show(MoodCounts(mapOf(Mood.entries[0] to 2, Mood.entries[1] to 2, Mood.entries[2] to 2), noMood = 1), dark = true)
    }

    @Test
    fun no_mood_at_all_still_opens() {
        show(MoodCounts(emptyMap(), noMood = 2))
    }
}
