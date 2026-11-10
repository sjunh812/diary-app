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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.platform.PlaceFinder
import org.sjhstudio.diary.core.platform.PlaceLookup
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.TestClock

// 쓰기 화면 모델(스펙 002 T034, T032 일부). 쓴 글을 잃지 않는지를 먼저 봄
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
        testWriteViewModel(target, repository, clock, TimeZone.UTC)

    private fun draft(key: DraftKey = DraftKey.New(), body: String = "<p>쓰던 글</p>", mood: Mood? = null) = Draft(
        key, today, LocalTime(9, 0), body, mood, null, null, false, false, clock.now()
    )

    private fun TestScope.events(vm: WriteViewModel): MutableList<WriteEvent> {
        val out = mutableListOf<WriteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(out) }
        return out
    }

    @Test
    fun checklist_tool_follows_the_plan_and_closes_when_it_ends() = runTest(dispatcher) {
        val plans = object : PlanRepository {
            override val plan = MutableStateFlow(Plan.FREE)
        }
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        assertEquals(false, viewModel.checklistOpen.value)

        plans.plan.value = Plan.AD_FREE
        advanceUntilIdle()
        assertEquals(false, viewModel.checklistOpen.value)

        plans.plan.value = Plan.PLUS
        advanceUntilIdle()
        assertEquals(true, viewModel.checklistOpen.value)

        plans.plan.value = Plan.FREE
        advanceUntilIdle()
        assertEquals(false, viewModel.checklistOpen.value)
    }

    @Test
    fun photo_limit_follows_the_plan() = runTest(dispatcher) {
        val plans = object : PlanRepository {
            override val plan = MutableStateFlow(Plan.FREE)
        }
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        assertEquals(10, viewModel.photoLimit.value)

        plans.plan.value = Plan.PLUS
        advanceUntilIdle()

        assertEquals(30, viewModel.photoLimit.value)
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

    /** 글자를 치는 동안은 HTML로 바꾸지 않고, 입력이 멈춘 뒤 한 번만 바꿈 */
    @Test
    fun typing_converts_once_after_the_pause() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        val editor = FakeEditor(vm)

        editor.type("<p>가</p>")
        advanceTimeBy(500)
        editor.type("<p>가나</p>")
        advanceTimeBy(500)
        editor.type("<p>가나다</p>")
        assertEquals(0, editor.reads)
        assertTrue(vm.state.value.canSave)

        advanceUntilIdle()

        assertEquals(1, editor.reads)
        assertEquals("<p>가나다</p>", repository.saved.single().bodyHtml)
    }

    /** 앱이 꺼지기 전 마지막 기회. 기다리지 않고 바로 꺼내 씀(헌법 I) */
    @Test
    fun leaving_the_app_keeps_the_last_letter() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        val editor = FakeEditor(vm)

        editor.type("<p>마지막 글자</p>")
        vm.onStop()
        testScheduler.runCurrent()

        assertEquals("<p>마지막 글자</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun closing_right_after_typing_keeps_the_last_letter() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        val editor = FakeEditor(vm)

        editor.type("<p>마지막 글자</p>")
        vm.close()
        advanceUntilIdle()

        assertEquals("<p>마지막 글자</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun saving_right_after_typing_keeps_the_last_letter() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val body = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.draft(DraftKey.New()).collect { draft -> draft?.let { body += it.bodyHtml } }
        }
        advanceUntilIdle()
        val editor = FakeEditor(vm)

        editor.type("<p>마지막 글자</p>")
        vm.save()
        advanceUntilIdle()

        assertEquals(listOf("<p>마지막 글자</p>"), body)
        assertEquals(1, repository.committed.size)
    }

    /** 다른 화면이 쓰기를 가리면 편집기가 사라짐. 그 전에 받아 둔 글이 저장됨 */
    @Test
    fun an_editor_going_away_hands_over_the_last_letter() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        val editor = FakeEditor(vm)

        editor.type("<p>마지막 글자</p>")
        vm.detachEditor()
        editor.text = "<p>사라진 편집기</p>"
        advanceUntilIdle()

        assertEquals("<p>마지막 글자</p>", repository.saved.single().bodyHtml)
    }

    /** 글을 다 지운 뒤에는 변환 전이라도 빈 글이 저장되지 않음 */
    @Test
    fun blank_editor_cannot_be_saved_before_it_is_converted() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        val editor = FakeEditor(vm)
        editor.type("<p>글</p>")
        advanceUntilIdle()

        editor.type("<p></p>", hasText = false)
        assertFalse(vm.state.value.canSave)
        vm.save()
        advanceUntilIdle()

        assertTrue(repository.committed.isEmpty())
    }

    /**
     * 고친 글을 도로 지워 불러온 모양과 같아지면 원문 그대로 남음.
     * 화면을 돌린 뒤에도 옛 값으로 돌아가지 않음
     */
    @Test
    fun loading_again_compares_with_the_body_it_put_in() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, body = "<p>옛 글</p>")))
        val vm = vm(repository, WriteTarget.Edit("e1"))
        advanceUntilIdle()
        vm.onBodyChange("<p>새 글</p>")

        vm.onBodyLoaded("<p>편집기 모양</p>")
        vm.onBodyChange("<p>편집기 모양</p>")

        assertEquals("<p>새 글</p>", vm.currentBody())
    }

    @Test
    fun a_draft_left_before_a_restore_says_saving_keeps_both() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key)))
        repository.openEdit("e1")
        repository.changedByRestore += "e1"

        val vm = vm(repository, WriteTarget.Edit("e1"))
        advanceUntilIdle()

        assertTrue(vm.state.value.entryChanged)
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
    fun tapping_save_twice_commits_once() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.onBodyChange("<p>글</p>")
        vm.save()
        vm.save()
        advanceUntilIdle()

        assertEquals(1, repository.committed.size)
        assertEquals(listOf<WriteEvent>(WriteEvent.Saved("saved-id")), events)
    }

    @Test
    fun saving_stops_the_button_until_it_fails() = runTest(dispatcher) {
        val repository = FakeDraftRepository().apply { failCommit = true }
        val vm = vm(repository)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")

        vm.save()
        assertTrue(vm.state.value.saving)
        advanceUntilIdle()

        assertFalse(vm.state.value.saving)
        assertTrue(vm.state.value.saveFailed)
    }

    /** 저장을 마친 뒤에는 버튼을 다시 켜지 않음. 임시 저장본이 지워져 다시 저장할 것이 없음 */
    @Test
    fun a_finished_save_stays_finished() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        vm.save()
        advanceUntilIdle()

        vm.onBodyChange("<p>글 더</p>")
        advanceUntilIdle()
        vm.save()
        vm.close()
        advanceUntilIdle()

        assertTrue(vm.state.value.saving)
        assertEquals(1, repository.committed.size)
        assertTrue(repository.saved.isEmpty())
        assertEquals(listOf<WriteEvent>(WriteEvent.Saved("saved-id")), events)
    }

    @Test
    fun closing_while_saving_does_not_bring_the_draft_back() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")

        vm.save()
        vm.close()
        advanceUntilIdle()

        assertTrue(repository.saved.isEmpty())
        assertEquals(listOf<WriteEvent>(WriteEvent.Saved("saved-id")), events)
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
        assertEquals("<p>쓰던 글</p>", vm.currentBody())
        assertEquals(1, vm.state.value.bodyVersion)
    }

    /**
     * 고르지 않고 뒤로 가면 쓰던 글을 불러오지 않고 나감.
     * 글은 다음에 다시 물을 수 있게 그대로 남음
     */
    @Test
    fun leaving_the_offer_closes_without_loading_the_draft() = runTest(dispatcher) {
        val repository = FakeDraftRepository(listOf(draft()))
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()

        vm.leaveOffer()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = false)), events)
        assertEquals("", vm.currentBody())
        assertEquals(0, vm.state.value.bodyVersion)
        assertEquals("<p>쓰던 글</p>", repository.latestNew()?.bodyHtml)
    }

    /** [새로 쓰기]를 골라도 쓰던 글은 버리기 전까지 남음(FR-010) */
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

    /** 전에 수정하다 남겨 둔 임시 저장본을 손대지 않고 닫아도 그 내용은 남음 */
    @Test
    fun closing_a_resumed_edit_keeps_what_was_left() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val left = draft(key, body = "<p>고치던 글</p>")
        val repository = FakeDraftRepository(listOf(left), editable = mapOf("e1" to draft(key, body = "<p>원래 글</p>")))
        val vm = vm(repository, WriteTarget.Edit("e1"))
        val events = events(vm)
        advanceUntilIdle()

        vm.close()
        advanceUntilIdle()

        assertTrue(repository.discarded.isEmpty())
        assertEquals("<p>고치던 글</p>", repository.saved.single().bodyHtml)
        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
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
    fun a_place_cut_at_the_limit_keeps_its_emoji_whole() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        vm.setPlace("가".repeat(29) + "😀")

        assertEquals("가".repeat(29), vm.state.value.placeName)
    }

    @Test
    fun a_picker_failure_says_so() = runTest(dispatcher) {
        val vm = vm(FakeDraftRepository())
        advanceUntilIdle()

        vm.photoPickFailed()

        assertEquals(PhotoNotice.Unreadable, vm.state.value.photoNotice)
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

    @Test
    fun found_place_waits_for_done_and_is_not_saved_by_itself() = runTest(dispatcher) {
        val finder = OnePlace(PlaceLookup.Found("마포구 망원동"))
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, places = finder)
        advanceUntilIdle()

        viewModel.findPlace()
        advanceUntilIdle()

        assertEquals(PlaceLocating.Done(PlaceLookup.Found("마포구 망원동")), viewModel.state.value.locating)
        assertEquals("", viewModel.state.value.placeName)
    }

    @Test
    fun place_is_read_only_when_asked() = runTest(dispatcher) {
        val finder = OnePlace(PlaceLookup.NotFound)
        testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, places = finder)
        advanceUntilIdle()

        assertEquals(0, finder.calls)
    }

    @Test
    fun closing_the_sheet_stops_finding_and_clears_the_note() = runTest(dispatcher) {
        val finder = OnePlace(PlaceLookup.Denied, wait = true)
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, places = finder)
        advanceUntilIdle()

        viewModel.findPlace()
        viewModel.findPlace()
        advanceUntilIdle()
        assertEquals(PlaceLocating.Finding, viewModel.state.value.locating)
        assertEquals(1, finder.calls)

        viewModel.stopFindingPlace()
        advanceUntilIdle()
        assertEquals(PlaceLocating.Idle, viewModel.state.value.locating)
    }

    /** 편집기 흉내. HTML을 꺼낸 횟수를 셈 */
    private class FakeEditor(private val vm: WriteViewModel) {
        var text = ""
        var reads = 0

        init {
            vm.attachEditor {
                reads++
                text
            }
        }

        fun type(html: String, hasText: Boolean = true) {
            text = html
            vm.onEditorChanged(hasText)
        }
    }

    private class OnePlace(private val result: PlaceLookup, private val wait: Boolean = false) : PlaceFinder {
        var calls = 0

        override suspend fun find(): PlaceLookup {
            calls++
            if (wait) awaitCancellation()
            return result
        }
    }
}
