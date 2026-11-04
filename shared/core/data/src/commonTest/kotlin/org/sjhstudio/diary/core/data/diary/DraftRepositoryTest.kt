package org.sjhstudio.diary.core.data.diary

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
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.testing.TestClock

// 임시 저장과 사진(스펙 002 T032, T033). 일기를 잃지 않는지를 본다(헌법 I).
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
        // 같은 순서 번호 두 줄로 사진 쓰기를 실패시킨다.
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
        val draft = assertNotNull(repository.openEdit("e1"))
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

    @Test
    fun saving_without_changes_keeps_updated_time_and_legacy_text() = runTest {
        // 레거시 본문처럼 태그와 글이 어긋나도 고치지 않으면 그대로 둔다.
        db.seed(entry("e1").copy(plainText = "원문  두 칸"))
        repository.openEdit("e1")

        clock.advance(1.hours)
        repository.commit(DraftKey.Edit("e1"))

        val saved = assertNotNull(db.entryDao().get("e1")).asModel()
        assertEquals(T0, saved.updatedAt)
        assertEquals("원문  두 칸", saved.plainText)
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
        repository.save(newDraft())
        val full = PickedImage { throw okio.IOException("no space") }

        assertEquals(AddPhotosResult.NoSpace(1), repository.addPhotos(DraftKey.New(), listOf(image("a"), full, image("b"))))
        assertEquals(1, repository.draft(DraftKey.New()).first()?.photos?.size)
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
