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
import kotlinx.datetime.TimeZone
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

    private suspend fun uploadBytes(bytes: ByteArray): org.sjhstudio.diary.core.drive.DriveFile {
        val path = dir / "note-${Random.nextLong()}.db"
        fs.write(path) { write(bytes) }
        return drive.uploadSmall(NewFile("note.db", null, "application/x-sqlite3"), path)
    }

    @Test
    fun an_empty_file_is_refused_and_leaves_no_copy() = runTest {
        db.entryDao().insertAll(listOf(local("here", "기기 글")))

        assertFailsWith<RestoreMismatch> { importer().prepare(uploadBytes(ByteArray(0))) }

        assertEquals(1, db.backupDao().allEntries().size)
        assertEquals(emptyList(), fs.listOrNull(dir / "work" / "incoming").orEmpty())
    }

    @Test
    fun a_database_without_the_note_table_is_refused() = runTest {
        val path = dir / "other.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.execSQL("CREATE TABLE memo (_id INTEGER PRIMARY KEY)")
        connection.close()

        assertFailsWith<RestoreMismatch> { importer().prepare(drive.uploadSmall(NewFile("note.db", null, "application/x-sqlite3"), path)) }
    }

    @Test
    fun an_old_backup_with_no_entries_adds_nothing() = runTest {
        db.entryDao().insertAll(listOf(local("here", "기기 글")))

        val plan = importer().prepare(upload())

        assertEquals(0, plan.preview.entriesAdded)
        assertEquals(0, plan.preview.entriesUnchanged)
        importer().apply(plan)
        assertEquals(1, db.backupDao().allEntries().size)
    }

    @Test
    fun a_file_cut_in_half_never_changes_the_device_before_it_is_confirmed() = runTest {
        db.entryDao().insertAll(listOf(local("here", "기기 글")))
        val rows = (1L..2000L).map { Triple(it, "2026-10-01 08:00:00", "글 $it") }.toTypedArray()
        val whole = drive.bytes(upload(*rows).id)

        runCatching { importer().prepare(uploadBytes(whole.copyOf(whole.size / 2))) }

        assertEquals(setOf("here"), db.backupDao().allEntries().map { it.id }.toSet())
    }

    @Test
    fun two_old_rows_at_the_time_of_one_local_entry_are_both_kept() = runTest {
        db.entryDao().insertAll(listOf(local("here", "기기 글")))
        val file = upload(Triple(1, "2026-10-31 21:40:00", "옛 글 하나"), Triple(2, "2026-10-31 21:40:00", "옛 글 둘"))

        val plan = importer().prepare(file)
        importer().apply(plan)

        assertEquals(2, plan.preview.entriesKeptBoth)
        assertEquals(setOf("기기 글", "옛 글 하나", "옛 글 둘"), db.backupDao().allEntries().map { it.plainText }.toSet())
    }

    @Test
    fun the_same_row_twice_in_one_file_comes_once() = runTest {
        val file = upload(Triple(1, "2026-10-01 08:00:00", "같은 글"), Triple(2, "2026-10-01 08:00:00", "같은 글"))

        val plan = importer().prepare(file)

        assertEquals(1, plan.preview.entriesAdded)
        assertEquals(1, plan.preview.entriesUnchanged)
    }

    /** 분까지 같고 초가 다르면 다른 일기. 글이 같아도 빼지 않음 */
    @Test
    fun same_minute_and_text_but_another_second_stays() = runTest {
        val file = upload(Triple(1, "2026-10-01 08:00:05", "같은 글"), Triple(2, "2026-10-01 08:00:40", "같은 글"))

        val plan = importer().prepare(file)

        assertEquals(2, plan.preview.entriesAdded)
    }

    @Test
    fun an_entry_in_the_trash_counts_as_already_here() = runTest {
        db.entryDao().insertAll(listOf(local("here", "같은 글").copy(deletedAt = 5)))

        val plan = importer().prepare(upload(Triple(1, "2026-10-31 21:40:00", "같은 글")))

        assertEquals(0, plan.preview.entriesAdded)
    }

    @Test
    fun text_with_quotes_and_marks_comes_through() = runTest {
        val path = dir / "quote.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.execSQL(CREATE)
        connection.execSQL(
            "INSERT INTO Note(_id, contents, create_date, modify_date) VALUES(1, '그''s & 😀', '2026-10-01 08:00:00', '2026-10-01 08:00:00')"
        )
        connection.close()
        val file = drive.uploadSmall(NewFile("note.db", null, "application/x-sqlite3"), path)

        importer().apply(importer().prepare(file))

        assertEquals("그's & 😀", db.backupDao().allEntries().single().plainText)
    }

    @Test
    fun a_time_zone_does_not_move_an_old_entry_to_another_day() = runTest {
        val file = upload(Triple(1, "2026-10-31 23:50:00", "밤에 쓴 글"))

        for (zone in listOf("Asia/Seoul", "America/Los_Angeles", "UTC")) {
            val other =
                LegacyBackupImporter(db, drive, { _, _, _ ->
                    null
                }, fs, dir / "work", timeZone = { TimeZone.of(zone) }, newId = { "n${Random.nextLong()}" })
            val entry = other.prepare(file).items.single().entry

            assertEquals("2026-10-31" to "23:50", entry.date to entry.time, zone)
        }
    }

    @Test
    fun a_refused_file_can_be_followed_by_a_good_one() = runTest {
        assertFailsWith<RestoreMismatch> { importer().prepare(uploadBytes("DB가 아니다".encodeToByteArray())) }

        val plan = importer().prepare(upload(Triple(1, "2026-10-01 08:00:00", "글")))

        assertEquals(1, plan.preview.entriesAdded)
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
