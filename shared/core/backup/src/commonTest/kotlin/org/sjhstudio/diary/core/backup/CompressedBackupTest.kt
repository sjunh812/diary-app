package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/**
 * 형식 3(줄인 스냅샷) 백업과 그 전에 올린 형식 2 백업
 * 옛 백업은 그대로 두고, 둘 다 복원됨
 */
class CompressedBackupTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var phoneA: DiaryDatabase
    private lateinit var phoneB: DiaryDatabase
    private lateinit var drive: FakeDrive

    private fun open(name: String): DiaryDatabase {
        fs.createDirectories(dir / name / "media")
        return Room.databaseBuilder<DiaryDatabase>((dir / name / "diary.db").toString()).configured().build()
    }

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "compressed-${Random.nextLong()}"
        phoneA = open("a")
        phoneB = open("b")
        drive = FakeDrive(fs)
    }

    @AfterTest
    fun tearDown() {
        phoneA.close()
        phoneB.close()
        fs.deleteRecursively(dir)
    }

    private val body = "출근길 하늘이 유난히 파랬다. 오늘은 좋은 일이 있을 것 같다. ".repeat(10)

    private fun entry(id: String) =
        EntryEntity(id, "2026-10-31", "21:40", "<p>$body</p>", body, "good", null, null, false, 1, 7, null, null, null, null)

    private suspend fun backUp(count: Int = 50) {
        phoneA.entryDao().insertAll((1..count).map { entry("e$it") })
        val runner =
            BackupRunner(
                phoneA, drive, FakeDriveAuthorizer(), BackupStore(InMemoryDataStore(emptyPreferences())), FakeNetworkState(),
                DeviceName { "A" }, fs, dir / "a" / "media", dir / "a" / "backup", BackupAppInfo("2.3.0", 51, "android"),
                { "README" }, { ManifestSettings() }, io = EmptyCoroutineContext
            )
        assertTrue(runner.run() is BackupOutcome.Done)
    }

    private fun planner() = RestorePlanner(
        drive,
        MergeApplier(phoneB, { fs.exists(dir / "b" / "media" / it) }),
        FreeSpace { 1_000_000 },
        fs,
        dir / "b" / "backup"
    )

    private fun catalog() = RestoreCatalog(drive, fs, dir / "b" / "backup")

    private fun kind(kind: String) = drive.visible.filter { it.props[DriveLayout.KEY_KIND] == kind }

    private fun incomingLeft() = fs.listOrNull(dir / "b" / "backup" / "incoming").orEmpty()

    @Test
    fun the_drive_snapshot_is_smaller_than_the_diary() = runTest {
        backUp()
        SnapshotWriter(phoneA, fs).write(dir / "raw.db")

        val uploaded = kind(DriveLayout.KIND_SNAPSHOT).single()

        assertTrue(uploaded.name.endsWith(".db.gz"))
        assertTrue(uploaded.size!! * 3 < fs.metadata(dir / "raw.db").size!!, "${uploaded.size}")
    }

    @Test
    fun a_compressed_backup_brings_every_entry_back() = runTest {
        backUp()

        val prepared = planner().prepare(catalog().list().single())
        MergeApplier(phoneB, { false }).apply(prepared.plan, "drive")

        assertEquals(50, phoneB.backupDao().allEntries().size)
        assertEquals(body, phoneB.backupDao().allEntries().first().plainText)
    }

    /** 2.2.0까지 올린 형식 2(.db 그대로) 백업은 지우거나 바꾸지 않고 그대로 복원 */
    @Test
    fun an_old_uncompressed_backup_still_restores() = runTest {
        backUp(3)
        val newSnapshot = kind(DriveLayout.KIND_SNAPSHOT).single()
        val newManifest = kind(DriveLayout.KIND_MANIFEST).single()
        val manifest = ManifestCodec.decode(drive.bytes(newManifest.id).decodeToString())!!
        fs.write(dir / "packed.db.gz") { write(drive.bytes(newSnapshot.id)) }
        fs.gunzip(dir / "packed.db.gz", dir / "old.db")
        val raw = fs.hashes(dir / "old.db")
        val oldManifest =
            manifest.copy(
                formatVersion = 2,
                snapshot = manifest.snapshot.copy(fileName = "old.db", byteSize = raw.byteSize, sha256 = raw.sha256, encoding = null)
            )
        fs.write(dir / "old.json") { writeUtf8(ManifestCodec.encode(oldManifest)) }
        drive.deleteByUser(newSnapshot.id)
        drive.deleteByUser(newManifest.id)
        drive.uploadSmall(NewFile("old.db", newSnapshot.parents.single(), "application/x-sqlite3", newSnapshot.props), dir / "old.db")
        drive.uploadSmall(NewFile("old.json", newManifest.parents.single(), "application/json", newManifest.props), dir / "old.json")

        val listed = catalog().list().single()
        assertEquals(BackupEntryState.Ready, listed.entry.state)
        val prepared = planner().prepare(listed)

        assertEquals(3, prepared.preview.entriesAdded)
    }

    /** 확인값까지 맞춰 올린 파일이라도 풀리지 않으면 아무것도 쓰지 않고 받은 파일도 남기지 않음 */
    @Test
    fun a_snapshot_that_does_not_unpack_is_refused() = runTest {
        backUp(3)
        val snapshot = kind(DriveLayout.KIND_SNAPSHOT).single()
        val manifestFile = kind(DriveLayout.KIND_MANIFEST).single()
        val manifest = ManifestCodec.decode(drive.bytes(manifestFile.id).decodeToString())!!
        fs.write(dir / "bad.db.gz") { writeUtf8("gzip이 아님") }
        val bad = fs.hashes(dir / "bad.db.gz")
        drive.deleteByUser(snapshot.id)
        drive.deleteByUser(manifestFile.id)
        drive.uploadSmall(NewFile("bad.db.gz", snapshot.parents.single(), "application/gzip", snapshot.props), dir / "bad.db.gz")
        fs.write(dir / "bad.json") {
            writeUtf8(ManifestCodec.encode(manifest.copy(snapshot = manifest.snapshot.copy(fileName = "bad.db.gz", sha256 = bad.sha256))))
        }
        drive.uploadSmall(NewFile("bad.json", manifestFile.parents.single(), "application/json", manifestFile.props), dir / "bad.json")

        assertFailsWith<RestoreMismatch> { planner().prepare(catalog().list().single()) }

        assertEquals(0, phoneB.backupDao().allEntries().size)
        assertTrue(incomingLeft().isEmpty(), "${incomingLeft()}")
    }

    /** 앞으로 다른 방식으로 줄인 백업은 이 앱이 열지 않고 새 앱이 필요하다고 봄 */
    @Test
    fun an_unknown_encoding_needs_a_newer_app() = runTest {
        backUp(3)
        val listed = catalog().list().single()
        val future = listed.copy(manifest = listed.manifest!!.copy(snapshot = listed.manifest.snapshot.copy(encoding = "zstd")))

        assertFailsWith<RestoreNeedsUpdate> { planner().prepare(future) }
    }

    /** 2.2.0 앱(형식 2까지)은 새 백업을 열지 않고 업데이트가 필요하다고 보여 줌 */
    @Test
    fun older_apps_see_the_new_backup_as_needing_an_update() = runTest {
        backUp(3)
        val manifest = ManifestCodec.decode(drive.bytes(kind(DriveLayout.KIND_MANIFEST).single().id).decodeToString())!!

        assertEquals(3, manifest.formatVersion)
        assertTrue(manifest.formatVersion > OLD_APP_FORMAT)
    }

    private companion object {
        const val OLD_APP_FORMAT = 2
    }
}
