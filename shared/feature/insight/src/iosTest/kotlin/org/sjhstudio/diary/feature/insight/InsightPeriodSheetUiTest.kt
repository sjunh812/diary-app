package org.sjhstudio.diary.feature.insight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_all_time
import org.sjhstudio.diary.feature.insight.resources.insight_period_sheet_title

// 기간 이름을 누르면 기간 고르기가 열리고, 고른 기간으로 감
@OptIn(ExperimentalTestApi::class)
class InsightPeriodSheetUiTest {
    private val today = LocalDate(2026, 11, 11)

    /** [label]·[cell]은 언어마다 다른 이름이라 날짜 형식으로 만듦 */
    private fun pick(
        start: StatsPeriod,
        label: @Composable (DiaryDateFormat) -> String,
        cell: @Composable (DiaryDateFormat) -> String
    ): StatsPeriod? {
        var picked: StatsPeriod? = null
        runComposeUiTest {
            var title = ""
            var target = ""
            var name = ""
            var period by mutableStateOf(start)
            setContent {
                title = stringResource(Res.string.insight_period_sheet_title)
                val dates = rememberDiaryDateFormat()
                target = cell(dates)
                name = label(dates)
                DiaryTheme(darkTheme = false) {
                    InsightScreen(
                        period = period,
                        data = InsightData(MoodCounts(emptyMap(), 0), emptyList(), emptyMap()),
                        today = today,
                        onKind = {},
                        onPrevious = {},
                        onNext = {},
                        onMood = {},
                        onJump = {
                            picked = it
                            period = it
                        },
                        firstYear = 2024
                    )
                }
            }
            onNode(hasText(name, substring = true).and(hasClickAction())).performClick()
            waitForIdle()
            onNode(hasText(title)).assertExists()
            onNode(hasText(target).and(hasClickAction()).and(isEnabled())).performClick()
            waitForIdle()
            onNode(hasText(title)).assertDoesNotExist()
        }
        return picked
    }

    @Test
    fun a_month_cell_goes_to_that_month() {
        assertEquals(
            StatsPeriod.Month(YearMonth(2026, 3)),
            pick(StatsPeriod.Month(YearMonth(2026, 11)), { "2026" }) {
                it.monthShort(LocalDate(2026, 3, 1))
            }
        )
    }

    @Test
    fun a_year_cell_goes_to_that_year() {
        assertEquals(StatsPeriod.Year(2024), pick(StatsPeriod.Year(2026), { "2026" }) { it.year(LocalDate(2024, 1, 1)) })
    }

    /** 주는 날짜를 누르면 그날이 든 주 */
    @Test
    fun a_day_goes_to_its_week() {
        assertEquals(
            StatsPeriod.Week(LocalDate(2026, 11, 1)),
            pick(StatsPeriod.Week(LocalDate(2026, 11, 8)), {
                it.monthDay(LocalDate(2026, 11, 8))
            }) { "4" }
        )
    }

    @Test
    fun all_time_has_no_sheet() = runComposeUiTest {
        var title = ""
        var all = ""
        setContent {
            title = stringResource(Res.string.insight_period_sheet_title)
            all = stringResource(Res.string.insight_all_time)
            DiaryTheme(darkTheme = false) {
                InsightScreen(
                    period = StatsPeriod.All,
                    data = null,
                    today = today,
                    onKind = {},
                    onPrevious = {},
                    onNext = {},
                    onMood = {}
                )
            }
        }
        onNode(hasText(title)).assertDoesNotExist()
        onNode(hasText(all)).assertExists()
        onNode(hasText(all).and(hasClickAction())).assertDoesNotExist()
    }
}
