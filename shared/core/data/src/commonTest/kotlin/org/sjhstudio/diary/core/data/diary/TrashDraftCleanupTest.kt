package org.sjhstudio.diary.core.data.diary

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.photo.DefaultMediaRepository
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.database.DraftMediaEntity
import org.sjhstudio.diary.core.testing.TestClock

// 완전히 지운 일기의 글과 사진이 고치던 저장본에 남지 않음(헌법 II)
// 지울 일기가 아닌 저장본과 다른 일기의 사진은 잃지 않음(헌법 I)
class TrashDraftCleanupTest {
    private val db = memoryDatabase()
    private val trash = DefaultTrashRepository(db, MEDIA_DIR, TestClock(T0))
    private val files = FakeFileSystem().apply { createDirectories(MEDIA_DIR) }
    private val cleaner = DefaultMediaRepository(db, files, MEDIA_DIR, THUMB_DIR)

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun editDraft(entryId: String, vararg photos: String) {
        db.draftDao().upsert(
            DraftEntity(entryId, "2026-10-31", "21:40", "<p>고치던 글</p>", null, null, null, false, false, 0)
        )
        val rows = photos.mapIndexed { index, name -> DraftMediaEntity(entryId, name, name, 1, 1, 1, index) }
        if (rows.isNotEmpty()) db.draftDao().insertMedia(rows)
    }

    private fun photoFile(name: String) = files.write(MEDIA_DIR / name) { writeUtf8("x") }

    private suspend fun cleanLater() {
        val written = Instant.fromEpochMilliseconds(requireNotNull(files.metadata(MEDIA_DIR).lastModifiedAtMillis))
        cleaner.cleanUnused(written + 2.days)
    }

    private fun photosLeft() = files.list(MEDIA_DIR).map { it.name }.toSet()

    @Test
    fun deleting_forever_removes_the_draft_and_its_photo() = runTest {
        db.seed(entry("a", deletedAt = T0))
        photoFile("edit.jpg")
        editDraft("a", "edit.jpg")

        trash.deleteForever(listOf("a"))
        cleanLater()

        assertNull(db.draftDao().get("a"))
        assertEquals(emptyList(), db.draftDao().media("a"))
        assertEquals(emptySet(), photosLeft())
    }

    @Test
    fun emptying_the_trash_removes_every_draft_of_it() = runTest {
        db.seed(entry("a", deletedAt = T0), entry("b", deletedAt = T0))
        photoFile("a.jpg")
        editDraft("a", "a.jpg")
        editDraft("b")

        trash.empty()
        cleanLater()

        assertNull(db.draftDao().get("a"))
        assertNull(db.draftDao().get("b"))
        assertEquals(emptySet(), photosLeft())
    }

    @Test
    fun the_thirty_day_cleanup_removes_only_expired_drafts() = runTest {
        db.seed(entry("old", deletedAt = T0 - 31.days), entry("recent", deletedAt = T0 - 1.days))
        editDraft("old")
        editDraft("recent")

        trash.purgeExpired(T0)

        assertNull(db.draftDao().get("old"))
        assertNotNull(db.draftDao().get("recent"))
    }

    /** 되돌릴 수 있는 일기의 고치던 글은 그대로 */
    @Test
    fun a_draft_of_an_entry_outside_the_trash_stays() = runTest {
        db.seed(entry("a", deletedAt = T0), entry("b"))
        editDraft("a")
        editDraft("b")

        trash.deleteForever(listOf("a", "b"))

        assertNull(db.draftDao().get("a"))
        assertNotNull(db.draftDao().get("b"))
    }

    @Test
    fun a_new_entry_draft_stays() = runTest {
        db.seed(entry("a", deletedAt = T0))
        editDraft("new")

        trash.empty()

        assertNotNull(db.draftDao().get("new"))
    }

    /** 같은 사진은 파일 하나를 함께 씀 */
    @Test
    fun a_photo_another_entry_uses_is_kept() = runTest {
        db.seed(entry("a", deletedAt = T0), entry("b"))
        db.seedPhotos(photo("shared", "b", 0))
        photoFile("shared.jpg")
        editDraft("a", "shared.jpg")

        trash.deleteForever(listOf("a"))
        cleanLater()

        assertEquals(setOf("shared.jpg"), photosLeft())
    }

    @Test
    fun a_photo_another_draft_uses_is_kept() = runTest {
        db.seed(entry("a", deletedAt = T0))
        photoFile("shared.jpg")
        editDraft("a", "shared.jpg")
        editDraft("new", "shared.jpg")

        trash.deleteForever(listOf("a"))
        cleanLater()

        assertEquals(setOf("shared.jpg"), photosLeft())
    }
}
