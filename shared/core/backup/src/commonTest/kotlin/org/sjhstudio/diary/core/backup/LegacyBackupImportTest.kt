package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.testing.FakeDrive

class LegacyBackupImportTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase
    private lateinit var drive: FakeDrive

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "legacy-${Random.nextLong()}"
        fs.createDirectories(dir)
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
        drive = FakeDrive(fs)
    }

    @AfterTest
    fun tearDown() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun importer() = LegacyBackupImporter(db, drive, { _, _, _ -> null }, fs, dir / "work", newId = { "n${Random.nextLong()}" })

    private suspend fun upload(vararg rows: Triple<Long, String, String>, picture: String = ""): org.sjhstudio.diary.core.drive.DriveFile {
        val path = dir / "note-${Random.nextLong()}.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.execSQL(CREATE)
        for ((id, created, text) in rows) {
            connection.execSQL(
                "INSERT INTO Note(_id, contents, create_date, modify_date, picture) VALUES($id, '$text', '$created', '$created', '$picture')"
            )
        }
        connection.close()
        return drive.uploadSmall(NewFile("note.db", null, "application/x-sqlite3"), path)
    }

    private fun local(id: String, text: String, legacyId: Int? = null) =
        EntryEntity(id, "2026-10-31", "21:40", "<p>$text</p>", text, null, null, null, false, 1, 1, null, legacyId, null, null)

    @Test
    fun same_entry_is_skipped_and_different_text_keeps_both() = runTest {
        db.entryDao().insertAll(listOf(local("here", "같은 글"), local("other", "기기 글").copy(time = "08:00")))
        val file =
            upload(
                Triple(1, "2026-10-31 21:40:00", "같은 글"),
                Triple(2, "2026-10-31 08:00:00", "예전 글"),
                Triple(3, "2026-10-01 08:00:00", "새 글")
            )

        val plan = importer().prepare(file)

        assertEquals(2, plan.preview.entriesAdded)
        assertEquals(1, plan.preview.entriesKeptBoth)
        assertEquals(1, plan.preview.entriesUnchanged)
        importer().apply(plan)
        assertEquals(4, db.backupDao().allEntries().size)
        assertEquals(setOf("other"), db.backupDao().markedIds().filter { !it.startsWith("n") }.toSet())
    }

    @Test
    fun running_again_adds_nothing() = runTest {
        val file = upload(Triple(1, "2026-10-01 08:00:00", "글"))
        importer().apply(importer().prepare(file))

        val second = importer().prepare(file)

        assertEquals(0, second.preview.entriesAdded)
        assertEquals(1, second.preview.entriesUnchanged)
    }

    @Test
    fun same_legacy_number_from_another_device_is_not_a_conflict() = runTest {
        db.entryDao().insertAll(listOf(local("here", "기기 글", legacyId = 1)))
        val file = upload(Triple(1, "2026-10-01 08:00:00", "다른 기기 글"))

        importer().apply(importer().prepare(file))

        assertEquals(2, db.backupDao().allEntries().size)
    }

    @Test
    fun photos_without_files_stay_as_missing_rows() = runTest {
        val file = upload(Triple(1, "2026-10-01 08:00:00", "글"), picture = "/cache/a.jpg,/cache/b.jpg,")

        val result = importer().apply(importer().prepare(file))

        assertEquals(2, result.photosMissing)
        assertEquals(listOf("/cache/a.jpg", "/cache/b.jpg"), db.backupDao().allMedia().sortedBy { it.sortOrder }.map { it.legacyPath })
    }

    @Test
    fun known_file_comes_with_named_ones_newest_first() = runTest {
        val first = upload(Triple(1, "2026-10-01 08:00:00", "글"))
        val second = upload(Triple(1, "2026-10-02 08:00:00", "글"))

        val found = importer().find(first.id)

        assertEquals(listOf(second.id, first.id), found.map { it.id })
    }

    @Test
    fun missing_known_file_falls_back_to_the_name() = runTest {
        val file = upload(Triple(1, "2026-10-01 08:00:00", "글"))

        assertEquals(listOf(file.id), importer().find("gone").map { it.id })
    }

    @Test
    fun broken_file_changes_nothing() = runTest {
        val path = dir / "broken.db"
        fs.write(path) { writeUtf8("이건 DB가 아니에요") }
        val file = drive.uploadSmall(NewFile("note.db", null, "application/x-sqlite3"), path)

        assertFailsWith<RestoreMismatch> { importer().prepare(file) }
        assertEquals(0, db.backupDao().allEntries().size)
    }

    private companion object {
        const val CREATE =
            "CREATE TABLE IF NOT EXISTS Note (_id INTEGER PRIMARY KEY AUTOINCREMENT, weather INTEGER DEFAULT -1, " +
                "address TEXT DEFAULT '', " +
                "location_x TEXT DEFAULT '', location_y TEXT DEFAULT '', contents TEXT DEFAULT '', mood INTEGER DEFAULT -1, " +
                "picture TEXT DEFAULT '', create_date TIMESTAMP, modify_date TIMESTAMP, year INTEGER DEFAULT 1900, " +
                "month INTEGER DEFAULT 1, star INTEGER DEFAULT 0);"
    }
}
