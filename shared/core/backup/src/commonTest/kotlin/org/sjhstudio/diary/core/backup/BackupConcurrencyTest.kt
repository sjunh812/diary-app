package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/** 백업이 도는 동안 일기가 바뀌어도 이번 백업은 한 시점이고, 복원·이전과는 겹치지 않는다(스펙 003 T037). */
class BackupConcurrencyTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase
    private lateinit var drive: FakeDrive
    private lateinit var store: BackupStore
    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = this@BackupConcurrencyTest.now
    }

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "backup-${Random.nextLong()}"
        fs.createDirectories(dir / "media")
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
        drive = FakeDrive(fs)
        store = BackupStore(InMemoryDataStore(emptyPreferences()))
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun runner(gate: BackupGate = BackupGate(), dataReady: () -> Boolean = { true }) = BackupRunner(
        db, drive, FakeDriveAuthorizer(), store, FakeNetworkState(), DeviceName { "Galaxy S24" }, fs, dir / "media", dir / "backup",
        BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings(font = "pretendard") }, clock, { TimeZone.UTC },
        gate = gate, dataReady = dataReady
    )

    private fun entry(id: String, body: String = id) =
        EntryEntity(id, "2026-11-01", "21:40", "<p>$body</p>", body, null, null, null, false, 1, 7, null, null, null, null)

    private suspend fun seed() {
        db.entryDao().insertAll(listOf(entry("a"), entry("b")))
        val bytes = "사진".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        fs.write(dir / "media" / "$hash.jpg") { write(bytes) }
        db.mediaDao().insertAll(listOf(MediaEntity("a-0", "a", 0, "normal", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1)))
    }

    private fun manifests() =
        drive.visible.filter { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST }.sortedBy { it.createdAt }.map {
            assertNotNull(ManifestCodec.decode(drive.bytes(it.id).decodeToString()))
        }

    @Test
    fun a_new_entry_during_backup_waits_for_the_next_backup() = runTest {
        seed()
        var added = false
        // 스냅샷을 뜬 뒤 사진을 올리는 동안 새 일기를 쓴다.
        drive.onUpload = { meta ->
            if (!added && meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO) {
                added = true
                db.entryDao().insertAll(listOf(entry("c")))
            }
        }
        val runner = runner()

        assertIs<BackupOutcome.Done>(runner.run())
        assertTrue(added)
        assertEquals(2, manifests().single().counts.entries)

        now = Instant.parse("2026-11-06T01:00:00Z")
        drive.onUpload = null
        assertIs<BackupOutcome.Done>(runner.runIfChanged())
        assertEquals(3, manifests().last().counts.entries)
    }

    @Test
    fun an_edit_during_backup_is_not_forgotten() = runTest {
        seed()
        var edited = false
        drive.onUpload = { meta ->
            if (!edited && meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST) {
                edited = true
                db.entryDao().setDeletedAt("b", 100, 100)
            }
        }
        val runner = runner()

        assertIs<BackupOutcome.Done>(runner.run())

        now = Instant.parse("2026-11-06T01:00:00Z")
        drive.onUpload = null
        // 지운 일이 지문에 잡혀 다음 자동 백업이 건너뛰지 않는다.
        assertIs<BackupOutcome.Done>(runner.runIfChanged())
    }

    @Test
    fun backup_does_not_start_while_a_restore_holds_the_gate() = runTest {
        seed()
        val gate = BackupGate()
        assertTrue(gate.tryEnter())

        assertIs<BackupOutcome.Busy>(runner(gate).run())
        assertTrue(drive.visible.isEmpty())

        gate.leave()
        assertIs<BackupOutcome.Done>(runner(gate).run())
    }

    @Test
    fun a_finished_backup_lets_a_waiting_restore_go() = runTest {
        seed()
        val gate = BackupGate()

        assertIs<BackupOutcome.Done>(runner(gate).run())

        assertTrue(gate.tryEnter())
    }

    @Test
    fun backup_does_not_start_before_migration_finishes() = runTest {
        seed()

        assertIs<BackupOutcome.Busy>(runner(dataReady = { false }).run())

        assertTrue(drive.visible.isEmpty())
    }
}
