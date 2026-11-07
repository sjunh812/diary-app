package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class ReplaceAndRollbackTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase
    private lateinit var store: BackupStore
    private var free = 1_000_000_000L
    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val applied = mutableListOf<ManifestSettings>()
    private val clock = object : Clock {
        override fun now() = this@ReplaceAndRollbackTest.now
    }

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "rollback-${Random.nextLong()}"
        fs.createDirectories(dir)
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
        store = BackupStore(InMemoryDataStore(emptyPreferences()))
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun manager() = RollbackManager(
        db, MergeApplier(db, { false }, clock), store, FreeSpace { free }, fs, dir / "backup",
        { ManifestSettings(font = "before") }, { applied += it }, clock
    )

    private val T0 get() = now.toEpochMilliseconds()

    private fun entry(id: String, updated: Long = 1) =
        EntryEntity(id, "2026-10-31", "21:40", "<p>$id</p>", id, null, null, null, false, 1, updated, null, null, null, null)

    private fun backup(vararg ids: String) = ids.map { MergeEntry(entry(it), emptyList()) }

    private suspend fun seed(vararg ids: String) {
        db.entryDao().insertAll(ids.map { entry(it) })
        db.mediaDao().insertAll(listOf(MediaEntity("m", ids.first(), 0, "normal", "a.jpg", "a", 1, 1, 1, null, 1)))
        db.backupDao().setBase(listOf(SyncBaseEntity(ids.first(), 1)))
        db.backupDao().mark(listOf(MergeMarkEntity(ids.first(), "kept_both", null, "drive", 1)))
    }

    private suspend fun ids() = db.backupDao().allEntries().map { it.id }.toSet()

    @Test
    fun no_room_for_the_copy_changes_nothing() = runTest {
        seed("here")
        free = 0

        assertFailsWith<NoRoomForRollback> { manager().replace(backup("new"), "drive") }

        assertEquals(setOf("here"), ids())
        assertNull(store.current().rollbackUntil)
    }

    @Test
    fun replace_swaps_the_entries_and_opens_seven_days() = runTest {
        seed("here")

        manager().replace(backup("new"), "drive")

        assertEquals(setOf("new"), ids())
        assertEquals(now + 7.days, store.current().rollbackUntil)
        assertEquals(1, manager().info.first()?.entriesBefore)
    }

    @Test
    fun replace_marks_every_left_edit_draft_but_not_new_ones() = runTest {
        seed("here")
        val at = now.toEpochMilliseconds()
        listOf("here", "new-1").forEach {
            db.draftDao().upsert(DraftEntity(it, "2026-11-01", "21:00", "<p>글</p>", null, null, null, false, false, at))
        }

        manager().replace(backup("new"), "drive")

        assertTrue(assertNotNull(db.draftDao().get("here")).entryChanged)
        assertFalse(assertNotNull(db.draftDao().get("new-1")).entryChanged)
    }

    @Test
    fun rollback_brings_everything_back() = runTest {
        seed("here")
        manager().replace(backup("new"), "drive")

        assertTrue(manager().rollBack())

        assertEquals(setOf("here"), ids())
        assertEquals("a", db.backupDao().allMedia().single().sha256)
        assertEquals(1, db.backupDao().bases().single().baseUpdatedAt)
        assertEquals(listOf("here"), db.backupDao().markedIds())
        assertEquals("before", applied.single().font)
        assertNull(store.current().rollbackUntil)
    }

    @Test
    fun entry_written_after_replace_survives_rollback() = runTest {
        seed("here")
        manager().replace(backup("new"), "drive")
        now += 1.days
        db.entryDao().insertAll(listOf(entry("after", T0)))
        assertEquals(1, manager().info.first()?.entriesAfter)

        manager().rollBack()

        assertEquals(setOf("here", "after"), ids())
    }

    @Test
    fun copy_is_dropped_after_seven_days() = runTest {
        seed("here")
        manager().replace(backup("new"), "drive")
        now += 7.days

        manager().expire()

        assertNull(store.current().rollbackUntil)
        assertFalse(manager().rollBack())
        assertEquals(setOf("new"), ids())
    }

    @Test
    fun info_is_gone_when_the_time_has_passed() = runTest {
        seed("here")
        manager().replace(backup("new"), "drive")
        assertNotNull(manager().info.first())

        now += 8.days

        assertNull(manager().info.first())
    }

    @Test
    fun info_counts_live_entries_only() = runTest {
        db.entryDao().insertAll(listOf(entry("kept"), entry("thrown").copy(deletedAt = 5)))
        manager().replace(backup("new"), "drive")
        now += 1.days
        db.entryDao().insertAll(listOf(entry("after", T0), entry("after-thrown", T0).copy(deletedAt = 5)))

        val info = assertNotNull(manager().info.first())

        assertEquals(1, info.entriesBefore)
        assertEquals(1, info.entriesAfter)
    }

    @Test
    fun a_damaged_copy_hides_the_rollback_instead_of_crashing() = runTest {
        seed("here")
        manager().replace(backup("new"), "drive")
        fs.write(fs.list(dir / "backup" / "rollback").single() / "diary.db") { writeUtf8("깨진 파일") }

        assertNull(manager().info.first())
    }
}
