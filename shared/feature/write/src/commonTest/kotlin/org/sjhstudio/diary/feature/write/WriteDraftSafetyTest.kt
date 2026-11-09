package org.sjhstudio.diary.feature.write

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.TestClock

// 앱이 종료되거나 잠겼다 돌아와도, 연타해도 쓴 글을 잃지 않는지 본다(헌법 I).
@OptIn(ExperimentalCoroutinesApi::class)
class WriteDraftSafetyTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock(Instant.parse("2026-11-01T12:00:00Z"))
    private val today = LocalDate(2026, 11, 1)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repository: FakeDraftRepository, target: WriteTarget = WriteTarget.New()) =
        WriteViewModel(target, repository, clock, TimeZone.UTC)

    private fun draft(key: DraftKey = DraftKey.New(), body: String = "<p>쓰던 글</p>") =
        Draft(key, today, LocalTime(9, 0), body, null, null, null, false, false, clock.now())

    private fun TestScope.events(vm: WriteViewModel): MutableList<WriteEvent> {
        val out = mutableListOf<WriteEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(out) }
        return out
    }

    @Test
    fun a_photo_arriving_before_the_draft_is_read_keeps_the_draft() = runTest(dispatcher) {
        val key = DraftKey.New("new-1")
        val repository = FakeDraftRepository(listOf(draft(key, body = "<p>남겨 둔 글</p>")))
        // 카메라에서 돌아온 결과가 화면 모델이 글을 읽기 전에 들어온다.
        val vm = WriteViewModel(WriteTarget.New(), repository, clock, TimeZone.UTC, restoredKey = key.value)
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals("<p>남겨 둔 글</p>", saved.bodyHtml)
        assertEquals(today, saved.date)
        assertEquals(1, saved.photos.size)
        assertEquals(null, vm.state.value.resume)
    }

    @Test
    fun a_restored_screen_continues_without_asking_again() = runTest(dispatcher) {
        val key = DraftKey.New("new-1")
        val repository = FakeDraftRepository(listOf(draft(key), draft(DraftKey.New("new-2"))))

        val vm = WriteViewModel(WriteTarget.New(), repository, clock, TimeZone.UTC, restoredKey = key.value)
        advanceUntilIdle()

        assertEquals(null, vm.state.value.resume)
        assertEquals(key.value, vm.state.value.draftKey)
    }

    @Test
    fun discarding_from_the_offer_does_not_overwrite_another_draft() = runTest(dispatcher) {
        clock.advance(1.seconds)
        val older = draft(DraftKey.New(), body = "<p>글 A</p>")
        clock.advance(1.seconds)
        val newer = draft(DraftKey.New("new-2"), body = "<p>글 B</p>")
        val repository = FakeDraftRepository(listOf(older, newer))
        val vm = vm(repository)
        advanceUntilIdle()

        vm.discardOffered()
        advanceUntilIdle()
        vm.onBodyChange("<p>새 글</p>")
        advanceTimeBy(2_000)
        advanceUntilIdle()

        assertEquals(setOf("<p>글 A</p>", "<p>새 글</p>"), repository.saved.map { it.bodyHtml }.toSet())
    }

    @Test
    fun starting_fresh_empties_the_editor() = runTest(dispatcher) {
        val repository = FakeDraftRepository(listOf(draft()))
        val vm = vm(repository)
        advanceUntilIdle()
        val before = vm.state.value.bodyVersion

        vm.startFresh()

        assertEquals(before + 1, vm.state.value.bodyVersion)
        assertEquals("", vm.currentBody())
    }

    @Test
    fun closing_twice_closes_once() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")

        vm.close()
        vm.close()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
    }

    @Test
    fun a_left_edit_that_matches_the_entry_is_not_kept() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key)))
        // 앱이 다시 떠서 전에 만든 저장본을 이어 연 때다.
        repository.openEdit("e1")
        val vm = vm(repository, WriteTarget.Edit("e1"))
        val events = events(vm)
        advanceUntilIdle()

        vm.close()
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = false)), events)
        assertEquals(listOf<DraftKey>(key), repository.discarded)
    }

    @Test
    fun closing_waits_for_photos_being_added() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDraftRepository().apply { addGate = gate }
        val vm = vm(repository)
        val events = events(vm)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()
        assertTrue(vm.state.value.addingPhotos)

        vm.close()
        advanceUntilIdle()
        assertTrue(events.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.saved.single().photos.size)
        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
        assertFalse(vm.state.value.addingPhotos)
    }

    /** Room은 사진을 적은 뒤 조금 늦게 알린다. 화면이 아직 모른다고 빈 글로 보면 사진을 잃는다. */
    @Test
    fun a_photo_the_screen_has_not_seen_yet_is_kept_on_closing() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val fake = FakeDraftRepository().apply { addGate = gate }
        var reads = 0
        val slow =
            object : DraftRepository by fake {
                override fun draft(key: DraftKey): Flow<Draft?> = if (reads++ == 0) emptyFlow() else fake.draft(key)
            }
        val vm = WriteViewModel(WriteTarget.New(), slow, clock, TimeZone.UTC)
        val events = events(vm)
        advanceUntilIdle()
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
        advanceUntilIdle()

        vm.close()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf<WriteEvent>(WriteEvent.Closed(keptDraft = true)), events)
    }

    @Test
    fun a_double_tap_on_remove_takes_one_photo() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }, PickedImage { byteArrayOf(2) }))
        advanceUntilIdle()

        vm.removePhoto(0)
        vm.removePhoto(0)
        advanceUntilIdle()
        assertEquals(1, repository.saved.single().photos.size)

        clock.advance(1.seconds)
        vm.removePhoto(0)
        advanceUntilIdle()
        assertEquals(0, repository.saved.single().photos.size)
    }

    // 잘못 뺀 사진은 토스트의 [되돌리기]로 원래 자리에 돌아온다(2026-11-10).
    @Test
    fun undo_puts_a_removed_photo_back_in_place() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val vm = vm(repository)
        advanceUntilIdle()
        vm.onBodyChange("<p>글</p>")
        vm.addPhotos(listOf(PickedImage { byteArrayOf(1) }, PickedImage { byteArrayOf(2) }, PickedImage { byteArrayOf(3) }))
        advanceUntilIdle()
        val before = repository.saved.single().photos

        vm.removePhoto(1)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.photoRemoved)
        assertEquals(2, repository.saved.single().photos.size)

        vm.undoRemovePhoto()
        advanceUntilIdle()
        assertEquals(before, repository.saved.single().photos)

        // 한 번 되돌린 뒤에는 다시 눌러도 아무것도 하지 않는다.
        vm.undoRemovePhoto()
        advanceUntilIdle()
        assertEquals(before, repository.saved.single().photos)
    }

    @Test
    fun discarding_shows_only_for_a_draft_left_before() = runTest(dispatcher) {
        val key = DraftKey.Edit("e1")
        val fresh = vm(FakeDraftRepository(editable = mapOf("e1" to draft(key))), WriteTarget.Edit("e1"))
        advanceUntilIdle()
        fresh.selectMood(Mood.entries.first())
        assertFalse(fresh.state.value.canDiscard)

        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key)))
        repository.openEdit("e1")
        val left = vm(repository, WriteTarget.Edit("e1"))
        advanceUntilIdle()
        assertTrue(left.state.value.canDiscard)
    }
}
