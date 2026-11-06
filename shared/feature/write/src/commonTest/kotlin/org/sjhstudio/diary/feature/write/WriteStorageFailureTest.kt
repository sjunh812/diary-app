package org.sjhstudio.diary.feature.write

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.TestClock

// 저장 공간이 차서 임시 저장본을 쓰지 못해도 앱이 꺼지지 않고, 쓴 글이 화면에 남아야 한다(헌법 I).
@OptIn(ExperimentalCoroutinesApi::class)
class WriteStorageFailureTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock(Instant.parse("2026-11-01T12:00:00Z"))
    private val repository = FakeDraftRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(target: WriteTarget = WriteTarget.New(), drafts: FakeDraftRepository = repository) =
        WriteViewModel(target, drafts, clock, TimeZone.UTC)

    private fun TestScope.events(vm: WriteViewModel): MutableList<WriteEvent> {
        val out = mutableListOf<WriteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(out) }
        return out
    }

    private fun photo(name: String) = DraftPhoto("$name.jpg", name, 10, 10, 1)

    private fun draft(key: DraftKey, vararg photos: DraftPhoto) = Draft(
        key,
        LocalDate(2026, 11, 1),
        LocalTime(9, 0),
        "<p>글</p>",
        null,
        null,
        null,
        false,
        false,
        clock.now(),
        photos.toList()
    )

    @Test
    fun a_failed_autosave_keeps_the_text_and_says_so() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        repository.failWrites = true

        vm.onBodyChange("<p>날아가면 안 되는 글</p>")
        advanceUntilIdle()

        assertTrue(vm.state.value.saveFailed)
        assertEquals("<p>날아가면 안 되는 글</p>", vm.currentBody())
    }

    @Test
    fun a_later_autosave_writes_what_failed_and_clears_the_notice() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        repository.failWrites = true
        vm.onBodyChange("<p>글</p>")
        advanceUntilIdle()

        repository.failWrites = false
        vm.onBodyChange("<p>글 더</p>")
        advanceUntilIdle()

        assertFalse(vm.state.value.saveFailed)
        assertEquals("<p>글 더</p>", repository.saved.single().bodyHtml)
    }

    @Test
    fun going_to_the_background_with_a_full_disk_stays_quiet() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        repository.failWrites = true

        vm.onStop()
        advanceUntilIdle()

        assertTrue(vm.state.value.saveFailed)
    }

    @Test
    fun closing_stays_on_the_screen_when_the_draft_cannot_be_kept() = runTest(dispatcher) {
        val vm = vm()
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        repository.failWrites = true

        vm.close()
        advanceUntilIdle()

        assertTrue(events.isEmpty(), "저장본이 없는데 닫으면 글을 잃는다")
        assertTrue(vm.state.value.saveFailed)

        repository.failWrites = false
        vm.close()
        advanceUntilIdle()
        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
    }

    @Test
    fun adding_photos_with_a_full_disk_says_there_is_no_room() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        repository.failWrites = true

        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()

        assertEquals(PhotoNotice.NoSpace, vm.state.value.photoNotice)
    }

    @Test
    fun removing_or_moving_a_photo_that_cannot_be_written_keeps_the_photos() = runTest(dispatcher) {
        val drafts = FakeDraftRepository(editable = mapOf("e1" to draft(DraftKey.Edit("e1"), photo("a"), photo("b"))))
        val vm = vm(WriteTarget.Edit("e1"), drafts)
        advanceUntilIdle()
        drafts.failWrites = true

        vm.removePhoto(0)
        advanceUntilIdle()
        assertTrue(vm.state.value.saveFailed)

        vm.movePhoto(0, 1)
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), vm.state.value.photos.map { it.sha256 })
    }

    @Test
    fun a_failed_discard_stays_on_the_screen() = runTest(dispatcher) {
        val vm = vm()
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        advanceTimeBy(2_000)
        repository.failWrites = true

        vm.discard()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertTrue(vm.state.value.saveFailed)
    }

    @Test
    fun a_failed_discard_of_the_offered_draft_keeps_the_offer() = runTest(dispatcher) {
        val drafts = FakeDraftRepository(listOf(draft(DraftKey.New())))
        val vm = vm(drafts = drafts)
        advanceUntilIdle()
        assertTrue(vm.state.value.resume != null)
        drafts.failWrites = true

        vm.discardOffered()
        advanceUntilIdle()

        assertTrue(vm.state.value.resume != null, "지우지 못했으면 시트가 그대로 남는다")
        assertTrue(vm.state.value.saveFailed)
    }
}
