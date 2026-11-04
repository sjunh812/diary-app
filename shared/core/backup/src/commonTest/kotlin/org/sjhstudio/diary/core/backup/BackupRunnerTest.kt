package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
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
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.TokenResult
import org.sjhstudio.diary.core.testing.DriveFault
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class BackupRunnerTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase
    private lateinit var drive: FakeDrive
    private lateinit var store: BackupStore
    private val auth = FakeDriveAuthorizer()
    private val network = FakeNetworkState()
    private val notifier = FakeBackupNotifier()
    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = this@BackupRunnerTest.now
    }

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "backup-${Random.nextLong()}"
        fs.createDirectories(dir / "media")
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
        drive = FakeDrive(fs)
        store = BackupStore(InMemoryDataStore(androidx.datastore.preferences.core.emptyPreferences()))
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun runner() = BackupRunner(
        db, drive, auth, store, network, DeviceName { "Galaxy S24" }, fs, dir / "media", dir / "backup",
        BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings(font = "pretendard") }, clock, { TimeZone.UTC },
        BackupAlerts(notifier) { "제목" to (it?.name ?: "까닭 없음") }
    )

    private fun entry(id: String, date: String) =
        EntryEntity(id, date, "21:40", "<p>$id</p>", id, null, null, null, false, 1, 7, null, null, null, null)

    private suspend fun photo(entry: String, order: Int, content: String): String {
        val bytes = content.encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        fs.write(dir / "media" / "$hash.jpg") { write(bytes) }
        db.mediaDao().insertAll(
            listOf(MediaEntity("$entry-$order", entry, order, "normal", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1))
        )
        return hash
    }

    private suspend fun seed(): List<String> {
        db.entryDao().insertAll(listOf(entry("old", "2026-10-01"), entry("new", "2026-11-01")))
        return listOf(photo("old", 0, "옛 사진"), photo("new", 0, "새 사진"))
    }

    private fun names(kind: String) = drive.visible.filter { it.props[DriveLayout.KEY_KIND] == kind }

    private fun manifest() = assertNotNull(ManifestCodec.decode(drive.bytes(names(DriveLayout.KIND_MANIFEST).single().id).decodeToString()))

    @Test
    fun first_backup_uploads_everything_and_says_done_last() = runTest {
        val hashes = seed()

        val outcome = assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(2, outcome.photosUploaded)
        assertEquals(hashes.toSet(), names(DriveLayout.KIND_PHOTO).map { it.name.substringBefore('.') }.toSet())
        assertEquals(1, names(DriveLayout.KIND_SNAPSHOT).size)
        val manifest = manifest()
        assertEquals(2, manifest.counts.entries)
        assertTrue(manifest.media.all { it.inBackup })
        assertEquals(names(DriveLayout.KIND_SNAPSHOT).single().sha256, manifest.snapshot.sha256)
        assertEquals(now, store.current().lastSuccessAt)
        assertEquals(2, db.backupDao().bases().size)
        assertEquals(2, db.backupDao().confirmed("sub-1").size)
    }

    @Test
    fun second_backup_uploads_only_new_photos() = runTest {
        seed()
        runner().run()
        photo("new", 1, "더한 사진")

        val outcome = assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(1, outcome.photosUploaded)
        assertEquals(3, names(DriveLayout.KIND_PHOTO).size)
    }

    @Test
    fun metered_network_keeps_photos_for_wifi_but_backs_up_text() = runTest {
        seed()
        network.isUnmetered = false

        val outcome = assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(0, outcome.photosUploaded)
        assertEquals(2, outcome.photosLeft)
        assertEquals(1, names(DriveLayout.KIND_SNAPSHOT).size)
        assertTrue(manifest().media.none { it.inBackup })
        assertEquals(2, store.current().photosNotBackedUp)
    }

    @Test
    fun cellular_once_uploads_photos() = runTest {
        seed()
        network.isUnmetered = false

        assertEquals(2, assertIs<BackupOutcome.Done>(runner().run(cellularOnce = true)).photosUploaded)
    }

    @Test
    fun low_space_uploads_recent_photos_first() = runTest {
        val (old, new) = seed()
        // README 6, 스냅샷, 목록 파일 몫 1MiB에 사진 한 장(10바이트)이 겨우 들어가는 공간이다.
        val snapshot = SnapshotWriter(db, fs).write(dir / "probe.db").hashes.byteSize
        drive.limit = 6 + snapshot + 1024L * 1024 + 15

        val outcome = assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(1, outcome.photosLeft)
        assertEquals(listOf(new), names(DriveLayout.KIND_PHOTO).map { it.name.substringBefore('.') })
        assertTrue(manifest().media.single { it.sha256 == old }.inBackup.not())
    }

    @Test
    fun no_room_for_snapshot_fails_without_a_manifest() = runTest {
        seed()
        drive.limit = 10

        assertEquals(BackupFailure.NO_SPACE, assertIs<BackupOutcome.Failed>(runner().run()).kind)
        assertTrue(names(DriveLayout.KIND_MANIFEST).isEmpty())
        assertNull(store.current().lastSuccessAt)
        assertEquals(BackupFailure.NO_SPACE, store.current().lastFailure)
    }

    @Test
    fun corrupted_snapshot_never_reports_done() = runTest {
        seed()
        val first = assertIs<BackupOutcome.Done>(runner().run())
        assertEquals(2, first.photosUploaded)
        val before = store.current().lastSuccessAt
        now = Instant.parse("2026-11-06T01:00:00Z")
        drive.corruptNames += "2026-11-06_01-00-00_Galaxy-S24_${store.deviceId().take(4)}.db"

        assertEquals(BackupFailure.DRIVE_ERROR, assertIs<BackupOutcome.Failed>(runner().run()).kind)
        assertEquals(before, store.current().lastSuccessAt)
        assertEquals(1, names(DriveLayout.KIND_MANIFEST).size)
    }

    @Test
    fun md5_is_used_when_drive_gives_no_sha256() = runTest {
        seed()
        drive.omitSha256 = true

        assertIs<BackupOutcome.Done>(runner().run())
    }

    @Test
    fun needs_user_stops_with_auth() = runTest {
        seed()
        auth.tokenResult = TokenResult.NeedsUser

        assertEquals(BackupFailure.AUTH, assertIs<BackupOutcome.Failed>(runner().run()).kind)
        assertEquals(0, drive.visible.size)
    }

    @Test
    fun offline_mid_way_fails_and_next_run_finishes() = runTest {
        seed()
        drive.faults += DriveFault.OFFLINE

        assertEquals(BackupFailure.OFFLINE, assertIs<BackupOutcome.Failed>(runner().run()).kind)
        assertNotNull(store.current().failingSince)

        assertIs<BackupOutcome.Done>(runner().run())
        assertNull(store.current().failingSince)
    }

    @Test
    fun photo_deleted_on_drive_is_uploaded_again() = runTest {
        seed()
        runner().run()
        val gone = names(DriveLayout.KIND_PHOTO).first()
        drive.deleteByUser(gone.id)

        assertEquals(1, assertIs<BackupOutcome.Done>(runner().run()).photosUploaded)
        assertEquals(2, names(DriveLayout.KIND_PHOTO).size)
    }

    @Test
    fun keeps_five_recent_and_trashes_older_same_month() = runTest {
        seed()
        repeat(8) { i ->
            now = Instant.parse("2026-11-${(i + 5).toString().padStart(2, '0')}T01:00:00Z")
            assertIs<BackupOutcome.Done>(runner().run())
        }

        val manifests = names(DriveLayout.KIND_MANIFEST)
        assertTrue(manifests.size in 5..6, "${manifests.size}")
        assertEquals(manifests.size, names(DriveLayout.KIND_SNAPSHOT).size)
    }

    @Test
    fun each_success_moves_the_alert_seven_days_ahead() = runTest {
        runner().run()

        assertEquals(now + 7.days, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }

    @Test
    fun failure_keeps_the_time_and_changes_the_reason() = runTest {
        runner().run()
        val first = now
        now += 2.days
        drive.faults += DriveFault.NO_SPACE
        drive.faults += DriveFault.NO_SPACE

        runner().run()

        assertEquals(first + 7.days, notifier.at)
        assertEquals("NO_SPACE", notifier.body)
    }

    @Test
    fun past_alert_is_not_scheduled_again() = runTest {
        runner().run()
        val first = notifier.at
        now += 8.days
        drive.faults += DriveFault.NO_SPACE
        drive.faults += DriveFault.NO_SPACE

        runner().run()

        assertEquals(first, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }
}
