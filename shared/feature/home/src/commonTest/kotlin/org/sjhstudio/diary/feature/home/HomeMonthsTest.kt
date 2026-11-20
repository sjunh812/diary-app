package org.sjhstudio.diary.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.model.MonthCount
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.fakeEntryRow

// 달별 일기 수는 읽기 전 null, 읽으면 값. 일기가 없으면 빈 목록이라 둘을 가림
class HomeMonthsTest {
    @Test
    fun unknown_until_read_then_counted() = runTest {
        val rows = listOf(
            fakeEntryRow("a", LocalDate(2024, 10, 29), "가", true),
            fakeEntryRow("b", LocalDate(2024, 10, 28), "나", false)
        )
        val months = HomeMonths(FakeEntryRepository(rows = rows), backgroundScope)
        assertNull(months.all.value)

        months.start()
        // 두 번 불러도 한 번만 읽음
        months.start()
        testScheduler.runCurrent()

        assertEquals(listOf(MonthCount(YearMonth(2024, 10), 2)), months.all.value)
        assertEquals(listOf(MonthCount(YearMonth(2024, 10), 1)), months.favorites.value)
    }

    @Test
    fun no_entries_is_an_empty_list_not_unknown() = runTest {
        val months = HomeMonths(FakeEntryRepository(), backgroundScope)
        months.start()
        testScheduler.runCurrent()

        assertEquals(emptyList(), months.all.value)
    }
}
