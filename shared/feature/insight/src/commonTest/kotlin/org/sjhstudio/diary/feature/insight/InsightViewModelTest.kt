package org.sjhstudio.diary.feature.insight

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.advancedStats
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository

@OptIn(ExperimentalCoroutinesApi::class)
class InsightViewModelTest {
    private var today = LocalDate(2026, 10, 31)

    private val dispatcher = StandardTestDispatcher()

    private val october = StatsPeriod.Month(YearMonth(2026, 10))
    private val september = StatsPeriod.Month(YearMonth(2026, 9))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 2025년 1월부터 일기가 있어 지난 기간으로 넘어감 */
    private val written = entriesOn(LocalDate(2025, 1, 2))

    private fun viewModel() = testInsightViewModel(NoStats, { today }, saved = SavedStateHandle(), entries = written)

    private fun entriesOn(vararg dates: LocalDate) = FakeEntryRepository(dates.associateWith { DayMood(it, Mood.GOOD, 1) })

    /** 고급 통계는 Plus만. 기존 통계는 어느 요금제든 그대로(FR-012) */
    @Test
    fun trying_what_the_tip_says_closes_the_tip_for_good() = runTest(dispatcher) {
        val done = mutableListOf<ContextTip>()
        val tips =
            object : ContextTips {
                override fun isOpen(tip: ContextTip): Boolean = true

                override suspend fun load() = Unit

                override fun done(tip: ContextTip) {
                    done += tip
                }
            }
        val model = testInsightViewModel(NoStats, { today }, saved = SavedStateHandle(), tips = tips)
        model.tried()
        advanceUntilIdle()
        assertEquals(listOf(ContextTip.INSIGHT_OPEN), done)
    }

