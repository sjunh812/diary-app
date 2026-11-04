package org.sjhstudio.diary.feature.write

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.TestClock

// 쓰기 화면 모델(스펙 002 T034, T032 일부). 쓴 글을 잃지 않는지를 먼저 본다.
@OptIn(ExperimentalCoroutinesApi::class)
class WriteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock(Instant.parse("2026-11-01T12:00:00Z"))
    private val today = LocalDate(2026, 11, 1)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repository: FakeDraftRepository, target: WriteTarget = WriteTarget.New()) =
        WriteViewModel(target, repository, clock, TimeZone.UTC)

    private fun draft(key: DraftKey = DraftKey.New(), body: String = "<p>쓰던 글</p>", mood: Mood? = null) = Draft(
        key, today, LocalTime(9, 0), body, mood, null, null, false, false, clock.now()
    )

    private fun TestScope.events(vm: WriteViewModel): MutableList<WriteEvent> {
        val out = mutableListOf<WriteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(out) }
        return out
    }

    @Test
    fun new_entry_starts_today_and_cannot_be_saved_empty() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        assertEquals(today, vm.state.value.date)
        assertEquals(LocalTime(12, 0), vm.state.value.time)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun a_chosen_date_is_kept() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository(), WriteTarget.New(LocalDate(2026, 10, 26)))
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 10, 26), vm.state.value.date)
    }

    @Test
    fun text_is_stored_one_second_after_typing_stops() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()

        vm.onBodyChange("<p>가</p>")
        advanceTimeBy(500)
        vm.onBodyChange("<p>가나</p>")
        advanceTimeBy(999)
        assertTrue(repository.saved.isEmpty())

        advanceTimeBy(2)
        assertEquals("<p>가나</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun leaving_the_app_stores_right_away() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()

        vm.onBodyChange("<p>가</p>")
        vm.onStop()
        testScheduler.runCurrent()

        assertEquals("<p>가</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun blank_text_makes_no_draft() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()

        vm.onBodyChange("<p>   </p>")
        advanceUntilIdle()

        assertTrue(repository.saved.isEmpty())
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun save_rule_accepts_text_mood_or_photo() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        vm.selectMood(Mood.GOOD)
        assertTrue(vm.state.value.canSave)
        vm.selectMood(Mood.GOOD)
        assertNull(vm.state.value.mood)
        assertFalse(vm.state.value.canSave)
    }

    @Test
    fun saving_commits_and_reports_the_entry() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.onBodyChange("<p>글</p>")
        vm.save()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Saved("saved-id")), events)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun failed_save_keeps_the_draft_and_tells_the_screen() = runTest(dispatcher) {
        val repository = FakeDraftRepository().apply { failCommit = true }
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.onBodyChange("<p>글</p>")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.state.value.saveFailed)
        assertTrue(events.isEmpty())
        assertEquals("<p>글</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun going_back_with_text_keeps_a_draft_without_asking() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.onBodyChange("<p>글</p>")
        vm.close()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun going_back_empty_leaves_nothing() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.close()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = false)), events)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun an_existing_draft_is_offered_and_continues() = runTest(dispatcher) {
        val repository = FakeDraftRepository(listOf(draft()))
        val vm = vm(repository)
        advanceUntilIdle()
        assertEquals("쓰던 글", vm.state.value.resume?.preview)

        vm.continueDraft()
        advanceUntilIdle()

        assertNull(vm.state.value.resume)
        assertEquals("<p>쓰던 글</p>", vm.state.value.initialBody)
        assertEquals(1, vm.state.value.bodyVersion)
    }

    /** [새로 쓰기]를 골라도 쓰던 글은 버리기 전까지 남는다(FR-010). */
    @Test
    fun starting_fresh_keeps_the_old_draft() = runTest(dispatcher) {
        val repository = FakeDraftRepository(listOf(draft()))
        val vm = vm(repository)
        advanceUntilIdle()

        vm.startFresh()
        vm.onBodyChange("<p>새 글</p>")
        advanceUntilIdle()

        assertEquals(setOf("<p>쓰던 글</p>", "<p>새 글</p>"), repository.saved.map { it.bodyHtml }.toSet())
        assertNotEquals(repository.saved[0].key, repository.saved[1].key)
    }

    @Test
    fun discarding_the_offered_draft_starts_empty() = runTest(dispatcher) {
        val repository = FakeDraftRepository(listOf(draft()))
        val vm = vm(repository)
        advanceUntilIdle()

        vm.discardOffered()
        advanceUntilIdle()

        assertNull(vm.state.value.resume)
        assertFalse(vm.state.value.loading)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun editing_loads_the_entry_and_unchanged_back_leaves_no_draft() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key)))
        val vm = vm(repository, WriteTarget.Edit("e1"))
        val events = events(vm)
        advanceUntilIdle()
        assertTrue(vm.state.value.isEdit)

        clock.advance(1.hours)
        vm.close()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = false)), events)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun a_missing_entry_closes_the_screen() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository(), WriteTarget.Edit("gone"))
        val events = events(vm)
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = false)), events)
    }

    @Test
    fun moving_to_another_day_clears_only_the_filled_in_weather() = runTest(dispatcher) {
        val repository =
            FakeDraftRepository(listOf(draft(mood = Mood.GOOD).copy(weather = Weather.RAIN, weatherAutoFilled = true)))
        val vm = vm(repository)
        advanceUntilIdle()
        vm.continueDraft()
        advanceUntilIdle()

        vm.setDate(LocalDate(2026, 10, 30))

        assertNull(vm.state.value.weather)
        assertEquals(Mood.GOOD, vm.state.value.mood)
    }

    @Test
    fun a_chosen_weather_stays_when_the_date_moves() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        vm.selectWeather(Weather.RAIN)
        vm.setDate(LocalDate(2026, 10, 30))

        assertEquals(Weather.RAIN, vm.state.value.weather)
    }

    @Test
    fun photos_arrive_from_the_store_and_limit_is_noticed_without_blocking() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()

        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.photos.size)
        assertTrue(vm.state.value.canSave)

        repository.nextAddResult = AddPhotosResult.LimitReached(10, canUpgrade = true, added = 0)
        vm.addPhotos(listOf(PickedImage { byteArrayOf(2) }))
        advanceUntilIdle()
        assertIs<PhotoNotice.Limit>(vm.state.value.photoNotice)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun place_name_is_cut_at_thirty_letters() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        vm.setPlace("가".repeat(40))

        assertEquals(30, vm.state.value.placeName.length)
    }

    @Test
    fun photo_stays_when_text_is_stored_later() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()

        vm.onBodyChange("<p>글</p>")
        advanceUntilIdle()

        val stored = assertNotNull(repository.draft(repository.saved.single().key).first())
        assertEquals(1, stored.photos.size)
    }
}
