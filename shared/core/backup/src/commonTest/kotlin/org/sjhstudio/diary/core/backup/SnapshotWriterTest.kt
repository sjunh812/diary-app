package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.BackupMediaEntity
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.database.configured

class SnapshotWriterTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "snapshot-${Random.nextLong()}"
        fs.createDirectories(dir)
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun entry(id: String, deletedAt: Long? = null) = EntryEntity(
        id = id,
        date = "2026-10-31",
        time = "21:40",
        bodyHtml = "<p>$id</p>",
        plainText = id,
        mood = null,
        weather = null,
        placeName = null,
        isFavorite = false,
        createdAt = 1,
        updatedAt = 1,
        deletedAt = deletedAt,
        legacyId = null,
        legacyCreateDate = null,
        legacyLocation = null
    )

    private fun media(id: String, entry: String, order: Int, status: String) = MediaEntity(
        id, entry, order, status,
        if (status ==
            "missing"
        ) {
            null
        } else {
            "$id.jpg"
        },
        if (status == "missing") null else id, null, null, null, null, 1
    )

    private suspend fun seed() {
        db.entryDao().insertAll(listOf(entry("a"), entry("b"), entry("c", deletedAt = 5)))
        db.mediaDao().insertAll(listOf(media("m1", "a", 0, "normal"), media("m2", "a", 1, "missing"), media("m3", "b", 0, "pending")))
        db.backupDao().confirm(listOf(BackupMediaEntity("acc", "m1", "f", 1, 1)))
        db.backupDao().setBase(listOf(SyncBaseEntity("a", 1)))
    }

    private fun count(path: Path, table: String): Int {
        val connection = BundledSQLiteDriver().open(path.toString())
        try {
            return connection.prepare("SELECT COUNT(*) FROM $table").use {
                it.step()
                it.getInt(0)
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun device_tables_are_empty_and_rows_stay() = runTest {
        seed()

        val snapshot = SnapshotWriter(db, fs).write(dir / "outgoing" / "a.db")

        for (table in SnapshotWriter.DEVICE_TABLES) assertEquals(0, count(snapshot.path, table), table)
        assertEquals(3, count(snapshot.path, "entry"))
        assertEquals(3, count(snapshot.path, "media"))
        assertEquals(2, snapshot.entries)
        assertEquals(1, snapshot.entriesInTrash)
        assertEquals(1, snapshot.mediaMissing)
        assertEquals(1, snapshot.mediaPending)
    }

    @Test
    fun hashes_match_the_file_and_original_is_untouched() = runTest {
        seed()

        val snapshot = SnapshotWriter(db, fs).write(dir / "b.db")

        assertEquals(fs.hashes(snapshot.path), snapshot.hashes)
        assertEquals(1, db.backupDao().confirmed("acc").size)
    }

    @Test
    fun later_writes_are_not_in_the_snapshot() = runTest {
        seed()

        val snapshot = SnapshotWriter(db, fs).write(dir / "c.db")
        db.entryDao().insertAll(listOf(entry("d")))

        assertEquals(3, count(snapshot.path, "entry"))
    }
}
