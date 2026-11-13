package org.sjhstudio.diary.feature.insight

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.advancedStats

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedStatsViewModelTest {
    private val today = LocalDate(2026, 11, 6)

    private class Stats : StatsRepository {
        val asked = ArrayList<AdvancedPeriod>()

        override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> = emptyFlow()

        override fun moodFlow(period: StatsPeriod): Flow<List<DayMood>> = emptyFlow()

        override fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>> = emptyFlow()

        override fun advanced(period: AdvancedPeriod, today: LocalDate): Flow<AdvancedStats> {
            asked += period
            return flowOf(advancedStats(emptyList()))
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun starts_with_everything_because_gaps_need_many_entries() = runTest {
        val stats = Stats()
        val viewModel = AdvancedStatsViewModel(stats) { today }

        viewModel.data.filterNotNull().first()

        assertEquals(AdvancedPeriod.ALL, viewModel.period.value)
        assertNull(viewModel.range())
    }

    /** 새 기간 이름 아래에 앞 기간 숫자가 보이지 않음 */
    @Test
    fun switching_hides_the_last_numbers_until_the_new_ones_arrive() = runTest {
        val slow = MutableSharedFlow<AdvancedStats>()
        val stats =
            object : StatsRepository by Stats() {
                override fun advanced(period: AdvancedPeriod, today: LocalDate): Flow<AdvancedStats> =
                    if (period == AdvancedPeriod.ALL) flowOf(advancedStats(emptyList())) else slow
            }
        val viewModel = AdvancedStatsViewModel(stats) { today }
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.data.value)

        viewModel.select(AdvancedPeriod.THIS_YEAR)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.data.value)
    }

    @Test
    fun tapping_the_same_period_keeps_the_numbers() = runTest {
        val viewModel = AdvancedStatsViewModel(Stats()) { today }
        testScheduler.advanceUntilIdle()
        val first = assertNotNull(viewModel.data.value)

        viewModel.select(AdvancedPeriod.ALL)
        testScheduler.advanceUntilIdle()

        assertSame(first, viewModel.data.value)
    }

    /** 켜 둔 채 날이 바뀌면 최근 기간을 새 오늘로 다시 계산 */
    @Test
    fun a_new_day_measures_the_period_again() = runTest {
        var date = today
        val days = ArrayList<LocalDate>()
        val stats =
            object : StatsRepository by Stats() {
                override fun advanced(period: AdvancedPeriod, today: LocalDate): Flow<AdvancedStats> {
                    days += today
                    return flowOf(advancedStats(emptyList()))
                }
            }
        val viewModel = AdvancedStatsViewModel(stats) { date }
        testScheduler.advanceUntilIdle()

        date = LocalDate(2026, 11, 7)
        viewModel.refreshToday()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(today, date), days.distinct())
    }

    @Test
    fun picking_a_period_recalculates_and_narrows_the_mood_list() = runTest {
        val stats = Stats()
        val viewModel = AdvancedStatsViewModel(stats) { today }

        viewModel.select(AdvancedPeriod.THIS_YEAR)
        viewModel.data.filterNotNull().first()

        assertEquals(AdvancedPeriod.THIS_YEAR, stats.asked.last())
        assertEquals(DateRange(LocalDate(2026, 1, 1), today), viewModel.range())
    }
}
