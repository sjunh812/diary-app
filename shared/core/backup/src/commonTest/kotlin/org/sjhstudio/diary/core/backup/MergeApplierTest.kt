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
}
