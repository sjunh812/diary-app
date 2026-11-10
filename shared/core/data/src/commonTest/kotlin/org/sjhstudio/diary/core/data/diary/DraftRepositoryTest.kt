package org.sjhstudio.diary.core.data.diary

import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.platform.ImageSize
import org.sjhstudio.diary.core.testing.TestClock

// 임시 저장과 사진(스펙 002 T032, T033)
// 일기를 잃지 않는지 봄(헌법 I)
class DraftRepositoryTest {
    private val db = memoryDatabase()
    private val files = FakeFileSystem()
    private val clock = TestClock(T0)
    private var limit = PhotoLimit.FREE
    private val repository = DefaultDraftRepository(db, photoStore(files), clock) { limit }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun image(text: String) = PickedImage { text.encodeToByteArray() }

    /** 하루 지난 사진을 다시 고르면 고친 시각이 새로워져, 쓰지 않는 사진 정리가 방금 고른 사진을 지우지 않음 */
    @Test
    fun picking_an_old_photo_again_refreshes_its_time() = runTest {
        var now = Instant.parse("2026-11-01T00:00:00Z")
        val fs = FakeFileSystem(
            clock = object : Clock {
                override fun now(): Instant = now
            }
        )
        val store = photoStore(fs)
        val first = store.store(byteArrayOf(1, 2, 3))!!

        now += 2.days
        val again = store.store(byteArrayOf(1, 2, 3))!!

        assertEquals(first.fileName, again.fileName)
        assertEquals(now.toEpochMilliseconds(), fs.metadata(MEDIA_DIR / again.fileName).lastModifiedAtMillis)
    }

    @Test
    fun a_saved_draft_comes_back_after_reopening() = runTest {
        repository.save(newDraft("<p>쓰던 글</p>", Mood.GOOD))

        val reopened = DefaultDraftRepository(db, photoStore(files), clock)

        assertEquals("<p>쓰던 글</p>", reopened.draft(DraftKey.New()).first()?.bodyHtml)
    }

    @Test
    fun commit_writes_entry_and_photos_and_removes_the_draft() = runTest {
        repository.save(newDraft("<p><strong>굵게</strong> 글</p>"))
        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b")))

        val id = repository.commit(DraftKey.New())

        val entry = assertNotNull(db.entryDao().get(id))
        assertEquals("<p><b>굵게</b> 글</p>", entry.bodyHtml)
        assertEquals("굵게 글", entry.plainText)
        assertEquals(listOf(0, 1), db.mediaDao().forEntry(id).map { it.sortOrder })
        assertNull(repository.draft(DraftKey.New()).first())
    }

    @Test
    fun failed_commit_keeps_the_draft_and_writes_nothing() = runTest {
        repository.save(newDraft())
        val draft = assertNotNull(db.draftDao().get(DraftKey.New().value))
        val row = entry("e1").asEntity()
        // 같은 순서 번호 두 줄로 사진 쓰기를 실패시킴
        val photos = listOf(photo("p1", "e1", 0).asEntity(), photo("p2", "e1", 0).asEntity())

        assertFailsWith<SQLiteException> { db.draftDao().commit(draft.key, row, photos, db.entryDao(), db.mediaDao()) }

        assertNull(db.entryDao().get("e1"))
        assertEquals(draft, db.draftDao().get(DraftKey.New().value))
    }

    @Test
    fun commit_without_a_draft_fails_and_writes_nothing() = runTest {
        assertFailsWith<IllegalArgumentException> { repository.commit(DraftKey.New()) }
        assertEquals(0, db.entryDao().countAll())
    }

    @Test
    fun editing_keeps_missing_photos_and_legacy_fields() = runTest {
        db.seed(entry("e1").copy(legacyId = 7, legacyCreateDate = "2020-01-01"))
        db.seedPhotos(
            photo("p1", "e1", 0, legacyPath = "/old/p1.jpg"),
            photo("gone", "e1", 1, MediaStatus.MISSING, legacyPath = "/old/gone.jpg")
        )
        val draft = assertNotNull(repository.openEdit("e1")).draft
        assertEquals(1, draft.photos.size)

        clock.advance(1.hours)
        repository.save(draft.copy(bodyHtml = "<p>고침</p>"))
        repository.commit(DraftKey.Edit("e1"))

        val saved = assertNotNull(db.entryDao().get("e1")).asModel()
        assertEquals(7, saved.legacyId)
        assertEquals(T0, saved.createdAt)
        assertEquals(T0 + 1.hours, saved.updatedAt)
        val rows = db.mediaDao().forEntry("e1")
        assertEquals(listOf("p1", "gone"), rows.map { it.id })
        assertEquals("/old/p1.jpg", rows[0].legacyPath)
        assertEquals(MediaStatus.MISSING.id, rows[1].status)
    }

