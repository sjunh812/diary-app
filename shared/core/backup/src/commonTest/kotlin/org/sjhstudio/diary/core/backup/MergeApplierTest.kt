package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DiaryDatabaseConstructor
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.database.configured

class MergeApplierTest {
    private lateinit var db: DiaryDatabase
    private val files = HashSet<String>()
    private val now = Instant.parse("2026-11-05T00:00:00Z")
    private var ids = 0

    @BeforeTest
    fun open() {
        db = Room.inMemoryDatabaseBuilder(DiaryDatabaseConstructor::initialize).configured().build()
    }

    @AfterTest
    fun close() = db.close()

    private fun applier() = MergeApplier(
        db,
        { it in files },
        object : Clock {
            override fun now() = this@MergeApplierTest.now
        }
    ) { "new-${ids++}" }

    private fun entry(id: String, updated: Long, body: String = id, deletedAt: Long? = null) =
        EntryEntity(id, "2026-10-31", "21:40", "<p>$body</p>", body, null, null, null, false, 1, updated, deletedAt, null, null, null)

    private fun media(id: String, entry: String, status: String = "normal") = MediaEntity(
        id, entry, 0, status,
        if (status ==
            "missing"
        ) {
            null
        } else {
            "$id.jpg"
        },
        if (status == "missing") null else id, 1, 1, 5, null, 1
    )

    private suspend fun merge(vararg backup: MergeEntry) = applier().let { it.apply(it.plan(backup.toList()), "drive") }

    @Test
    fun local_entries_are_never_deleted_and_new_ones_arrive() = runTest {
        db.entryDao().insertAll(listOf(entry("mine", 1)))

        val counts = merge(MergeEntry(entry("theirs", 2), listOf(media("p", "theirs"))))

        assertEquals(1, counts.added)
        assertEquals(setOf("mine", "theirs"), db.backupDao().allEntries().map { it.id }.toSet())
        assertEquals(2, db.backupDao().bases().single().baseUpdatedAt)
    }

    @Test
    fun photos_wait_unless_the_file_is_here() = runTest {
        files += "here.jpg"

        merge(
            MergeEntry(entry("a", 2), listOf(media("here", "a"))),
            MergeEntry(entry("b", 2), listOf(media("away", "b"))),
            MergeEntry(entry("c", 2), listOf(media("gone", "c", "missing")))
        )

        val status = db.backupDao().allMedia().associate { it.id to it.status }
        assertEquals(mapOf("here" to "normal", "away" to "pending", "gone" to "missing"), status)
    }

