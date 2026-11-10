package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.backup.TestPhone.Companion.photoRow
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveErrorMapper
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.TokenResult
import org.sjhstudio.diary.core.testing.DriveFault
import org.sjhstudio.diary.core.testing.FakeDrive

/** 백업이 실패하거나 사진이 이상할 때 드라이브와 기기가 어떻게 남는지 확인 */
class BackupFailureTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var phone: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "backup-failure-${Random.nextLong()}"
        drive = FakeDrive(fs)
        phone = TestPhone(dir / "phone", drive)
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        fs.deleteRecursively(dir)
    }

    private fun manifests() = phone.driveFiles(DriveLayout.KIND_MANIFEST)

    private fun photos() = phone.driveFiles(DriveLayout.KIND_PHOTO)

    private fun manifestAt(index: Int) = assertNotNull(ManifestCodec.decode(drive.bytes(manifests()[index].id).decodeToString()))

    private suspend fun done() = assertIs<BackupOutcome.Done>(phone.runner().run())

    // 기기의 사진이 이상할 때

    @Test
    fun a_photo_gone_from_the_device_is_not_called_a_full_drive() = runTest {
        phone.write("diary")
        val gone = phone.photo("diary", 0, "지워진 사진")
        phone.photo("diary", 1, "남은 사진")
        fs.delete(phone.media / "$gone.jpg")

        val outcome = done()

        assertEquals(1, outcome.photosUploaded)
        assertEquals(0, outcome.photosLeft)
        assertEquals(1, outcome.photosUnusable)
        assertNull(phone.store.current().lastFailure)
        assertEquals(0, phone.store.current().bytesShort)
        assertFalse(manifestAt(0).media.first { it.sha256 == gone }.inBackup)
    }

    @Test
    fun a_damaged_photo_on_the_device_is_not_called_backed_up() = runTest {
        phone.write("diary")
        val damaged = phone.photo("diary", 0, "깨질 사진")
        val good = phone.photo("diary", 1, "멀쩡한 사진")
        fs.write(phone.media / "$damaged.jpg") { writeUtf8("다른 내용") }

        val outcome = done()

        assertEquals(1, outcome.photosUploaded)
        assertEquals(0, outcome.photosLeft)
        assertEquals(1, outcome.photosUnusable)
        assertEquals(setOf("$good.jpg"), photos().map { it.name }.toSet())
        assertFalse(manifestAt(0).media.first { it.sha256 == damaged }.inBackup)
        assertNull(phone.store.current().lastFailure)
    }

    @Test
    fun an_empty_photo_file_is_not_called_backed_up() = runTest {
        phone.write("diary")
        val empty = phone.photo("diary", 0, "비게 될 사진")
        fs.write(phone.media / "$empty.jpg") {}

        val outcome = done()

        assertEquals(0, outcome.photosUploaded)
        assertEquals(0, outcome.photosLeft)
        assertEquals(1, outcome.photosUnusable)
        assertTrue(photos().isEmpty())
    }

    // 기기에서 깨진 사진

    /** 일기 하나에 사진 둘, 하나는 파일이 깨짐 */
    private suspend fun brokenPhoto(): String {
        phone.write("diary")
        phone.photo("diary", 0, "멀쩡한 사진")
        val hash = phone.photo("diary", 1, "깨질 사진")
        fs.write(phone.media / "$hash.jpg") { writeUtf8("다른 내용") }
        return hash
    }

    @Test
    fun a_broken_photo_is_counted_apart_and_is_not_waiting_for_wifi() = runTest {
        val hash = brokenPhoto()

        val outcome = assertIs<BackupOutcome.Done>(phone.runner().run())

        assertEquals(0, outcome.photosLeft)
        assertEquals(1, outcome.photosUnusable)
        assertEquals(0, phone.store.current().photosNotBackedUp)
        assertEquals(setOf(hash), phone.store.current().unusablePhotos)
    }

    @Test
    fun a_photo_whose_file_is_gone_is_counted_the_same() = runTest {
        val hash = brokenPhoto()
        fs.delete(phone.media / "$hash.jpg")

        val outcome = assertIs<BackupOutcome.Done>(phone.runner().run())

        assertEquals(1, outcome.photosUnusable)
    }

    @Test
    fun a_broken_photo_does_not_make_a_new_backup_every_day() = runTest {
        brokenPhoto()
        done()
        val kept = manifests().map { it.name }

        repeat(6) {
            phone.clock.advance(1.days)
            assertEquals(BackupOutcome.Unchanged, phone.runner().runIfChanged())
        }

        assertEquals(kept, manifests().map { it.name }, "같은 내용 백업이 지난 시점을 밀어냈다")
    }

    @Test
    fun a_new_entry_still_backs_up_with_a_broken_photo_around() = runTest {
        brokenPhoto()
        done()
        phone.clock.advance(1.days)
        phone.write("new")

        assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())
    }

    @Test
    fun backup_now_still_runs_with_a_broken_photo_around() = runTest {
        brokenPhoto()
        done()
        phone.clock.advance(1.days)

        assertIs<BackupOutcome.Done>(phone.runner().run())

        assertEquals(2, manifests().size)
    }

    @Test
    fun photos_waiting_for_wifi_do_not_make_a_new_backup_every_day() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        phone.network.isUnmetered = false
        assertEquals(1, assertIs<BackupOutcome.Done>(phone.runner().run()).photosLeft)
        val kept = manifests().map { it.name }

        repeat(6) {
            phone.clock.advance(1.days)
            assertEquals(BackupOutcome.Unchanged, phone.runner().runIfChanged())
        }

        assertEquals(kept, manifests().map { it.name })
        assertEquals(1, phone.store.current().photosNotBackedUp)
    }

    @Test
    fun photos_that_do_not_fit_on_the_drive_do_not_make_a_new_backup_every_day() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진".repeat(1_000))
        done()
        drive.limit = drive.visible.sumOf { it.size ?: 0 } + 2 * 1024 * 1024
        phone.photo("diary", 1, "큰 사진".repeat(1_000_000))
        phone.db.entryDao().update(TestPhone.entry("diary", updated = 11))
        val second = assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())
        assertEquals(1, second.photosLeft)
        val kept = manifests().map { it.name }

        repeat(3) {
            phone.clock.advance(1.days)
            assertEquals(BackupOutcome.Unchanged, phone.runner().runIfChanged())
        }

        assertEquals(kept, manifests().map { it.name })
        assertEquals(BackupFailure.NO_SPACE, phone.store.current().lastFailure)
        assertTrue(phone.store.current().bytesShort > 0)
    }

    @Test
    fun a_photo_fixed_later_goes_up_on_the_next_backup() = runTest {
        phone.write("diary")
        val hash = phone.photo("diary", 0, "깨질 사진")
        fs.write(phone.media / "$hash.jpg") { writeUtf8("다른 내용") }
        done()
        fs.write(phone.media / "$hash.jpg") { writeUtf8("깨질 사진") }
        phone.clock.advance(1.days)

        val outcome = assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())

        assertEquals(1, outcome.photosUploaded)
        assertEquals(0, outcome.photosLeft)
    }

    @Test
    fun a_photo_shared_by_many_entries_goes_up_once() = runTest {
        repeat(5) {
            phone.write("e$it")
            phone.db.mediaDao().insertAll(listOf(photoRow("e$it", 0, "같은 사진")))
        }
        fs.write(phone.media / "${TestPhone.hashOf("같은 사진")}.jpg") { writeUtf8("같은 사진") }

        val outcome = done()

        assertEquals(1, outcome.photosUploaded)
        assertEquals(1, photos().size)
        assertEquals(1, manifestAt(0).media.size)
    }

    @Test
    fun a_photo_attached_again_is_not_sent_twice() = runTest {
        phone.write("first")
        phone.photo("first", 0, "같은 사진")
        done()
        phone.write("second")
        phone.db.mediaDao().insertAll(listOf(photoRow("second", 0, "같은 사진")))
        phone.clock.advance(1.days)

        assertEquals(0, done().photosUploaded)
        assertEquals(1, photos().size)
    }

    // 드라이브 공간

    @Test
    fun text_goes_up_even_when_no_photo_fits_and_the_rest_follow_later() = runTest {
        phone.write("first", date = "2026-11-01")
        phone.write("second", date = "2026-10-01")
        phone.photo("first", 0, "가".repeat(500_000))
        phone.photo("second", 0, "나".repeat(500_000))
        drive.limit = 2_000_000

        val outcome = done()

        assertEquals(0, outcome.photosUploaded)
        assertEquals(2, outcome.photosLeft)
        assertEquals(1, manifests().size)
        assertEquals(BackupFailure.NO_SPACE, phone.store.current().lastFailure)
        assertTrue(phone.store.current().bytesShort > 0)

        drive.limit = null
        phone.clock.advance(1.days)
        val next = assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())

        assertEquals(2, next.photosUploaded)
        assertEquals(0, next.photosLeft)
        assertNull(phone.store.current().lastFailure)
        assertEquals(0, phone.store.current().bytesShort)
    }

    @Test
    fun not_enough_room_for_the_text_by_one_byte_says_exactly_that() = runTest {
        phone.write("diary")
        done()
        val snapshot = assertNotNull(phone.driveFiles(DriveLayout.KIND_SNAPSHOT).single().size)
        val room = snapshot + 1024 * 1024
        phone.clock.advance(1.days)

        drive.limit = drive.usage() + room - 1
        assertEquals(BackupOutcome.Failed(BackupFailure.NO_SPACE), phone.runner().run())
        assertEquals(1, phone.store.current().bytesShort)
        assertEquals(1, manifests().size)

        drive.limit = drive.usage() + room
        phone.clock.advance(1.days)
        done()
        assertEquals(2, manifests().size)
    }

    @Test
    fun a_drive_that_fills_up_after_the_check_keeps_the_last_backup() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        done()
        phone.photo("diary", 1, "새 사진")
        phone.clock.advance(1.days)
        val full = FullDrive(drive)

        assertEquals(BackupOutcome.Failed(BackupFailure.NO_SPACE), phone.runner(full).run())

        assertEquals(1, manifests().size)
        assertEquals(1, photos().size)
        assertEquals(BackupFailure.NO_SPACE, phone.store.current().lastFailure)
    }

    @Test
    fun a_full_drive_changes_nothing_on_the_device() = runTest {
        phone.write("diary")
        val hash = phone.photo("diary", 0, "사진")
        drive.limit = 1

        assertEquals(BackupOutcome.Failed(BackupFailure.NO_SPACE), phone.runner().run())

        assertEquals(setOf("diary"), phone.ids())
        assertEquals("normal", phone.db.backupDao().allMedia().single().status)
        assertEquals(hash, fs.hashes(phone.media / "$hash.jpg").sha256)
    }

    // 계정

    @Test
    fun a_login_that_expires_during_photos_keeps_what_went_up() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "첫째")
        phone.photo("diary", 1, "둘째")
        var cut = false
        drive.onUpload = { meta ->
            if (!cut && meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO) {
                cut = true
                drive.faults.addLast(DriveFault.UNAUTHORIZED)
            }
        }

        assertEquals(BackupOutcome.Failed(BackupFailure.AUTH), phone.runner().run())

        assertTrue(manifests().isEmpty())
        val first = photos().map { it.id }
        assertEquals(1, first.size)
        drive.onUpload = null
        val next = done()
        assertEquals(1, next.photosUploaded)
        assertEquals(2, photos().size)
        assertTrue(first.single() in photos().map { it.id })
    }

    @Test
    fun taking_back_the_permission_asks_to_reconnect() = runTest {
        phone.write("diary")
        val revoked = object : DriveApi by drive {
            override suspend fun about() = throw DriveErrorMapper.map(403, "insufficientPermissions")
        }

        assertEquals(BackupOutcome.Failed(BackupFailure.AUTH), phone.runner(revoked).run())

        assertEquals(BackupFailure.AUTH, phone.store.current().lastFailure)
        assertTrue(manifests().isEmpty())
    }

    @Test
    fun a_token_refused_before_starting_touches_nothing() = runTest {
        phone.write("diary")
        phone.auth.tokenResult = TokenResult.Failed(BackupFailure.OFFLINE)

        assertEquals(BackupOutcome.Failed(BackupFailure.OFFLINE), phone.runner().run())

        assertTrue(drive.visible.isEmpty())
    }

    @Test
    fun a_failure_keeps_the_last_success_time() = runTest {
        phone.write("diary")
        done()
        val success = phone.store.current().lastSuccessAt
        phone.write("next")
        phone.clock.advance(1.days)
        phone.auth.tokenResult = TokenResult.NeedsUser

        assertEquals(BackupOutcome.Failed(BackupFailure.AUTH), phone.runner().run())

        assertEquals(success, phone.store.current().lastSuccessAt)
        assertEquals(phone.clock.now(), phone.store.current().failingSince)
    }

    // 폴더

    @Test
    fun a_folder_deleted_during_a_backup_fails_and_the_next_one_starts_over() = runTest {
        phone.write("diary")
        val gone = object : DriveApi by drive {
            override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
                if (meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_SNAPSHOT) throw DriveNotFound()
                return drive.uploadSmall(meta, body)
            }
        }

        assertEquals(BackupOutcome.Failed(BackupFailure.DRIVE_ERROR), phone.runner(gone).run())
        assertTrue(manifests().isEmpty())

        phone.clock.advance(1.days)
        drive.visible.forEach { drive.deleteByUser(it.id) }
        done()
        assertEquals(1, manifests().size)
    }

    @Test
    fun a_folder_deleted_with_nothing_new_still_backs_up_again() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        done()
        drive.visible.forEach { drive.deleteByUser(it.id) }
        phone.clock.advance(1.days)

        val outcome = assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())

        assertEquals(1, outcome.photosUploaded)
        assertEquals(1, manifests().size)
    }

    @Test
    fun an_automatic_backup_that_cannot_check_the_drive_keeps_waiting_quietly() = runTest {
        phone.write("diary")
        done()
        drive.faults.addLast(DriveFault.OFFLINE)

        assertEquals(BackupOutcome.Unchanged, phone.runner().runIfChanged())

        assertNull(phone.store.current().lastFailure)
    }

    @Test
    fun a_list_file_deleted_by_hand_is_noticed_too() = runTest {
        phone.write("diary")
        done()
        drive.deleteByUser(manifests().single().id)
        phone.clock.advance(1.days)

        assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())

        assertEquals(1, manifests().size)
    }

    @Test
    fun another_account_gets_a_backup_even_with_nothing_new() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        done()
        val other = FakeDrive(fs)
        phone.clock.advance(1.days)

        val outcome = assertIs<BackupOutcome.Done>(phone.runner(other).runIfChanged())

        assertEquals(1, outcome.photosUploaded)
        assertEquals(1, other.visible.count { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST })
    }

    @Test
    fun an_edit_older_than_the_newest_entry_is_still_backed_up() = runTest {
        // 시계가 앞선 기기의 일기가 섞여 있으면 고쳐도 가장 늦은 시각은 그대로
        phone.write("from-the-future", updated = 9_000)
        phone.write("diary", updated = 10)
        done()
        phone.db.entryDao().setFavorite("diary", true, 20)
        phone.clock.advance(1.days)

        assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())

        assertEquals(2, manifests().size)
    }

    @Test
    fun an_edit_made_with_the_clock_set_back_is_still_backed_up() = runTest {
        phone.write("diary", updated = 10)
        done()
        phone.db.entryDao().setFavorite("diary", true, 5)
        phone.clock.advance(1.days)

        assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())
    }

    // 정리

    @Test
    fun cleanup_never_touches_another_devices_backups() = runTest {
        val other = TestPhone(dir / "other", drive, name = "Other")
        try {
            other.write("theirs")
            repeat(8) {
                other.clock.advance(1.days)
                other.db.entryDao().setFavorite("theirs", it % 2 == 0, it.toLong())
                assertIs<BackupOutcome.Done>(other.runner().run())
            }
            val theirs = manifests().size
            phone.write("mine")
            repeat(8) {
                phone.clock.advance(1.days)
                phone.db.entryDao().setFavorite("mine", it % 2 == 0, it.toLong())
                done()
            }

            // 최근 5개에 그보다 옛것 가운데 달마다 1개를 남김
            val mine = manifests().filter { it.name.contains("Galaxy") }
            assertEquals(6, mine.size)
            assertEquals(theirs, manifests().size - mine.size)
        } finally {
            other.close()
        }
    }

    @Test
    fun a_snapshot_left_by_a_cut_goes_after_a_week_and_finished_ones_stay() = runTest {
        phone.write("diary")
        val cut = object : DriveApi by drive {
            override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
                if (meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST) throw DriveNotFound()
                return drive.uploadSmall(meta, body)
            }
        }
        assertIs<BackupOutcome.Failed>(phone.runner(cut).run())
        val orphan = phone.driveFiles(DriveLayout.KIND_SNAPSHOT).single().name
        phone.clock.advance(1.days)

        done()

        val left = phone.driveFiles(DriveLayout.KIND_SNAPSHOT).map { it.name }
        assertEquals(1, left.size)
        assertFalse(orphan in left)
        assertTrue(orphan in drive.trashedNames)
    }

    @Test
    fun cleanup_only_trashes_so_nothing_is_gone_for_good() = runTest {
        phone.write("diary")
        repeat(7) {
            phone.clock.advance(1.days)
            phone.db.entryDao().setFavorite("diary", it % 2 == 0, it.toLong())
            done()
        }

        // 최근 5개에 그보다 옛것 가운데 달마다 1개를 남기고 나머지 하나의 목록 파일과 스냅샷을 치움
        assertEquals(6, manifests().size)
        assertEquals(2, drive.trashedNames.size)
    }

    // 기기 이름

    @Test
    fun a_very_long_device_name_does_not_stop_backups() = runTest {
        phone.write("diary")

        assertIs<BackupOutcome.Done>(phone.runner(deviceName = "아주 긴 이름".repeat(100)).run())

        assertEquals(BackupEntryState.Ready, phone.catalog().list().single().entry.state)
    }

    @Test
    fun odd_device_names_stay_in_the_folder() = runTest {
        phone.write("diary")
        for (name in listOf("../../밖으로", "민수's 폰", "폰 \\ 하나", "  ", "😀 폰", "a:b/c", "..")) {
            phone.clock.advance(1.days)
            phone.db.entryDao().setFavorite("diary", name.length % 2 == 0, phone.clock.now().toEpochMilliseconds())
            assertIs<BackupOutcome.Done>(phone.runner(deviceName = name).run(), name)
        }

        assertTrue(manifests().size >= 5)
        assertEquals(emptyList(), fs.listOrNull(dir).orEmpty().filter { it.name != "phone" })
        manifests().forEach { assertTrue(RestoreNames.isSingleName(it.name), it.name) }
    }

    // 백업 중 일어난 일

    @Test
    fun an_entry_deleted_for_good_during_a_backup_is_only_in_that_backup() = runTest {
        phone.write("gone")
        phone.write("kept")
        phone.photo("gone", 0, "사진")
        var deleted = false
        drive.onUpload = { meta ->
            if (!deleted && meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO) {
                deleted = true
                phone.db.entryDao().deletePermanently("gone")
            }
        }

        done()

        assertEquals(2, manifestAt(0).counts.entries)
        assertEquals(setOf("kept"), phone.ids())
        drive.onUpload = null
        phone.clock.advance(1.days)
        assertIs<BackupOutcome.Done>(phone.runner().runIfChanged())
    }

    @Test
    fun the_uploaded_snapshot_matches_its_list_file_whatever_happens_during_upload() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        var edited = false
        drive.onUpload = { meta ->
            if (!edited && meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO) {
                edited = true
                phone.db.entryDao().setFavorite("diary", true, 99)
            }
        }

        done()

        val snapshot = phone.driveFiles(DriveLayout.KIND_SNAPSHOT).single()
        assertEquals(manifestAt(0).snapshot.sha256, snapshot.sha256)
    }

    @Test
    fun a_failed_backup_leaves_no_copy_of_the_diary_behind() = runTest {
        phone.write("diary")
        drive.limit = 1

        assertIs<BackupOutcome.Failed>(phone.runner().run())

        assertEquals(emptyList(), fs.listOrNull(phone.work / "outgoing").orEmpty())
    }

    @Test
    fun a_device_that_cannot_write_its_work_folder_fails_and_keeps_its_entries() = runTest {
        phone.write("diary")
        // 폴더가 있어야 할 자리에 파일이 있어 쓸 수 없음
        fs.write(phone.work) { writeUtf8("막힌 자리") }

        assertEquals(BackupOutcome.Failed(BackupFailure.LOCAL_IO), phone.runner().run())

        assertEquals(setOf("diary"), phone.ids())
        assertTrue(phone.gate.tryEnter())
    }

    @Test
    fun checksum_mismatch_on_a_photo_never_says_done() = runTest {
        phone.write("diary")
        val hash = phone.photo("diary", 0, "사진")
        drive.corruptNames += "$hash.jpg"

        assertEquals(BackupOutcome.Failed(BackupFailure.DRIVE_ERROR), phone.runner().run())

        assertTrue(photos().isEmpty())
        assertTrue(manifests().isEmpty())
        assertNull(phone.store.current().lastSuccessAt)
    }

    @Test
    fun a_drive_without_sha256_is_checked_by_md5() = runTest {
        phone.write("diary")
        phone.photo("diary", 0, "사진")
        drive.omitSha256 = true

        assertEquals(1, done().photosUploaded)
    }

    private class FullDrive(private val real: DriveApi) : DriveApi by real {
        override suspend fun about() = real.about().copy(limit = null)

        override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
            if (meta.parentId != null) throw DriveErrorMapper.map(403, "storageQuotaExceeded")
            return real.uploadSmall(meta, body)
        }
    }
}
