package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod

// 새 기간을 읽는 동안 아래 광고가 밀렸다 돌아오면 깜빡여 보임
@OptIn(ExperimentalTestApi::class)
class InsightReloadUiTest {
    @Test
    fun reading_a_new_week_keeps_the_ad_in_place() = runComposeUiTest {
        val today = LocalDate(2026, 11, 22)
        val read = InsightData(MoodCounts(emptyMap(), noMood = 3), emptyList(), emptyMap())
        var period by mutableStateOf<StatsPeriod>(StatsPeriod.Week.of(today))
        var data by mutableStateOf<InsightData?>(read)
        setContent {
            DiaryTheme(darkTheme = false) {
                InsightScreen(
                    period = period,
                    data = data,
                    today = today,
                    onKind = {},
                    onPrevious = {},
                    onNext = {},
                    onMood = {},
                    first = LocalDate(2026, 1, 1),
                    adSlot = { Box(Modifier.fillMaxWidth().height(120.dp).testTag("ad")) }
                )
            }
        }
        val before = onNode(hasTestTag("ad")).fetchSemanticsNode().positionInRoot.y

        period = StatsPeriod.Week(LocalDate(2026, 11, 15))
        data = null
        waitForIdle()

        assertEquals(before, onNode(hasTestTag("ad")).fetchSemanticsNode().positionInRoot.y)
    }
}
