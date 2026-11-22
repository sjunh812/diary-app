package org.sjhstudio.diary.feature.insight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
import org.sjhstudio.diary.feature.insight.resources.insight_next
import org.sjhstudio.diary.feature.insight.resources.insight_period_sheet_title
import org.sjhstudio.diary.feature.insight.resources.insight_previous

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
                        first = LocalDate(2024, 1, 1)
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

    /** 첫 일기가 든 달이면 화살표도 시트도 그 앞으로 가지 않음 */
    @Test
    fun arrows_and_sheet_start_at_the_first_month() = runComposeUiTest {
        var title = ""
        var previous = ""
        var next = ""
        var february = ""
        var march = ""
        setContent {
            title = stringResource(Res.string.insight_period_sheet_title)
            previous = stringResource(Res.string.insight_previous)
            next = stringResource(Res.string.insight_next)
            val dates = rememberDiaryDateFormat()
            february = dates.monthShort(LocalDate(2026, 2, 1))
            march = dates.monthShort(LocalDate(2026, 3, 1))
            DiaryTheme(darkTheme = false) {
                InsightScreen(
                    period = StatsPeriod.Month(YearMonth(2026, 3)),
                    data = InsightData(MoodCounts(emptyMap(), 0), emptyList(), emptyMap()),
                    today = today,
                    onKind = {},
                    onPrevious = {},
                    onNext = {},
                    onMood = {},
                    first = LocalDate(2026, 3, 1)
                )
            }
        }
        onNode(hasContentDescription(previous)).assertIsNotEnabled()
        onNode(hasContentDescription(next)).assertIsEnabled()
        onNode(hasText("2026", substring = true).and(hasClickAction())).performClick()
        waitForIdle()
        onNode(hasText(title)).assertExists()
        onNode(hasText(february).and(hasClickAction())).assertIsNotEnabled()
        onNode(hasText(march).and(hasClickAction())).assertIsEnabled()
    }

    /** 전체는 넘기거나 고를 기간이 없어 기간 줄을 두지 않음 */
    @Test
    fun all_time_has_no_period_row() = runComposeUiTest {
        var title = ""
        var previous = ""
        var next = ""
        setContent {
            title = stringResource(Res.string.insight_period_sheet_title)
            previous = stringResource(Res.string.insight_previous)
            next = stringResource(Res.string.insight_next)
            DiaryTheme(darkTheme = false) {
                InsightScreen(
                    period = StatsPeriod.All,
                    data = null,
                    today = today,
                    onKind = {},
                    onPrevious = {},
                    onNext = {},
                    onMood = {},
                    first = today
                )
            }
        }
        onNode(hasContentDescription(title)).assertDoesNotExist()
        onNode(hasText(title)).assertDoesNotExist()
        onNode(hasContentDescription(previous)).assertDoesNotExist()
        onNode(hasContentDescription(next)).assertDoesNotExist()
    }
}
