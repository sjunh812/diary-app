package org.sjhstudio.diary.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.model.MonthCount

// 홈 큰 제목의 달(스펙 002 FR-015)
class HomeTitleTest {
    private val today = LocalDate(2026, 11, 7)
    private val counts =
        listOf(
            MonthCount(YearMonth(2028, 3), 1),
            MonthCount(YearMonth(2027, 12), 2),
            MonthCount(YearMonth(2027, 1), 30)
        )

    @Test
    fun the_top_row_sets_the_title() {
        assertEquals(YearMonth(2027, 12), shownMonth(LocalDate(2027, 12, 5), counts, position = 1, today))
    }

    /** 먼 달에서 맨 위로 가면 첫 줄을 읽기 전에도 맨 위 달. 앞서 보던 달이 남지 않음 */
    @Test
    fun an_unread_top_row_shows_the_newest_month() {
        assertEquals(YearMonth(2028, 3), shownMonth(null, counts, position = 0, today))
    }

    /** 달 고르기로 건너뛴 자리도 읽기 전에 그 달 */
    @Test
    fun an_unread_row_is_counted_into_its_month() {
        assertEquals(YearMonth(2027, 12), shownMonth(null, counts, position = 2, today))
        assertEquals(YearMonth(2027, 1), shownMonth(null, counts, position = 3, today))
        assertEquals(YearMonth(2027, 1), shownMonth(null, counts, position = 32, today))
    }

    /** 일기를 지운 직후 목록이 편수보다 길어도 마지막 달 */
    @Test
    fun a_row_past_the_counts_shows_the_oldest_month() {
        assertEquals(YearMonth(2027, 1), shownMonth(null, counts, position = 40, today))
    }

    @Test
    fun an_empty_list_shows_today() {
        assertEquals(YearMonth(2026, 11), shownMonth(null, emptyList(), position = 0, today))
    }
}