    @Test
    fun both_changed_keeps_both_and_marks_them() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))

        val counts = merge(MergeEntry(entry("a", 9, "백업"), listOf(media("p", "a"))))

        assertEquals(1, counts.keptBoth)
        val all = db.backupDao().allEntries().associateBy { it.plainText }
        assertEquals("a", all.getValue("기기").id)
        assertEquals(all.getValue("백업").id, db.backupDao().allMedia().single().entryId)
        assertEquals(5, db.backupDao().bases().first { it.entryId == "a" }.baseUpdatedAt)
    }

    @Test
    fun replace_swaps_photo_rows() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 5)))
        db.mediaDao().insertAll(listOf(media("old", "a")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))

        merge(MergeEntry(entry("a", 9, "새 글"), listOf(media("new", "a"))))

        assertEquals("새 글", assertNotNull(db.entryDao().get("a")).plainText)
        assertEquals(listOf("new"), db.backupDao().allMedia().map { it.id })
    }

    @Test
    fun nearly_expired_trash_gets_seven_days() = runTest {
        val almost = now.toEpochMilliseconds() - 29L * 24 * 3600 * 1000 - 3600 * 1000

        merge(MergeEntry(entry("a", 2, deletedAt = almost), emptyList()))

        val left = assertNotNull(db.entryDao().get("a")?.deletedAt) + 30L * 24 * 3600 * 1000 - now.toEpochMilliseconds()
        assertEquals(7L * 24 * 3600 * 1000, left)
    }

    @Test
    fun keeping_both_twice_leaves_one_copy() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))
        val backup = MergeEntry(entry("a", 9, "백업"), listOf(media("p", "a")))

        merge(backup)
        val second = merge(backup)
        val third = merge(backup)

        assertEquals(0, second.keptBoth + third.keptBoth)
        assertEquals(2, db.backupDao().allEntries().size)
        assertEquals(1, db.backupDao().allMedia().size)
    }

    @Test
    fun the_same_time_with_different_text_twice_leaves_one_copy() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        val backup = MergeEntry(entry("a", 8, "백업"), emptyList())

        assertEquals(1, merge(backup).keptBoth)
        assertEquals(0, merge(backup).keptBoth)

        assertEquals(setOf("기기", "백업"), db.backupDao().allEntries().map { it.plainText }.toSet())
    }

    @Test
    fun a_copy_edited_after_keeping_both_is_not_mistaken_for_the_backup() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))
        val backup = MergeEntry(entry("a", 9, "백업"), emptyList())
        merge(backup)
        val copy = db.backupDao().allEntries().first { it.id != "a" }
        db.entryDao().update(copy.copy(updatedAt = 12, bodyHtml = "<p>고친 사본</p>", plainText = "고친 사본"))

        merge(backup)

        assertEquals(setOf("기기", "고친 사본", "백업"), db.backupDao().allEntries().map { it.plainText }.toSet())
    }

    @Test
    fun an_identical_text_in_another_entry_with_another_time_is_not_a_copy() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기"), entry("other", 3, "백업")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))

        assertEquals(1, merge(MergeEntry(entry("a", 9, "백업"), emptyList())).keptBoth)
    }

    @Test
    fun an_old_trashed_entry_restored_twice_stays_one_entry() = runTest {
        val backup = MergeEntry(entry("a", 2, deletedAt = 1), emptyList())

        merge(backup)
        val first = assertNotNull(db.entryDao().get("a")?.deletedAt)
        val second = merge(backup)

        assertEquals(0, second.keptBoth)
        assertEquals(1, db.backupDao().allEntries().size)
        assertEquals(first, db.entryDao().get("a")?.deletedAt)
    }

    @Test
    fun trash_with_exactly_one_day_left_keeps_its_date() = runTest {
        val deleted = now.toEpochMilliseconds() - 29L * DAY

        merge(MergeEntry(entry("a", 2, deletedAt = deleted), emptyList()))

        assertEquals(deleted, db.entryDao().get("a")?.deletedAt)
    }

    @Test
    fun trash_a_moment_short_of_one_day_gets_seven_days() = runTest {
        val deleted = now.toEpochMilliseconds() - 29L * DAY - 1

        merge(MergeEntry(entry("a", 2, deletedAt = deleted), emptyList()))

        assertEquals(now.toEpochMilliseconds() - 23L * DAY, db.entryDao().get("a")?.deletedAt)
    }

    @Test
    fun a_kept_copy_in_the_trash_gets_the_same_grace() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))

        merge(MergeEntry(entry("a", 9, "백업", deletedAt = 1), emptyList()))

        val copy = db.backupDao().allEntries().first { it.id != "a" }
        assertEquals(now.toEpochMilliseconds() - 23L * DAY, copy.deletedAt)
    }

    @Test
    fun the_kept_copy_points_at_the_same_photo_file_with_its_own_row() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 8, "기기")))
        db.mediaDao().insertAll(listOf(media("p", "a")))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))
        files += "p.jpg"

        merge(MergeEntry(entry("a", 9, "백업"), listOf(media("p", "a"))))

        val rows = db.backupDao().allMedia()
        assertEquals(2, rows.size)
        assertEquals(setOf("p.jpg"), rows.map { it.fileName }.toSet())
        assertEquals(setOf("normal"), rows.map { it.status }.toSet())
    }

    @Test
    fun a_replace_drops_the_old_rows_when_the_backup_has_no_photos() = runTest {
        db.entryDao().insertAll(listOf(entry("a", 5)))
        db.mediaDao().insertAll(listOf(media("old", "a"), media("old2", "a").copy(sortOrder = 1)))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 5)))

        merge(MergeEntry(entry("a", 9), emptyList()))

        assertEquals(emptyList(), db.backupDao().allMedia())
    }

    private companion object {
        const val DAY = 24L * 3600 * 1000
    }
}
