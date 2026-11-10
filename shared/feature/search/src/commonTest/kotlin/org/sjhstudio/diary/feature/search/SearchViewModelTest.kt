package org.sjhstudio.diary.feature.search

import androidx.lifecycle.SavedStateHandle
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
import org.sjhstudio.diary.core.model.Weather
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
        SearchViewModel(FakeEntryRepository(), filter, FakeSettingsRepository(), NoFonts, today = { today }, saved = SavedStateHandle())

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

    /** 초기화는 고른 조건만 지우고 쓴 낱말은 남김 */
    @Test
    fun clearing_conditions_keeps_the_query() {
        val range = DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
        val model = viewModel(SearchFilter(moods = setOf(Mood.GOOD), period = range))
        model.togglePhoto()
        model.setQuery("바다")
        assertTrue(model.filter.value.hasConditions)

        model.clearConditions()

        assertEquals(SearchFilter(query = "바다"), model.filter.value)
        assertEquals(null, model.period.value)
        assertFalse(model.filter.value.hasConditions)
    }

    private object NoFonts : FontInstaller {
        override fun installedPath(id: String): String? = null

        override fun install(id: String) = emptyFlow<FontInstallState>()
    }

    /** 앱이 꺼졌다 돌아와도 치던 글과 고른 조건 유지 */
    @Test
    fun the_filter_survives_the_app_being_killed() {
        val filter = SearchFilter(
            query = "바다 😀",
            moods = setOf(Mood.entries.first()),
            weathers = setOf(Weather.entries.last()),
            hasPhoto = true,
            favoritesOnly = true,
            period = DateRange(LocalDate(2026, 1, 1), LocalDate(2026, 1, 31))
        )
        assertEquals(filter, decodeFilter(encodeFilter(filter)))
        assertEquals(SearchFilter(), decodeFilter(encodeFilter(SearchFilter())))
        assertEquals(null, decodeFilter("깨진 값"))
    }
}
