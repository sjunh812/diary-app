package org.sjhstudio.diary.feature.calendar

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val today = LocalDate(2026, 10, 31)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg days: LocalDate) =
        CalendarViewModel(FakeEntryRepository(days.associateWith { DayMood(it, null, 1) }), FakeSettingsRepository(), NoFonts) { today }

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