    @Test
    fun advanced_stats_open_only_on_plus() = runTest(dispatcher) {
        val table = mapOf(Plan.FREE to false, Plan.AD_FREE to false, Plan.PLUS to true)
        for ((plan, open) in table) {
            val model = testInsightViewModel(NoStats, { today }, FakePlanRepository(plan), SavedStateHandle())
            advanceUntilIdle()

            assertEquals(open, model.advancedOpen.value, "$plan")
            assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value, "$plan")
        }
    }

    /** 요금제를 끝내 모르면(오프라인) 기다린 뒤 지금 값으로 정함 */
    @Test
    fun an_unknown_plan_falls_back_after_the_wait() = runTest(dispatcher) {
        val model = testInsightViewModel(NoStats, { today }, FakePlanRepository(Plan.FREE, known = false), SavedStateHandle())
        advanceUntilIdle()

        assertEquals(false, model.advancedAllowed())
    }

    /** 구독이 끝나면 칸이 바로 잠기고 앱 틀이 고급 통계 화면을 닫음 */
    @Test
    fun advanced_stats_close_when_plus_ends() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.PLUS)
        val model = testInsightViewModel(NoStats, { today }, plans, SavedStateHandle())
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
    fun does_not_move_past_today() = runTest(dispatcher) {
        val model = viewModel()
        runCurrent()

        model.next()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value)

        model.previous()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 9)), model.period.value)

        model.next()
        assertEquals(StatsPeriod.Month(YearMonth(2026, 10)), model.period.value)
    }

    /** 켜 둔 채 달이 바뀌면 이번 달을 보던 사람은 새 달을 봄 */
    @Test
    fun a_new_month_moves_the_current_period_along() {
        val model = viewModel()

        today = LocalDate(2026, 11, 1)
        model.refreshToday()

        assertEquals(today, model.todayState.value)
        assertEquals(StatsPeriod.Month(YearMonth(2026, 11)), model.period.value)
    }

    /** 켜 둔 채 해가 바뀌면 올해를 보던 사람은 새해를 보고, 새해 첫 주는 지난해 마지막 주와 이어짐 */
    @Test
    fun a_new_year_moves_this_year_and_this_week_along() = runTest(dispatcher) {
        today = LocalDate(2026, 12, 31)
        val model = viewModel()
        runCurrent()
        model.select(InsightKind.Year)

        today = LocalDate(2027, 1, 1)
        model.refreshToday()
        assertEquals(StatsPeriod.Year(2027), model.period.value)

        model.select(InsightKind.Week)
        val week = model.period.value as StatsPeriod.Week
        assertEquals(LocalDate(2026, 12, 27), week.start)
        model.previous()
        assertEquals(LocalDate(2026, 12, 20), (model.period.value as StatsPeriod.Week).start)
        model.next()
        model.next()
        assertEquals(week, model.period.value)
    }

    @Test
    fun a_new_day_leaves_a_past_period_alone() = runTest(dispatcher) {
        val model = viewModel()
        runCurrent()
        model.previous()

        today = LocalDate(2026, 11, 1)
        model.refreshToday()

        assertEquals(today, model.todayState.value)
        assertEquals(september, model.period.value)
    }

    /** 새 기간 이름 아래에 앞 기간 숫자가 보이지 않음 */
    @Test
    fun a_new_period_hides_the_numbers_of_the_last_one() = runTest(dispatcher) {
        val counts = MutableStateFlow<Map<StatsPeriod, MoodCounts>>(mapOf(october to MoodCounts(mapOf(Mood.GOOD to 3), 0)))
        val model = testInsightViewModel(SlowStats(counts), { today }, saved = SavedStateHandle(), entries = written)
        backgroundScope.launch { model.state.collect() }
        advanceUntilIdle()
        assertEquals(3, model.state.value.data?.counts?.total)

        model.previous()
        advanceUntilIdle()

        assertEquals(InsightState(september, null), model.state.value)

        counts.value += september to MoodCounts(mapOf(Mood.SAD to 1), 0)
        advanceUntilIdle()

        assertEquals(september, model.state.value.period)
        assertEquals(1, model.state.value.data?.counts?.total)
    }

    /** 숫자를 읽지 못하면 스켈레톤으로 남지 않고 다시 시도를 보이고, 다시 시도하면 다시 읽음 */
    @Test
    fun a_failed_read_shows_retry_and_reads_again() = runTest(dispatcher) {
        var fail = true
        val stats =
            object : StatsRepository by NoStats {
                override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> = flow {
                    if (fail) throw IllegalStateException("disk")
                    emit(MoodCounts(mapOf(Mood.GOOD to 2), 0))
                }
            }
        val model = testInsightViewModel(stats, { today }, saved = SavedStateHandle())
        backgroundScope.launch { model.state.collect() }
        advanceUntilIdle()
        assertTrue(model.state.value.failed)
        assertNull(model.state.value.data)

        fail = false
        model.retry()
        advanceUntilIdle()

        assertFalse(model.state.value.failed)
        assertEquals(2, model.state.value.data?.counts?.total)
    }

    /** 다른 탭에 다녀와 다시 읽어도 보던 숫자가 비워지지 않음 */
    @Test
    fun coming_back_keeps_the_numbers_while_reading_again() = runTest(dispatcher) {
        val model = testInsightViewModel(NoStats, { today }, saved = SavedStateHandle())
        val watching = backgroundScope.launch { model.state.collect() }
        advanceUntilIdle()
        val read = model.state.value.data
        assertEquals(0, read?.counts?.total)

        watching.cancel()
        advanceTimeBy(STOP_WATCHING_MILLIS)
        backgroundScope.launch { model.state.collect() }
        runCurrent()

        assertEquals(read, model.state.value.data)
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
        // 2026년 10월 1일은 목요일이라 첫 주는 3일까지
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
    fun each_cell_knows_the_dates_it_covers() {
        // 2026년 10월 1일은 목요일이라 첫 주는 1~3일, 마지막 주는 25~31일
        val october = moodBuckets(StatsPeriod.Month(YearMonth(2026, 10)), listOf(DayMood(LocalDate(2026, 10, 31), null, 1)))
        assertEquals(DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 3)), october[0].range)
        assertEquals(DateRange(LocalDate(2026, 10, 4), LocalDate(2026, 10, 10)), october[1].range)
        assertEquals(DateRange(LocalDate(2026, 10, 25), LocalDate(2026, 10, 31)), october[4].range)

        val year = moodBuckets(StatsPeriod.Year(2026), emptyList())
        assertEquals(DateRange(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28)), year[1].range)

        val all =
            moodBuckets(StatsPeriod.All, listOf(DayMood(LocalDate(2024, 3, 1), Mood.SAD, 1), DayMood(LocalDate(2026, 3, 1), Mood.GOOD, 1)))
        assertEquals(DateRange(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31)), all[1].range)

        val day = LocalDate(2026, 11, 5)
        assertEquals(DateRange(day, day), moodBuckets(StatsPeriod.Week.of(day), emptyList()).single { it.range.start == day }.range)
    }

    @Test
    fun only_cells_with_entries_can_be_opened() {
        // 기분을 고르지 않은 일기만 있는 칸도 열 수 있음
        val october = moodBuckets(StatsPeriod.Month(YearMonth(2026, 10)), listOf(DayMood(LocalDate(2026, 10, 31), null, 1)))
        assertEquals(listOf(false, false, false, false, true), october.map { it.written })

        val all =
            moodBuckets(StatsPeriod.All, listOf(DayMood(LocalDate(2024, 3, 1), Mood.SAD, 1), DayMood(LocalDate(2026, 3, 1), Mood.GOOD, 1)))
        assertEquals(listOf(true, false, true), all.map { it.written })
    }

    @Test
    fun tie_uses_the_later_day() {
        val days = listOf(DayMood(LocalDate(2026, 10, 5), Mood.SAD, 1), DayMood(LocalDate(2026, 10, 6), Mood.GOOD, 1))

        assertEquals(Mood.GOOD, moodBuckets(StatsPeriod.Month(YearMonth(2026, 10)), days)[1].mood)
    }

    @Test
    fun last_entry_without_mood_does_not_empty_the_cell() {
        // 마지막 일기에 기분이 없어도 앞 일기의 기분으로 칸을 채움
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

    /** 넣어 준 기간만 숫자를 내놓음 */
    private class SlowStats(private val counts: StateFlow<Map<StatsPeriod, MoodCounts>>) : StatsRepository by NoStats {
        override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> = counts.mapNotNull { it[period] }
    }

    private object NoStats : StatsRepository {
        override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> = flowOf(MoodCounts(emptyMap(), 0))

        override fun advanced(period: AdvancedPeriod, today: LocalDate) = flowOf(advancedStats(emptyList()))

        override fun moodFlow(period: StatsPeriod): Flow<List<DayMood>> = flowOf(emptyList())

        override fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>> = flowOf(emptyMap())
    }

    /** 기간 고르기에서 고른 기간으로 바로 감. 오늘 뒤 기간은 받지 않음 */
    @Test
    fun picking_a_period_jumps_there_but_not_past_today() {
        val model = viewModel()

        model.jump(StatsPeriod.Month(YearMonth(2024, 3)))
        assertEquals(StatsPeriod.Month(YearMonth(2024, 3)), model.period.value)

        model.jump(StatsPeriod.Week(LocalDate(2026, 10, 25)))
        assertEquals(StatsPeriod.Week(LocalDate(2026, 10, 25)), model.period.value)

        model.jump(StatsPeriod.Month(YearMonth(2026, 11)))
        model.jump(StatsPeriod.Year(2027))
        assertEquals(StatsPeriod.Week(LocalDate(2026, 10, 25)), model.period.value)
    }

    /** 화살표와 기간 고르기는 첫 일기가 든 달의 1일부터 */
    @Test
    fun the_first_day_comes_from_the_oldest_entry() = runTest(dispatcher) {
        val model = testInsightViewModel(NoStats, { today }, entries = entriesOn(LocalDate(2023, 5, 2), LocalDate(2021, 12, 31)))
        runCurrent()

        assertEquals(LocalDate(2021, 12, 1), model.first.value)
    }

    /** 달 목록을 읽기 전에는 첫날을 모름. 이번 달 1일로 두면 ‹ 화살표가 잠깐 꺼짐으로 보임 */
    @Test
    fun the_first_day_is_unknown_until_months_are_read() = runTest(dispatcher) {
        val model = testInsightViewModel(NoStats, { today }, entries = entriesOn(LocalDate(2023, 5, 2)))

        assertNull(model.first.value)
        runCurrent()
        assertEquals(LocalDate(2023, 5, 1), model.first.value)
    }

    /** 달 목록을 못 읽으면 첫날을 모름으로 두고, 다시 시도에서 다시 읽음 */
    @Test
    fun the_first_day_stays_unknown_when_months_fail_and_rereads_on_retry() = runTest(dispatcher) {
        var reads = 0
        val written = entriesOn(LocalDate(2023, 5, 2))
        val flaky =
            object : EntryRepository by written {
                override fun months(): Flow<Set<YearMonth>> =
                    if (reads++ == 0) flow { throw IllegalStateException("읽을 수 없음") } else written.months()
            }
        val model = testInsightViewModel(NoStats, { today }, entries = flaky)
        runCurrent()
        assertNull(model.first.value)

        model.retry()
        runCurrent()
        assertEquals(LocalDate(2023, 5, 1), model.first.value)
    }

    /** 3월·7월에만 일기가 있으면 화살표는 사이 빈 달까지 넘기고 3월에서 멈춤 */
    @Test
    fun arrows_stop_at_the_period_of_the_first_entry() = runTest(dispatcher) {
        today = LocalDate(2026, 11, 22)
        val model = testInsightViewModel(NoStats, { today }, entries = entriesOn(LocalDate(2026, 3, 10), LocalDate(2026, 7, 5)))
        runCurrent()

        val months = mutableListOf(model.period.value)
        repeat(10) {
            model.previous()
            months += model.period.value
        }
        assertEquals((11 downTo 3).map { StatsPeriod.Month(YearMonth(2026, it)) }, months.distinct())

        model.select(InsightKind.Week)
        repeat(60) { model.previous() }
        // 2026-03-01은 일요일이라 첫 주가 3월 1일에 시작
        assertEquals(StatsPeriod.Week(LocalDate(2026, 3, 1)), model.period.value)

        model.select(InsightKind.Year)
        model.previous()
        assertEquals(StatsPeriod.Year(2026), model.period.value)
    }

    /** 일기가 없거나 오늘 뒤 날짜에만 있으면 이번 기간뿐 */
    @Test
    fun without_past_entries_only_this_period_is_open() = runTest(dispatcher) {
        for (entries in listOf(FakeEntryRepository(), entriesOn(LocalDate(2027, 1, 5)))) {
            val model = testInsightViewModel(NoStats, { today }, entries = entries)
            runCurrent()

            assertEquals(LocalDate(2026, 10, 1), model.first.value)
            model.previous()
            assertEquals(october, model.period.value)
        }
    }

    /** 첫 일기보다 이른 기간을 보던 중이면 더 앞으로 가지 않고 뒤로는 감 */
    @Test
    fun a_period_before_the_first_entry_only_moves_forward() = runTest(dispatcher) {
        val model = testInsightViewModel(NoStats, { today }, entries = entriesOn(LocalDate(2026, 3, 10)))
        runCurrent()
        model.jump(StatsPeriod.Month(YearMonth(2025, 6)))

        model.previous()
        assertEquals(StatsPeriod.Month(YearMonth(2025, 6)), model.period.value)
        model.next()
        assertEquals(StatsPeriod.Month(YearMonth(2025, 7)), model.period.value)
    }

    /** 앱이 꺼졌다 돌아와도 보던 기간 유지 */
    @Test
    fun the_period_survives_the_app_being_killed() {
        for (period in listOf(
            StatsPeriod.Week(LocalDate(2026, 11, 1)),
            StatsPeriod.Month(YearMonth(2026, 2)),
            StatsPeriod.Year(2025),
            StatsPeriod.All
        )) {
            assertEquals(period, decodePeriod(encodePeriod(period)))
        }
        assertEquals(null, decodePeriod("x:1"))
    }
}

private const val STOP_WATCHING_MILLIS = 6_000L
