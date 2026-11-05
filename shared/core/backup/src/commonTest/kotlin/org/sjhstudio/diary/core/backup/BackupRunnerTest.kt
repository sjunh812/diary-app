package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import androidx.sqlite.SQLiteException
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
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
import org.sjhstudio.diary.core.drive.ChunkResult
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.SMALL_UPLOAD_LIMIT
import org.sjhstudio.diary.core.drive.UploadSession
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.DriveAccount
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

    private fun runner(
        api: DriveApi = drive,
        authorizer: FakeDriveAuthorizer = auth,
        settings: suspend () -> ManifestSettings = { ManifestSettings(font = "pretendard") }
    ) = BackupRunner(
        db, api, authorizer, store, network, DeviceName { "Galaxy S24" }, fs, dir / "media", dir / "backup",
        BackupAppInfo("2.0.0", 37, "android"), { "README" }, settings, clock, { TimeZone.UTC },
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
    fun empty_diary_uploads_nothing_so_it_cannot_hide_an_older_backup() = runTest {
        assertIs<BackupOutcome.Empty>(runner().run())

        assertTrue(drive.visible.isEmpty())
        assertEquals(null, store.current().lastSuccessAt)
        assertEquals(null, store.current().lastFailure)
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
        seed()
        runner().run()

        assertEquals(now + 7.days, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }

    @Test
    fun failure_keeps_the_time_and_changes_the_reason() = runTest {
        seed()
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
        seed()
        runner().run()
        val first = notifier.at
        now += 8.days
        drive.faults += DriveFault.NO_SPACE
        drive.faults += DriveFault.NO_SPACE

        runner().run()

        assertEquals(first, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }

    @Test
    fun nothing_changed_means_no_upload() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        runner().run()
        val files = drive.visible.size

        assertEquals(BackupOutcome.Unchanged, runner().runIfChanged())
        assertEquals(files, drive.visible.size)
    }

    @Test
    fun a_new_entry_runs_again() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        runner().run()
        db.entryDao().insertAll(listOf(entry("b", "2026-10-30")))

        assertIs<BackupOutcome.Done>(runner().runIfChanged())
    }

    @Test
    fun photos_left_on_cellular_keep_the_next_run_going() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        photo("a", 0, "내용")
        network.isUnmetered = false
        val first = assertIs<BackupOutcome.Done>(runner().run())
        assertEquals(1, first.photosLeft)
        assertEquals(0, store.current().bytesShort)

        network.isUnmetered = true

        assertEquals(0, assertIs<BackupOutcome.Done>(runner().runIfChanged()).photosLeft)
    }

    @Test
    fun running_out_of_drive_space_saves_the_text_and_says_how_much() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        photo("a", 0, "내용".repeat(2_000_000))
        drive.limit = 8_000_000

        val outcome = assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(1, outcome.photosLeft)
        assertEquals(BackupFailure.NO_SPACE, store.current().lastFailure)
        assertTrue(store.current().bytesShort > 0)
    }

    @Test
    fun cut_before_the_list_file_leaves_the_last_backup_as_it_was() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        runner().run()
        val manifests = drive.visible.count { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST }
        db.entryDao().insertAll(listOf(entry("b", "2026-10-30")))
        now += 1.days
        val cut =
            object : DriveApi by drive {
                override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
                    if (meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST) throw DriveException(BackupFailure.OFFLINE)
                    return drive.uploadSmall(meta, body)
                }
            }

        assertIs<BackupOutcome.Failed>(runner(cut).run())

        assertEquals(manifests, drive.visible.count { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST })
    }

    @Test
    fun photos_already_up_are_not_sent_again_after_a_cut() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        photo("a", 0, "첫째")
        photo("a", 1, "둘째")
        val cut =
            object : DriveApi by drive {
                override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
                    if (meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_SNAPSHOT) throw DriveException(BackupFailure.OFFLINE)
                    return drive.uploadSmall(meta, body)
                }
            }
        assertIs<BackupOutcome.Failed>(runner(cut).run())
        val photos = drive.visible.filter { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO }.map { it.id }

        assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(photos, drive.visible.filter { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO }.map { it.id })
    }

    @Test
    fun deleting_the_drive_folder_starts_over_with_everything() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        photo("a", 0, "내용")
        runner().run()
        drive.deleteByUser(drive.visible.first { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_ROOT }.id)
        drive.visible.forEach { drive.deleteByUser(it.id) }

        assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(1, drive.visible.count { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO })
    }

    @Test
    fun another_account_does_not_inherit_uploaded_photos() = runTest {
        db.entryDao().insertAll(listOf(entry("a", "2026-10-31")))
        photo("a", 0, "내용")
        runner().run()
        val otherDrive = FakeDrive(fs)
        val other = FakeDriveAuthorizer(DriveAccount("other", "other@gmail.com"))

        assertIs<BackupOutcome.Done>(runner(otherDrive, other).run())

        assertEquals(1, otherDrive.visible.count { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO })
    }

    /** 저장 공간 확인에서 뜻밖의 예외를 던지는 드라이브. */
    private class ThrowingDrive(private val real: DriveApi, private val error: Exception) : DriveApi by real {
        override suspend fun about() = throw error
    }

    /** 이어 올리기 세션이 끝나지 않는 드라이브. 고치기 전에는 끝없이 돌아서 시험을 끊으려고 상한을 둔다. */
    private class EndlessUpload(private val real: DriveApi, private val answer: ChunkResult) : DriveApi by real {
        var starts = 0
            private set
        private var chunks = 0

        override suspend fun startResumable(meta: NewFile, size: Long): UploadSession {
            starts++
            return real.startResumable(meta, size)
        }

        override suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult {
            if (++chunks > TOO_MANY_CHUNKS) throw AssertionError("이어 올리기가 끝나지 않는다")
            return answer
        }
    }

    private suspend fun largePhoto() {
        db.entryDao().insertAll(listOf(entry("big", "2026-11-01")))
        val bytes = ByteArray(SMALL_UPLOAD_LIMIT.toInt() + 1)
        val hash = bytes.toByteString().sha256().hex()
        fs.write(dir / "media" / "$hash.jpg") { write(bytes) }
        db.mediaDao().insertAll(listOf(MediaEntity("big-0", "big", 0, "normal", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1)))
    }

    @Test
    fun a_full_disk_is_a_failed_backup_and_not_a_crash() = runTest {
        seed()

        val failed = runner(ThrowingDrive(drive, SQLiteException("database or disk is full"))).run()

        assertEquals(BackupOutcome.Failed(BackupFailure.UNKNOWN), failed)
        assertEquals(BackupFailure.UNKNOWN, store.current().lastFailure)
    }

    @Test
    fun a_web_page_instead_of_json_is_a_failed_backup_and_not_a_crash() = runTest {
        seed()

        val failed = runner(ThrowingDrive(drive, IllegalArgumentException("<html>"))).run()

        assertEquals(BackupOutcome.Failed(BackupFailure.UNKNOWN), failed)
    }

    @Test
    fun the_next_backup_runs_after_an_unexpected_failure() = runTest {
        seed()
        val gate = BackupGate()
        val failing = BackupRunner(
            db, ThrowingDrive(drive, IllegalStateException()), auth, store, network, DeviceName { "A" }, fs, dir / "media", dir / "backup",
            BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings() }, clock, { TimeZone.UTC }, gate = gate
        )

        failing.run()

        assertTrue(gate.tryEnter())
    }

    @Test
    fun an_automatic_backup_that_cannot_even_look_is_a_failed_backup() = runTest {
        seed()

        val failed = runner(settings = { error("일기 내용") }).runIfChanged()

        assertEquals(BackupOutcome.Failed(BackupFailure.UNKNOWN), failed)
    }

    @Test
    fun stopping_a_backup_is_not_recorded_as_a_failure() = runTest {
        seed()

        assertFailsWith<CancellationException> { runner(ThrowingDrive(drive, CancellationException("멈춤"))).run() }

        assertNull(store.current().lastFailure)
    }

    @Test
    fun an_upload_session_that_keeps_expiring_gives_up() = runTest {
        largePhoto()
        val endless = EndlessUpload(drive, ChunkResult.Expired)

        assertIs<BackupOutcome.Failed>(runner(endless).run())

        assertTrue(endless.starts in 2..MOST_STARTS, "세션을 ${endless.starts}번 열었다")
    }

    @Test
    fun an_upload_that_stops_moving_gives_up() = runTest {
        largePhoto()

        assertIs<BackupOutcome.Failed>(runner(EndlessUpload(drive, ChunkResult.Progress(0))).run())
    }

    @Test
    fun a_failed_cleanup_leaves_the_backup_done() = runTest {
        seed()
        drive.onUpload = { meta -> if (meta.mimeType == "application/json") drive.faults.addLast(DriveFault.OFFLINE) }

        assertIs<BackupOutcome.Done>(runner().run())

        assertEquals(now, store.current().lastSuccessAt)
        assertNull(store.current().lastFailure)
    }

    private companion object {
        const val TOO_MANY_CHUNKS = 200
        const val MOST_STARTS = 5
    }
}
