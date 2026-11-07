package org.sjhstudio.diary.feature.calendar

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.fakeEntryRow

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private var today = LocalDate(2026, 10, 31)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg days: LocalDate) =
        CalendarViewModel(FakeEntryRepository(days.associateWith { DayMood(it, null, 1) }), FakeSettingsRepository(), NoFonts) { today }

    private fun viewModel(rows: List<EntryRow> = emptyList(), slowDays: Map<LocalDate, Flow<List<EntryRow>>>): CalendarViewModel {
        val entries =
            object : EntryRepository by FakeEntryRepository(rows = rows) {
                override fun day(date: LocalDate): Flow<List<EntryRow>> = slowDays[date] ?: flowOf(rows.filter { it.date == date })
            }
        return CalendarViewModel(entries, FakeSettingsRepository(), NoFonts) { today }
    }

    @Test
    fun opens_on_today() {
        assertEquals(today, viewModel().selected.value)
    }

    @Test
    fun moving_to_a_month_selects_its_last_written_day() {
        val model = viewModel(LocalDate(2026, 9, 3), LocalDate(2026, 9, 20))

        model.showMonth(YearMonth(2026, 9))

        assertEquals(LocalDate(2026, 9, 20), model.selected.value)
    }

    @Test
    fun an_empty_month_selects_nothing() {
        val model = viewModel()

        model.showMonth(YearMonth(2026, 3))

        assertNull(model.selected.value)
    }

    @Test
    fun coming_back_to_this_month_selects_today() {
        val model = viewModel(LocalDate(2026, 9, 3))
        model.showMonth(YearMonth(2026, 9))

        model.showMonth(YearMonth(2026, 10))

        assertEquals(today, model.selected.value)
    }

    @Test
    fun keeps_a_day_picked_in_the_shown_month() {
        val model = viewModel()
        model.select(LocalDate(2026, 10, 5))

        model.showMonth(YearMonth(2026, 10))

        assertEquals(LocalDate(2026, 10, 5), model.selected.value)
    }

    @Test
    fun future_entries_extend_the_last_month() {
        assertEquals(YearMonth(2026, 12), viewModel(LocalDate(2026, 12, 25)).lastMonth.value)
        assertEquals(YearMonth(2026, 10), viewModel(LocalDate(2026, 1, 1)).lastMonth.value)
    }

    /** 일기가 있는 날에 "이 날 쓰기"가 먼저 보이지 않게 읽는 중과 빈 날을 가른다. */
    @Test
    fun before_the_rows_arrive_the_day_is_unknown_not_empty() = runTest {
        val slow = MutableSharedFlow<List<EntryRow>>()
        val model = viewModel(slowDays = mapOf(today to slow))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.day.collect() }

        assertEquals(CalendarDay(today, null), model.day.value)

        slow.emit(emptyList())

        assertEquals(CalendarDay(today, emptyList()), model.day.value)
    }

    /** 새 날짜 아래에 지난 날 일기가 보이지 않는다. */
    @Test
    fun picking_another_day_hides_the_rows_of_the_last_one() = runTest {
        val written = LocalDate(2026, 10, 5)
        val row = fakeEntryRow("a", today)
        val slow = MutableSharedFlow<List<EntryRow>>()
        val model = viewModel(rows = listOf(row), slowDays = mapOf(written to slow))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.day.collect() }
        assertEquals(CalendarDay(today, listOf(row)), model.day.value)

        model.select(written)

        assertEquals(CalendarDay(written, null), model.day.value)
    }

    /** 달을 넘겨 다녀와도 얼굴이 사라졌다 나타나지 않게 읽은 값을 둔다. */
    @Test
    fun a_month_read_once_is_there_at_once_when_you_come_back() = runTest {
        val day = LocalDate(2026, 9, 3)
        val model = viewModel(day)
        val month = YearMonth(2026, 9)
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.moods(month).collect() }
        assertEquals(setOf(day), model.moods(month).value.keys)
        first.cancel()

        assertEquals(setOf(day), model.moods(month).value.keys)
    }

    /** 켜 둔 채 자정을 넘기면 오늘을 보던 사람은 새 오늘을 본다. */
    @Test
    fun a_new_day_moves_today_and_the_selection_that_followed_it() {
        today = LocalDate(2026, 10, 30)
        val model = viewModel()

        today = LocalDate(2026, 10, 31)

        assertEquals(false, model.refreshToday())
        assertEquals(today, model.todayState.value)
        assertEquals(today, model.selected.value)
    }

    @Test
    fun a_new_day_leaves_a_day_the_user_picked() {
        today = LocalDate(2026, 10, 30)
        val model = viewModel()
        model.select(LocalDate(2026, 10, 5))

        today = LocalDate(2026, 10, 31)
        model.refreshToday()

        assertEquals(LocalDate(2026, 10, 5), model.selected.value)
        assertEquals(today, model.todayState.value)
    }

    @Test
    fun a_new_month_asks_the_calendar_to_turn_and_opens_the_next_page() {
        val model = viewModel()

        today = LocalDate(2026, 11, 1)

        assertEquals(true, model.refreshToday())
        assertEquals(YearMonth(2026, 11), model.lastMonth.value)
        model.showMonth(YearMonth(2026, 11))
        assertEquals(today, model.selected.value)
    }

    @Test
    fun the_same_day_changes_nothing() {
        val model = viewModel()
        model.select(LocalDate(2026, 10, 5))

        assertEquals(false, model.refreshToday())
        assertEquals(LocalDate(2026, 10, 5), model.selected.value)
    }

    @Test
    fun page_numbers_round_trip() {
        for (month in listOf(YearMonth(1970, 1), YearMonth(1999, 12), YearMonth(2026, 10))) {
            assertEquals(month, monthOfPage(month.pageIndex()))
        }
    }

    private object NoFonts : FontInstaller {
        override fun installedPath(id: String): String? = null

        override fun install(id: String) = kotlinx.coroutines.flow.emptyFlow<FontInstallState>()
    }
}
