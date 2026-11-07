package org.sjhstudio.diary.feature.search

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val today = LocalDate(2026, 10, 31)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(filter: SearchFilter = SearchFilter()) =
        SearchViewModel(FakeEntryRepository(), filter, FakeSettingsRepository(), NoFonts) { today }

    @Test
    fun preset_periods_follow_today() {
        val model = viewModel()

        model.setPeriod(SearchPeriod.Month)
        assertEquals(DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)), model.filter.value.period)

        model.setPeriod(SearchPeriod.Year)
        assertEquals(DateRange(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)), model.filter.value.period)

        model.setPeriod(null)
        assertNull(model.filter.value.period)
        assertNull(model.period.value)
    }

    @Test
    fun custom_period_is_kept_as_custom() {
        val model = viewModel()
        val range = DateRange(LocalDate(2026, 1, 2), LocalDate(2026, 3, 4))

        model.setCustomPeriod(range)

        assertEquals(SearchPeriod.Custom, model.period.value)
        assertEquals(range, model.filter.value.period)
    }

    @Test
    fun filter_from_insight_keeps_mood_and_period() {
        val range = DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))

        val model = viewModel(SearchFilter(moods = setOf(Mood.GOOD), period = range))

        assertEquals(SearchPeriod.Custom, model.period.value)
        assertEquals(setOf(Mood.GOOD), model.filter.value.moods)
    }

    @Test
    fun toggles_flip_conditions() {
        val model = viewModel()

        model.togglePhoto()
        model.toggleFavorites()
        model.setQuery("바다")

        assertEquals(SearchFilter(query = "바다", hasPhoto = true, favoritesOnly = true), model.filter.value)
    }

    private object NoFonts : FontInstaller {
        override fun installedPath(id: String): String? = null

        override fun install(id: String) = emptyFlow<FontInstallState>()
    }
}