    /** 고치던 글을 남겨 둔 사이에 복원이 일기를 바꾼 상황을 만듦 */
    private suspend fun leftDraftThenRestore(id: String, merged: String = "합쳐 온 글"): org.sjhstudio.diary.core.model.Draft {
        val draft = assertNotNull(repository.openEdit(id)).draft
        db.entryDao().update(entry(id, text = merged).asEntity())
        db.draftDao().markEntryChanged(id)
        return draft
    }

    @Test
    fun saving_a_left_draft_after_restore_keeps_both_versions() = runTest {
        db.seed(entry("e1"))
        db.seedPhotos(photo("p1", "e1", 0))
        val draft = leftDraftThenRestore("e1")
        repository.save(draft.copy(bodyHtml = "<p>남겨 둔 글</p>"))

        clock.advance(1.hours)
        val saved = repository.commit(DraftKey.Edit("e1"))

        assertTrue(saved != "e1")
        assertEquals("합쳐 온 글", assertNotNull(db.entryDao().get("e1")).plainText)
        assertEquals("남겨 둔 글", assertNotNull(db.entryDao().get(saved)).plainText)
        assertEquals(listOf("p1"), db.mediaDao().forEntry("e1").map { it.id })
        assertEquals(1, db.mediaDao().forEntry(saved).size)
        assertEquals(setOf("e1", saved), db.backupDao().marks().first().map { it.entryId }.toSet())
        assertNull(db.draftDao().get("e1"))
    }

    @Test
    fun a_left_draft_that_matches_the_restored_entry_adds_no_copy() = runTest {
        db.seed(entry("e1"))
        val draft = leftDraftThenRestore("e1")
        repository.save(draft.copy(bodyHtml = "<p>합쳐 온 글</p>"))

        assertEquals("e1", repository.commit(DraftKey.Edit("e1")))

        assertEquals(1, db.entryDao().countAll())
    }

    @Test
    fun autosave_does_not_forget_the_restore_mark() = runTest {
        db.seed(entry("e1"))
        val draft = leftDraftThenRestore("e1")

        repository.save(draft.copy(bodyHtml = "<p>이어 쓴 글</p>"))

        assertTrue(assertNotNull(repository.openEdit("e1")).entryChanged)
    }

    @Test
    fun a_draft_whose_entry_is_gone_is_saved_as_a_new_entry() = runTest {
        db.seed(entry("e1"))
        val draft = assertNotNull(repository.openEdit("e1")).draft
        repository.save(draft.copy(bodyHtml = "<p>남겨 둔 글</p>"))
        db.backupDao().clearEntries()

        val saved = repository.commit(DraftKey.Edit("e1"))

        assertEquals("남겨 둔 글", assertNotNull(db.entryDao().get(saved)).plainText)
    }

    @Test
    fun a_star_given_while_a_draft_waits_stays_after_saving() = runTest {
        db.seed(entry("e1"))
        val draft = assertNotNull(repository.openEdit("e1")).draft
        repository.save(draft.copy(bodyHtml = "<p>남겨 둔 글</p>"))
        db.entryDao().setFavorite("e1", true, T0.toEpochMilliseconds())

        repository.commit(DraftKey.Edit("e1"))

        assertTrue(assertNotNull(db.entryDao().get("e1")).isFavorite)
    }

    @Test
    fun a_check_ticked_while_a_draft_waits_is_not_lost() = runTest {
        db.seed(entry("e1").copy(bodyHtml = "<p>${Checklist.OPEN} 우유</p>"))
        val entries = DefaultEntryRepository(db, MEDIA_DIR, clock)
        val draft = assertNotNull(repository.openEdit("e1")).draft
        repository.save(draft.copy(bodyHtml = "<p>${Checklist.OPEN} 우유 사기</p>"))

        entries.toggleCheck("e1", 0)

        assertTrue(assertNotNull(repository.openEdit("e1")).entryChanged)
        assertTrue(repository.commit(DraftKey.Edit("e1")) != "e1")
    }

    @Test
    fun a_draft_only_matches_an_unchanged_entry() = runTest {
        db.seed(entry("e1"))
        val draft = assertNotNull(repository.openEdit("e1")).draft
        assertTrue(repository.matchesEntry(DraftKey.Edit("e1")))

        repository.save(draft.copy(bodyHtml = "<p>고침</p>"))

        assertFalse(repository.matchesEntry(DraftKey.Edit("e1")))
    }

