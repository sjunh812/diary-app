package org.sjhstudio.diary.feature.insight

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.advancedStats
import org.sjhstudio.diary.core.testing.FakePlanRepository

@OptIn(ExperimentalCoroutinesApi::class)
class InsightViewModelTest {
    private val today = LocalDate(2026, 10, 31)

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = InsightViewModel(NoStats, { today })

    /** 고급 통계는 Plus만 연다. 기존 통계는 어느 요금제든 그대로다(FR-012). */
    @Test
    fun advanced_stats_open_only_on_plus() = runTest(dispatcher) {
        val table = mapOf(Plan.FREE to false, Plan.AD_FREE to false, Plan.PLUS to true)
        for ((plan, open) in table) {
            val model = InsightViewModel(NoStats, { today }, FakePlanRepository(plan))
            advanceUntilIdle()

            assertEquals(open, model.advancedOpen.value, "$plan")
            assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value, "$plan")
        }
    }

    /** 구독이 끝나면 칸이 바로 잠기고 앱 틀이 고급 통계 화면을 닫는다. */
    @Test
    fun advanced_stats_close_when_plus_ends() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.PLUS)
        val model = InsightViewModel(NoStats, { today }, plans)
        advanceUntilIdle()
        assertEquals(true, model.advancedOpen.value)

        plans.plan.value = Plan.AD_FREE
        advanceUntilIdle()

        assertEquals(false, model.advancedOpen.value)
    }

    @Test
    fun opens_on_this_month() {
        assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), viewModel().period.value)
    }

    @Test
    fun tabs_pick_the_period_with_today() {
        val model = viewModel()

        model.select(InsightKind.Week)
        assertEquals(LocalDate(2026, 10, 25), (model.period.value as StatsPeriod.Week).start)

        model.select(InsightKind.Year)
        assertEquals(StatsPeriod.Year(2026), model.period.value)

        model.select(InsightKind.All)
        assertEquals(StatsPeriod.All, model.period.value)
    }

    @Test
    fun does_not_move_past_today() {
        val model = viewModel()

        model.next()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value)

        model.previous()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 9)), model.period.value)

        model.next()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value)
    }

    @Test
    fun bars_are_sorted_by_count_and_show_all_nine() {
        val counts = MoodCounts(Mood.entries.associateWith { 0 } + mapOf(Mood.SAD to 3, Mood.GOOD to 5), 1)

        val sorted = counts.sorted()

        assertEquals(Mood.entries.size, sorted.size)
        assertEquals(listOf(Mood.GOOD to 5, Mood.SAD to 3), sorted.take(2))
    }

    @Test
    fun month_buckets_follow_calendar_weeks() {
        // 2026년 10월 1일은 목요일이라 첫 주는 3일까지다.
        val period = StatsPeriod.Month(YearMonth(2026, 10))
        val days =
            listOf(
                DayMood(LocalDate(2026, 10, 3), Mood.GOOD, 1),
                DayMood(LocalDate(2026, 10, 4), Mood.SAD, 1),
                DayMood(LocalDate(2026, 10, 5), Mood.TIRED, 1),
                DayMood(LocalDate(2026, 10, 6), Mood.TIRED, 1),
                DayMood(LocalDate(2026, 10, 31), null, 1)
            )

        val buckets = moodBuckets(period, days)

        assertEquals(5, buckets.size)
        assertEquals(Mood.GOOD, buckets[0].mood)
        assertEquals(Mood.TIRED, buckets[1].mood)
        assertEquals(null, buckets[2].mood)
        assertEquals(null, buckets[4].mood)
    }

    @Test
    fun tie_uses_the_later_day() {
        val days = listOf(DayMood(LocalDate(2026, 10, 5), Mood.SAD, 1), DayMood(LocalDate(2026, 10, 6), Mood.GOOD, 1))

        assertEquals(Mood.GOOD, moodBuckets(StatsPeriod.Month(YearMonth(2026, 10)), days)[1].mood)
    }

    @Test
    fun last_entry_without_mood_does_not_empty_the_cell() {
        // 마지막 일기에 기분이 없어도 앞 일기의 기분으로 칸을 채운다.
        val day = DayMood(LocalDate(2026, 11, 5), null, 7, moods = listOf(Mood.SICK, Mood.MEH))

        assertEquals(Mood.MEH, moodBuckets(StatsPeriod.All, listOf(day)).single().mood)
        assertEquals(Mood.MEH, moodBuckets(StatsPeriod.Week.of(day.date), listOf(day)).firstNotNullOf { it.mood })
    }

    @Test
    fun counts_every_entry_of_the_cell() {
        val day = DayMood(LocalDate(2026, 11, 5), Mood.MEH, 3, moods = listOf(Mood.SICK, Mood.SICK, Mood.MEH))

        assertEquals(Mood.SICK, moodBuckets(StatsPeriod.All, listOf(day)).single().mood)
    }

    @Test
    fun week_shows_seven_days_and_year_twelve_months() {
        assertEquals(7, moodBuckets(StatsPeriod.Week.of(today), emptyList()).size)
        assertEquals(12, moodBuckets(StatsPeriod.Year(2026), emptyList()).size)
        assertEquals(0, moodBuckets(StatsPeriod.All, emptyList()).size)
    }

    @Test
    fun all_time_shows_each_year_and_keeps_gaps() {
        val days = listOf(DayMood(LocalDate(2024, 3, 1), Mood.SAD, 1), DayMood(LocalDate(2026, 3, 1), Mood.GOOD, 1))

        val buckets = moodBuckets(StatsPeriod.All, days)

        assertEquals(listOf(Mood.SAD, null, Mood.GOOD), buckets.map { it.mood })
    }

    private object NoStats : StatsRepository {
        override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> = flowOf(MoodCounts(emptyMap(), 0))

        override fun advanced(period: AdvancedPeriod, today: LocalDate) = flowOf(advancedStats(emptyList()))

        override fun moodFlow(period: StatsPeriod): Flow<List<DayMood>> = flowOf(emptyList())

        override fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>> = flowOf(emptyMap())
    }
}
