package org.sjhstudio.diary.feature.calendar

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.onDay
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.DiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat

/** 달력 앞뒤 달 날짜: 넘길 수 있는 달이면 눌러 고르고, 첫 달 앞과 마지막 달 뒤는 누르지 않음 */
@OptIn(ExperimentalTestApi::class)
class MonthGridUiTest {
    private val october = YearMonth(2026, 10)
    private val before = LocalDate(2026, 9, 28)
    private val after = LocalDate(2026, 11, 1)

    @Test
    fun earlier_month_day_is_picked_and_later_month_day_is_not_clickable() = runComposeUiTest {
        val picked = mutableListOf<LocalDate>()
        lateinit var dates: DiaryDateFormat
        setContent {
            DiaryTheme(darkTheme = false) {
                dates = rememberDiaryDateFormat()
                CalendarPanel(
                    shown = october,
                    pager = rememberPagerState(initialPage = october.pageIndex()) { october.pageIndex() + 1 },
                    twoPane = false,
                    today = LocalDate(2026, 10, 31),
                    selected = null,
                    lastMonth = october,
                    showToday = false,
                    dates = dates,
                    moods = { MutableStateFlow(emptyMap()) },
                    onSelect = { picked += it },
                    onToday = {},
                    onPick = {},
                    onMove = {}
                )
            }
        }

        // 미리 그린 9월 쪽에도 같은 날이 있어 화면 안의 10월 칸을 누름
        val cells = onAllNodes(hasContentDescription(dates.dayWithWeekday(before)))
        val visible = cells.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.width > 0f }
        cells[visible].performClick()
        waitForIdle()
        assertEquals(listOf(before), picked)

        onNode(hasContentDescription(dates.dayWithWeekday(after)) and !SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertExists()
    }

    @Test
    fun days_before_the_first_month_are_not_clickable() = runComposeUiTest {
        val first = monthOfPage(0)
        val before = first.onDay(1).minus(1, DateTimeUnit.DAY)
        lateinit var dates: DiaryDateFormat
        setContent {
            DiaryTheme(darkTheme = false) {
                dates = rememberDiaryDateFormat()
                CalendarPanel(
                    shown = first,
                    pager = rememberPagerState { 1 },
                    twoPane = false,
                    today = LocalDate(2026, 10, 31),
                    selected = null,
                    lastMonth = YearMonth(2026, 10),
                    showToday = true,
                    dates = dates,
                    moods = { MutableStateFlow(emptyMap()) },
                    onSelect = {},
                    onToday = {},
                    onPick = {},
                    onMove = {}
                )
            }
        }

        onNode(hasContentDescription(dates.dayWithWeekday(before)) and !SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertExists()
    }
}