    @Test
    fun saving_without_changes_keeps_updated_time_and_legacy_text() = runTest {
        // 레거시 본문처럼 태그와 글이 어긋나도 고치지 않으면 그대로 둠
        db.seed(entry("e1").copy(plainText = "원문  두 칸"))
        repository.openEdit("e1")

        clock.advance(1.hours)
        repository.commit(DraftKey.Edit("e1"))

        val saved = assertNotNull(db.entryDao().get("e1")).asModel()
        assertEquals(T0, saved.updatedAt)
        assertEquals("원문  두 칸", saved.plainText)
    }

    @Test
    fun opening_an_edit_says_whether_a_draft_was_left() = runTest {
        db.seed(entry("e1"))

        assertFalse(assertNotNull(repository.openEdit("e1")).resumed)
        assertTrue(assertNotNull(repository.openEdit("e1")).resumed)
    }

    @Test
    fun new_and_edit_drafts_are_kept_apart() = runTest {
        db.seed(entry("e1"))
        repository.save(newDraft("<p>새 글</p>"))
        repository.openEdit("e1")

        assertEquals("<p>새 글</p>", repository.draft(DraftKey.New()).first()?.bodyHtml)
        assertEquals("<p>글 e1</p>", repository.draft(DraftKey.Edit("e1")).first()?.bodyHtml)
    }

    @Test
    fun a_second_new_draft_keeps_the_first_until_it_is_discarded() = runTest {
        repository.save(newDraft("<p>쓰던 글</p>"))
        clock.advance(1.hours)
        val second = DraftKey.New("new-2")
        repository.save(newDraft("<p>새 글</p>").copy(key = second, updatedAt = clock.now()))

        assertEquals(second, repository.latestNew()?.key)
        repository.commit(second)

        assertEquals("<p>쓰던 글</p>", repository.latestNew()?.bodyHtml)
    }

    @Test
    fun saving_twice_keeps_the_photos() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a")))

        repository.save(newDraft("<p>더 씀</p>"))

        assertEquals(1, repository.draft(DraftKey.New()).first()?.photos?.size)
    }

    @Test
    fun trashed_entry_cannot_be_opened_for_editing() = runTest {
        db.seed(entry("e1", deletedAt = T0))

        assertNull(repository.openEdit("e1"))
    }

    @Test
    fun photo_limit_adds_only_what_fits_and_saving_still_works() = runTest {
        limit = 2
        repository.save(newDraft())

        val result = repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b"), image("c")))

        assertEquals(AddPhotosResult.LimitReached(2, canUpgrade = true, added = 2), result)
        assertEquals(2, repository.draft(DraftKey.New()).first()?.photos?.size)
        assertTrue(assertNotNull(repository.draft(DraftKey.New()).first()).canSave())
    }

    @Test
    fun adding_exactly_up_to_the_limit_shows_no_notice() = runTest {
        limit = 3
        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b")))

        val result = repository.addPhotos(DraftKey.New(), listOf(image("c")))

        assertIs<AddPhotosResult.Added>(result)
    }

    @Test
    fun plus_limit_is_not_offered_an_upgrade() = runTest {
        limit = PhotoLimit.PLUS
        repository.save(newDraft())
        repeat(PhotoLimit.PLUS) { repository.addPhotos(DraftKey.New(), listOf(image("p$it"))) }

        assertEquals(
            AddPhotosResult.LimitReached(PhotoLimit.PLUS, canUpgrade = false, added = 0),
            repository.addPhotos(DraftKey.New(), listOf(image("more")))
        )
    }

    @Test
    fun an_entry_already_over_the_limit_keeps_its_photos_and_only_blocks_adding() = runTest {
        db.seed(entry("e1"))
        db.seedPhotos(*Array(12) { photo("p$it", "e1", it) })
        repository.openEdit("e1")

        val result = repository.addPhotos(DraftKey.Edit("e1"), listOf(image("a")))

        assertIs<AddPhotosResult.LimitReached>(result)
        repository.commit(DraftKey.Edit("e1"))
        assertEquals(12, db.mediaDao().forEntry("e1").size)
    }

    @Test
    fun same_photo_twice_is_one_file_and_two_rows() = runTest {
        repository.save(newDraft())

        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("a")))

        val jpgs = files.list(MEDIA_DIR).filter { it.name.endsWith(".jpg") }
        assertEquals(1, jpgs.size)
        assertEquals(2, repository.draft(DraftKey.New()).first()?.photos?.size)
    }

    @Test
    fun an_unreadable_image_is_counted_and_leaves_no_files() = runTest {
        repository.save(newDraft())

        val result = repository.addPhotos(DraftKey.New(), listOf(PickedImage { ByteArray(0) }, image("a")))

        assertEquals(AddPhotosResult.Added(1, failed = 1), result)
        assertEquals(1, files.list(MEDIA_DIR).size)
    }

    @Test
    fun no_space_stops_and_keeps_what_was_added() = runTest {
        var left = 2
        val full = FullDisk(files) { left-- <= 0 }
        val store = PhotoStore(full, MEDIA_DIR, copyEncoder(full), { ImageSize(10, 20) })
        val small = DefaultDraftRepository(db, store, clock) { limit }
        small.save(newDraft())

        assertEquals(AddPhotosResult.NoSpace(1), small.addPhotos(DraftKey.New(), listOf(image("a"), image("b"), image("c"))))
        assertEquals(1, small.draft(DraftKey.New()).first()?.photos?.size)
    }

    /**
     * 그림을 읽다 실패한 것은 공간 문제가 아님
     * 멈추지 않고 나머지를 더함
     */
    @Test
    fun an_image_that_cannot_be_read_is_counted_and_the_rest_are_added() = runTest {
        repository.save(newDraft())
        val io = PickedImage { throw okio.IOException("읽기 실패") }
        val other = PickedImage { throw IllegalStateException("파일이 사라짐") }

        val result = repository.addPhotos(DraftKey.New(), listOf(image("a"), io, other, image("b")))

        assertEquals(AddPhotosResult.Added(2, failed = 2), result)
        assertEquals(listOf(0, 1), db.draftDao().media(DraftKey.New().value).map { it.sortOrder })
    }

    @Test
    fun photos_can_be_reordered_and_removed() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b"), image("c")))
        val before = assertNotNull(repository.draft(DraftKey.New()).first()).photos

        repository.reorderPhotos(DraftKey.New(), listOf(2, 0, 1))
        repository.removePhoto(DraftKey.New(), 1)

        assertEquals(listOf(before[2], before[1]), repository.draft(DraftKey.New()).first()?.photos)
    }

    /** 뺀 사진은 되돌리면 원래 자리로 돌아오고, 파일은 그대로라 다시 쓸 수 있음 */
    @Test
    fun a_removed_photo_comes_back_in_place() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b"), image("c")))
        val before = assertNotNull(repository.draft(DraftKey.New()).first()).photos

        val removed = assertNotNull(repository.removePhoto(DraftKey.New(), 1))
        assertEquals(before[1], removed)
        assertEquals(true, repository.restorePhoto(DraftKey.New(), removed, 1))

        assertEquals(before, repository.draft(DraftKey.New()).first()?.photos)
        assertEquals(listOf(0, 1, 2), db.draftDao().media(DraftKey.New().value).map { it.sortOrder })
    }

    /** 없는 자리를 빼면 아무것도 돌려주지 않음 */
    @Test
    fun removing_a_missing_photo_returns_nothing() = runTest {
        repository.save(newDraft())
        assertEquals(null, repository.removePhoto(DraftKey.New(), 3))
    }

    /** 앞선 더하기가 그림을 읽는 동안 다른 더하기가 끼어들어도 같은 순번을 쓰지 않음 */
    @Test
    fun adding_photos_at_the_same_time_keeps_every_photo() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a")))
        val reading = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val slow = PickedImage {
            reading.complete(Unit)
            release.await()
            "slow".encodeToByteArray()
        }

        withContext(Dispatchers.Default) {
            val first = async { repository.addPhotos(DraftKey.New(), listOf(slow)) }
            reading.await()
            val second = async { repository.addPhotos(DraftKey.New(), listOf(image("b"))) }
            // 고치기 전에는 뒤의 더하기가 먼저 끝나고, 고친 뒤에는 앞의 것을 기다림
            withTimeoutOrNull(WAIT_MILLIS) { second.join() }
            release.complete(Unit)
            awaitAll(first, second)
        }

        assertEquals(listOf(0, 1, 2), db.draftDao().media(DraftKey.New().value).map { it.sortOrder })
    }

    /**
     * 사진을 지우는 사이에 만든 순서는 사진 수가 맞지 않음
     * 앱이 죽지 않고 그대로 둠
     */
    @Test
    fun a_stale_order_leaves_the_photos_as_they_are() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a"), image("b"), image("c")))
        val order = listOf(2, 0, 1)

        repository.removePhoto(DraftKey.New(), 0)
        repository.reorderPhotos(DraftKey.New(), order)

        assertEquals(listOf(0, 1), db.draftDao().media(DraftKey.New().value).map { it.sortOrder })
    }

    /** 임시 저장본이 지워진 뒤 도착한 사진은 앱을 죽이지 않고 못 더한 것으로 셈 */
    @Test
    fun photos_for_a_discarded_draft_are_counted_as_failed() = runTest {
        repository.save(newDraft())
        repository.discard(DraftKey.New())

        assertEquals(AddPhotosResult.Added(0, failed = 1), repository.addPhotos(DraftKey.New(), listOf(image("a"))))
    }

    @Test
    fun two_commits_at_once_make_one_entry() = runTest {
        repeat(ROUNDS) { round ->
            repository.save(newDraft())
            withContext(Dispatchers.Default) {
                List(2) { async { runCatching { repository.commit(DraftKey.New()) } } }.awaitAll()
            }
            assertEquals(round + 1, db.entryDao().countAll())
        }
    }

    /**
     * 사진 줄이 빠진 임시 저장본이 남으면 저장할 때 사진이 사라짐
     * 줄을 넣다 실패하면 임시 저장본도 남기지 않음
     */
    @Test
    fun opening_an_edit_writes_the_draft_and_its_photos_together() = runTest {
        db.seed(entry("e1"))
        db.seedPhotos(photo("p1", "e1", 0))
        db.useWriterConnection { connection ->
            connection.usePrepared(
                "CREATE TEMP TRIGGER no_photos BEFORE INSERT ON draft_media BEGIN SELECT RAISE(ABORT, '시험'); END"
            ) { it.step() }
        }

        assertFailsWith<SQLiteException> { repository.openEdit("e1") }

        assertNull(db.draftDao().get("e1"))
    }

    @Test
    fun removing_a_photo_raises_the_edit_time() = runTest {
        val saved = commitAfter { repository.removePhoto(DraftKey.Edit("e1"), 0) }

        assertEquals(T0 + 1.hours, saved)
    }

    @Test
    fun adding_a_photo_raises_the_edit_time() = runTest {
        val saved = commitAfter { repository.addPhotos(DraftKey.Edit("e1"), listOf(image("a"))) }

        assertEquals(T0 + 1.hours, saved)
    }

    @Test
    fun moving_a_photo_raises_the_edit_time() = runTest {
        val saved = commitAfter { repository.reorderPhotos(DraftKey.Edit("e1"), listOf(1, 0)) }

        assertEquals(T0 + 1.hours, saved)
    }

    /**
     * 찾을 수 없는 사진 줄은 임시 저장본에 없음
     * 이 줄 때문에 고친 것으로 치지 않음
     */
    @Test
    fun the_same_photos_keep_the_edit_time() = runTest {
        val saved = commitAfter(missing = true) {}

        assertEquals(T0, saved)
    }

    private suspend fun commitAfter(missing: Boolean = false, change: suspend () -> Unit): kotlin.time.Instant {
        db.seed(entry("e1"))
        db.seedPhotos(photo("p1", "e1", 0), photo("p2", "e1", 1))
        if (missing) db.seedPhotos(photo("gone", "e1", 2, MediaStatus.MISSING, legacyPath = "/old/gone.jpg"))
        repository.openEdit("e1")
        clock.advance(1.hours)
        change()
        repository.commit(DraftKey.Edit("e1"))
        return assertNotNull(db.entryDao().get("e1")).asModel().updatedAt
    }

    @Test
    fun discard_removes_only_that_draft() = runTest {
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), listOf(image("a")))

        repository.discard(DraftKey.New())

        assertNull(repository.draft(DraftKey.New()).first())
        assertEquals(0, db.entryDao().countAll())
    }

    @Test
    fun save_rule_needs_text_photo_or_mood() {
        assertFalse(newDraft("<p>   </p>").canSave())
        assertFalse(newDraft("").canSave())
        assertTrue(newDraft("<p>글</p>").canSave())
        assertTrue(newDraft("", Mood.GOOD).canSave())
    }
}

/**
 * 쓰기를 [full]이 참일 때부터 실패시킴
 * 저장 공간이 다 찬 기기
 */
private class FullDisk(delegate: FileSystem, private val full: () -> Boolean) : ForwardingFileSystem(delegate) {
    override fun sink(file: Path, mustCreate: Boolean): Sink {
        if (full()) throw okio.IOException("no space")
        return super.sink(file, mustCreate)
    }
}

private const val ROUNDS = 30
private const val WAIT_MILLIS = 300L
