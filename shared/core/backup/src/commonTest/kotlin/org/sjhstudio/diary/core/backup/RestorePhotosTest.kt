package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Timeout
import okio.blackholeSink
import okio.buffer
import okio.openZip
import okio.use
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveUnauthorized
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * 사진을 받다가 일이 꼬였을 때 남은 사진이 돌아오는지 확인
 * 파일 복원 중 다른 파일 고르기, 앱 종료, 공간 부족, 네트워크 끊김
 */
class RestorePhotosTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-photos-${Random.nextLong()}"
        drive = FakeDrive(fs)
        a = TestPhone(dir / "a", drive, name = "A")
        b = TestPhone(dir / "b", drive, name = "B")
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    /** 일기 둘과 사진 둘을 묶음 파일로 냄 */
    private suspend fun export(name: String = "export.zip"): Path {
        a.write("one")
        a.write("two")
        a.photo("one", 0, "첫째 사진")
        a.photo("two", 0, "둘째 사진")
        val zip = dir / name
        a.exportTo(zip)
        return zip
    }

    private fun other(name: String, text: String): Path {
        val path = dir / name
        fs.write(path) { writeUtf8(text) }
        return path
    }

    private suspend fun RestoreController.pick(path: Path) = prepareFile(fs.source(path))

    // 복원 중 다른 파일 고르기

    @Test
    fun picking_another_file_during_a_restore_is_refused_and_every_photo_arrives() = runTest {
        val zip = export()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        // 백업이 돌고 있어 복원이 시작만 하고 기다림
        b.gate.enter()
        controller.merge()

        val second = controller.pick(other("second.zip", "다른 파일"))

        assertEquals(RestoreFailure.BUSY, assertIs<RestorePreparation.Failed>(second).reason)
        assertEquals(RestoreFailure.BUSY, assertIs<RestorePreparation.Failed>(controller.prepare("snapshot")).reason)
        b.gate.leave()
        val result = assertIs<RestoreState.Done>(controller.finished()).result
        assertEquals(2, result.photosRestored)
        assertEquals(0, result.photosMissing)
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    // 파일 복원이 끊긴 뒤

    private val held get() = b.work / "unpacking" / "bundle.zip"

    /** 사진을 꺼내다 실패하는 기기로 복원 */
    private suspend fun cutFileRestore(zip: Path, scope: CoroutineScope): RestoreResult {
        val cut = b.controller(scope, files = JammedFiles(fs) { it.name.endsWith(".part") })
        assertIs<RestorePreparation.Ready>(cut.pick(zip))
        cut.merge()
        val result = assertIs<RestoreState.Done>(cut.finished()).result
        cut.dismiss()
        gateFree()
        return result
    }

    private suspend fun gateFree() = until { b.gate.tryEnter().also { if (it) b.gate.leave() } }

    @Test
    fun a_file_restore_cut_midway_keeps_the_file_and_finishes_when_the_app_opens_again() = runTest {
        val zip = export()
        val result = cutFileRestore(zip, backgroundScope)
        assertEquals(2, result.photosPending)
        assertTrue(fs.exists(held), "남은 사진이 든 파일을 지웠다")

        // 앱 다시 열기
        // 드라이브를 연결하지 않은 사람도 이어서 꺼냄
        b.controller(backgroundScope).resumePhotos()

        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
        assertFalse(fs.exists(held), "다 꺼낸 파일이 남았다")
    }

    @Test
    fun picking_a_new_file_does_not_touch_the_file_being_unpacked() = runTest {
        val zip = export()
        cutFileRestore(zip, backgroundScope)

        val second = b.controller(backgroundScope)
        assertIs<RestorePreparation.Failed>(second.pick(other("second.zip", "다른 파일")))
        second.dismiss()
        until { fs.listOrNull(b.work / "incoming").orEmpty().isEmpty() }
        second.resumePhotos()

        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    @Test
    fun a_kept_file_goes_away_after_a_week_and_the_entries_stay() = runTest {
        val zip = export()
        cutFileRestore(zip, backgroundScope)

        b.clock.advance(8.days)
        b.controller(backgroundScope).resumePhotos()

        assertFalse(fs.exists(held), "일기 사본이 남았다")
        assertEquals(setOf("one", "two"), b.ids())
        assertEquals(setOf(MediaStatus.PENDING.id), b.photoStates().values.toSet())
    }

    @Test
    fun running_out_of_space_while_unpacking_says_so_and_resumes_later() = runTest {
        val zip = export()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        b.freeBytes = 0
        controller.merge()

        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(2, result.photosPending)
        assertEquals(PhotoStop.NO_SPACE, result.photosStop)
        controller.dismiss()
        gateFree()
        assertTrue(fs.exists(held))
        b.freeBytes = 1_000_000_000
        b.controller(backgroundScope).resumePhotos()
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    @Test
    fun a_photo_that_is_not_in_the_file_is_not_called_a_lack_of_space() = runTest {
        val zip = export()
        val thin = dir / "thin.zip"
        val archive = fs.openZip(zip)
        val root = "/".toPath()
        val items =
            listOf(BundleExporter.MANIFEST, BundleExporter.SNAPSHOT, "media/${TestPhone.hashOf("첫째 사진")}.jpg").mapIndexed { index, name ->
                val path = dir / "item$index"
                fs.write(path) { write(archive.read(root / name) { readByteArray() }) }
                BundleItem(name, path)
            }
        fs.sink(thin).buffer().use { BundleWriter(fs).write(it, items) }
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(thin))
        controller.merge()

        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(1, result.photosPending)
        assertNull(result.photosStop)
        assertFalse(fs.exists(held))
    }

    @Test
    fun running_out_of_space_while_downloading_says_so() = runTest {
        a.write("one")
        a.photo("one", 0, "첫째 사진")
        a.clock.advance(1.days)
        assertIs<BackupOutcome.Done>(a.runner().run())
        val controller = b.controller(backgroundScope)
        controller.prepare(controller.backups()!!.first().snapshotName)
        b.freeBytes = 0
        controller.merge()

        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(1, result.photosPending)
        assertEquals(PhotoStop.NO_SPACE, result.photosStop)
    }

    // 받은 수로 말하기

    /** 일기 둘, 사진 둘을 드라이브에 올림 */
    private suspend fun backedUp() {
        a.write("one")
        a.write("two")
        a.photo("one", 0, "첫째 사진")
        a.photo("two", 0, "둘째 사진")
        a.clock.advance(1.days)
        assertIs<BackupOutcome.Done>(a.runner().run())
    }

    private suspend fun RestoreController.restoreLatest(): RestoreResult {
        prepare(backups()!!.first().snapshotName)
        merge()
        return assertIs<RestoreState.Done>(finished()).result
    }

    @Test
    fun every_photo_received_counts_as_finished() = runTest {
        backedUp()
        val controller = b.controller(backgroundScope)

        controller.restoreLatest()

        assertEquals(PhotoRun(fetched = 2, left = 0, unavailable = 0), controller.lastPhotoRun)
        assertTrue(controller.lastPhotoRun!!.complete)
    }

    @Test
    fun a_photo_gone_from_the_drive_is_not_called_finished_and_is_not_waited_for() = runTest {
        backedUp()
        drive.trash(a.driveFiles(DriveLayout.KIND_PHOTO).first().id)
        val controller = b.controller(backgroundScope)

        val result = controller.restoreLatest()

        assertEquals(PhotoRun(fetched = 1, left = 0, unavailable = 1), controller.lastPhotoRun)
        assertFalse(controller.lastPhotoRun!!.complete)
        assertEquals(0, result.photosPending)
        assertEquals(1, result.photosOnOriginal)
    }

    @Test
    fun a_cut_download_reports_what_really_arrived() = runTest {
        backedUp()
        val controller = b.controller(backgroundScope, photos = CutDrive(drive, after = 1))

        val result = controller.restoreLatest()

        val run = assertNotNull(controller.lastPhotoRun)
        assertFalse(run.complete)
        assertEquals(result.photosRestored, run.fetched)
        assertEquals(result.photosPending, run.left)
        assertTrue(run.left > 0)
    }

    // 드라이브 사진 이어 받기

    /** 사진 둘을 받다 끊긴 기기 */
    private suspend fun cutDriveRestore(scope: CoroutineScope): RestoreResult {
        backedUp()
        b.store.update { it.copy(accountKey = "sub-1") }
        val cut = b.controller(scope, photos = CutDrive(drive, after = 0))
        val result = cut.restoreLatest()
        cut.dismiss()
        gateFree()
        return result
    }

    private suspend fun RestoreController.waiting() = waitingPhotos.first()

    @Test
    fun a_cut_download_waits_for_the_network_by_itself() = runTest {
        val result = cutDriveRestore(backgroundScope)

        assertEquals(PhotoStop.NETWORK, result.photosStop)
        assertEquals(listOf(true), b.scheduler.photoWaits)
        assertEquals(WaitingPhotos(2, PhotoStop.NETWORK), b.controller(backgroundScope).waiting())
    }

    @Test
    fun opening_the_app_on_mobile_data_says_it_waits_for_wifi_and_asks_the_system_to_wake_it() = runTest {
        cutDriveRestore(backgroundScope)
        b.network.isUnmetered = false
        val counting = CountingDownloads(drive)
        val controller = b.controller(backgroundScope, photos = counting)

        assertTrue(controller.resumePhotos(), "기다리는 작업이 다시 불려야 한다")

        assertEquals(0, counting.downloads)
        assertEquals(WaitingPhotos(2, PhotoStop.WIFI), controller.waiting())
        assertEquals(listOf(true, true), b.scheduler.photoWaits)
    }

    @Test
    fun opening_the_app_offline_says_it_waits_for_the_network() = runTest {
        cutDriveRestore(backgroundScope)
        b.network.isConnected = false
        val controller = b.controller(backgroundScope)

        assertTrue(controller.resumePhotos())

        assertEquals(WaitingPhotos(2, PhotoStop.NETWORK), controller.waiting())
    }

    @Test
    fun someone_who_allows_mobile_data_keeps_receiving_and_waits_for_any_network() = runTest {
        cutDriveRestore(backgroundScope)
        b.store.update { it.copy(photosOnCellular = true) }
        b.network.isConnected = false
        b.controller(backgroundScope).resumePhotos()

        assertEquals(false, b.scheduler.photoWaits.last())

        b.network.isConnected = true
        b.network.isUnmetered = false
        assertFalse(b.controller(backgroundScope).resumePhotos())
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    @Test
    fun fetch_now_receives_on_mobile_data_once() = runTest {
        cutDriveRestore(backgroundScope)
        b.network.isUnmetered = false
        val controller = b.controller(backgroundScope)

        controller.fetchPhotosNow()

        until { b.photoStates().values.toSet() == setOf(MediaStatus.NORMAL.id) }
        until { controller.waiting() == null }
    }

    @Test
    fun running_out_of_space_is_shown_as_such_and_does_not_wait_for_the_network() = runTest {
        cutDriveRestore(backgroundScope)
        b.freeBytes = 0
        val waits = b.scheduler.photoWaits.size
        val controller = b.controller(backgroundScope)

        assertFalse(controller.resumePhotos())

        assertEquals(WaitingPhotos(2, PhotoStop.NO_SPACE), controller.waiting())
        assertEquals(waits, b.scheduler.photoWaits.size)
    }

    @Test
    fun a_photo_only_the_original_phone_has_is_not_counted_or_retried() = runTest {
        backedUp()
        drive.trash(a.driveFiles(DriveLayout.KIND_PHOTO).first().id)
        b.store.update { it.copy(accountKey = "sub-1") }
        val controller = b.controller(backgroundScope)
        controller.restoreLatest()
        controller.dismiss()
        gateFree()
        val kept = b.scheduler.restoreKept
        val again = b.controller(backgroundScope)

        assertFalse(again.resumePhotos())

        assertNull(again.waiting())
        assertEquals(kept, b.scheduler.restoreKept, "받을 사진이 없는데 진행 알림을 띄웠다")
        assertTrue(b.scheduler.photoWaits.isEmpty())
    }

    @Test
    fun a_lost_account_is_shown_as_such() = runTest {
        cutDriveRestore(backgroundScope)
        val controller = b.controller(backgroundScope, photos = DeniedDrive(drive))

        assertFalse(controller.resumePhotos())

        assertEquals(WaitingPhotos(2, PhotoStop.ACCOUNT), controller.waiting())
    }

    @Test
    fun someone_without_a_drive_is_asked_to_pick_the_file_again() = runTest {
        val zip = export()
        cutFileRestore(zip, backgroundScope)
        b.clock.advance(8.days)
        val controller = b.controller(backgroundScope)

        controller.resumePhotos()

        assertEquals(WaitingPhotos(2, PhotoStop.FILE), controller.waiting())
    }

    @Test
    fun nothing_waits_once_every_photo_arrived() = runTest {
        cutDriveRestore(backgroundScope)
        val controller = b.controller(backgroundScope)

        assertFalse(controller.resumePhotos())

        assertNull(controller.waiting())
        assertNull(b.store.current().photoStop)
    }

    // 파일을 불러올 공간

    @Test
    fun a_file_too_big_for_the_phone_is_refused_before_copying_with_the_number() = runTest {
        val zip = export()
        val size = fs.metadata(zip).size!!
        b.freeBytes = size
        val controller = b.controller(backgroundScope)

        val failed = assertIs<RestorePreparation.Failed>(controller.prepareFile(fs.source(zip), size))

        assertEquals(RestoreFailure.NO_SPACE, failed.reason)
        assertEquals(10L * 1024 * 1024, failed.bytesShort)
        assertTrue(fs.listOrNull(b.work / "incoming").orEmpty().isEmpty())
    }

    @Test
    fun a_file_that_fits_is_read_as_usual() = runTest {
        val zip = export()
        val size = fs.metadata(zip).size!!
        b.freeBytes = size + 11L * 1024 * 1024

        assertIs<RestorePreparation.Ready>(b.controller(backgroundScope).prepareFile(fs.source(zip), size))
    }

    // 백업이 끝나기를 기다리는 복원

    @Test
    fun a_restore_says_it_waits_for_the_backup_and_starts_when_the_backup_ends() = runTest {
        val zip = export()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        b.gate.enter()

        controller.merge()
        until { controller.state.value == RestoreState.WaitingForBackup }
        // 기다리는 동안 복원을 두 번 시작하거나 닫아도 사라지지 않음
        controller.merge()
        controller.dismiss()
        assertEquals(RestoreState.WaitingForBackup, controller.state.value)

        b.gate.leave()
        assertEquals(2, assertIs<RestoreState.Done>(controller.finished()).result.entriesAdded)
    }

    @Test
    fun a_restore_with_nothing_running_does_not_pass_through_waiting() = runTest {
        val zip = export()
        val seen = mutableListOf<RestoreState>()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        backgroundScope.launch(Dispatchers.Unconfined) { controller.state.collect { seen += it } }

        controller.merge()
        controller.finished()

        assertFalse(RestoreState.WaitingForBackup in seen)
    }

    // 되돌리기

    /** 바꾸기를 마쳐 되돌릴 수 있는 기기 */
    private suspend fun replaced(scope: CoroutineScope): RestoreController {
        val zip = export()
        b.write("mine")
        val controller = b.controller(scope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        controller.replace()
        assertIs<RestoreState.Done>(controller.finished())
        gateFree()
        return controller
    }

    @Test
    fun a_rollback_during_a_restore_waits_and_then_runs() = runTest {
        val controller = replaced(backgroundScope)
        b.gate.enter()

        val rolling = backgroundScope.async { controller.rollBack() }
        until { controller.rollbackRun.value == RollbackRun.WAITING }
        assertEquals(setOf("one", "two"), b.ids())

        b.gate.leave()

        assertTrue(rolling.await())
        assertEquals(RollbackRun.IDLE, controller.rollbackRun.value)
        assertEquals(setOf("mine"), b.ids())
    }

    @Test
    fun a_rollback_that_fails_says_so_and_can_be_tried_again() = runTest {
        val controller = replaced(backgroundScope)
        val copy = fs.list(b.work / "rollback").single() / "diary.db"
        val moved = dir / "moved.db"
        fs.atomicMove(copy, moved)

        assertFalse(controller.rollBack())
        assertEquals(RollbackRun.FAILED, controller.rollbackRun.value)

        fs.atomicMove(moved, copy)
        assertTrue(controller.rollBack())
        assertEquals(RollbackRun.IDLE, controller.rollbackRun.value)
    }

    @Test
    fun pressing_rollback_twice_runs_it_once() = runTest {
        val controller = replaced(backgroundScope)
        b.gate.enter()
        val first = backgroundScope.async { controller.rollBack() }
        until { controller.rollbackRun.value == RollbackRun.WAITING }

        assertFalse(controller.rollBack())

        b.gate.leave()
        assertTrue(first.await())
    }

    @Test
    fun a_finished_rollback_closes_the_result_of_the_replace() = runTest {
        val controller = replaced(backgroundScope)

        assertTrue(controller.rollBack())

        assertEquals(RestoreState.Idle, controller.state.value)
    }

    // 파일로 백업하기

    private fun nowhere(): () -> Sink = { blackholeSink() }

    @Test
    fun a_finished_file_leaves_no_mark() = runTest {
        a.write("one")
        val controller = a.controller(backgroundScope)

        controller.writeBundle(nowhere()) { _, _ -> }

        assertFalse(controller.takeStoppedBundle())
    }

    @Test
    fun a_file_cut_by_the_app_closing_is_told_once_next_time() = runTest {
        a.write("one")
        a.store.update { it.copy(bundleWriting = true) }
        val next = a.controller(backgroundScope)

        assertTrue(next.takeStoppedBundle())
        assertFalse(next.takeStoppedBundle())
    }

    @Test
    fun a_file_being_written_right_now_is_not_called_stopped() = runTest {
        a.write("one")
        a.photo("one", 0, "첫째 사진")
        val controller = a.controller(backgroundScope)
        var told = false

        controller.writeBundle(nowhere()) { _, _ -> told = controller.bundleBeingWritten }

        assertTrue(told)
        assertFalse(controller.takeStoppedBundle())
    }

    @Test
    fun a_file_that_fails_while_writing_leaves_no_mark_because_the_screen_says_it() = runTest {
        a.write("one")
        val controller = a.controller(backgroundScope)
        val failing: () -> Sink = { FailingSink() }

        assertFailsWith<IOException> { controller.writeBundle(failing) { _, _ -> } }

        assertFalse(controller.takeStoppedBundle())
    }

    @Test
    fun stopping_a_file_stops_at_the_next_item_and_keeps_the_mark() = runTest {
        a.write("one")
        repeat(3) { a.photo("one", it, "사진 $it") }
        val controller = a.controller(backgroundScope)
        var items = 0

        val job =
            launch(Dispatchers.Default) {
                controller.writeBundle(nowhere()) { _, _ ->
                    items++
                    if (items == 1) this.cancel()
                }
            }
        job.join()

        assertTrue(items < 6, "멈추라는데 끝까지 썼다")
        assertTrue(a.store.current().bundleWriting)
        assertEquals(emptyList(), fs.listOrNull(a.work / "bundle").orEmpty())
    }

    // 찾을 수 없는 사진 다시 받기

    @Test
    fun a_photo_marked_missing_comes_back_when_the_same_file_is_restored_again() = runTest {
        val zip = export()
        val first = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(first.pick(zip))
        first.merge()
        assertIs<RestoreState.Done>(first.finished())
        first.dismiss()
        // 받던 파일이 깨져 찾을 수 없는 사진으로 굳음
        val photos = b.db.backupDao().allMedia()
        photos.forEach { row ->
            fs.delete(b.media / requireNotNull(row.fileName))
            b.db.mediaDao().update(row.copy(status = MediaStatus.MISSING.id))
        }

        // 앱의 복원기는 하나
        // 둘을 만들면 앞 복원기가 뒤늦게 지우는 사본이 뒤 복원기의 파일과 부딪힘
        val ready = assertIs<RestorePreparation.Ready>(first.pick(zip))
        assertEquals(2, ready.preview.photosToDownload)
        first.merge()

        val result = assertIs<RestoreState.Done>(first.finished()).result
        assertEquals(2, result.photosRestored)
        assertEquals(0, result.photosMissing)
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
        photos.forEach { assertEquals(it.sha256, fs.hashes(b.media / requireNotNull(it.fileName)).sha256) }
    }

    @Test
    fun a_missing_photo_comes_back_from_the_drive_too() = runTest {
        a.write("one")
        a.photo("one", 0, "첫째 사진")
        a.clock.advance(1.days)
        assertIs<BackupOutcome.Done>(a.runner().run())
        val first = b.controller(backgroundScope)
        first.prepare(first.backups()!!.first().snapshotName)
        first.merge()
        assertIs<RestoreState.Done>(first.finished())
        first.dismiss()
        val row = b.db.backupDao().allMedia().single()
        fs.delete(b.media / requireNotNull(row.fileName))
        b.db.mediaDao().update(row.copy(status = MediaStatus.MISSING.id))

        val again = b.controller(backgroundScope)
        val ready = assertIs<RestorePreparation.Ready>(again.prepare(again.backups()!!.first().snapshotName))
        assertEquals(1, ready.preview.photosToDownload)
        again.merge()

        assertEquals(1, assertIs<RestoreState.Done>(again.finished()).result.photosRestored)
        assertEquals(MediaStatus.NORMAL.id, b.db.backupDao().allMedia().single().status)
    }

    @Test
    fun a_missing_photo_without_a_hash_stays_as_it_is() = runTest {
        val zip = export()
        b.write("mine")
        val lost = TestPhone.photoRow("mine", 0, "없는 사진").copy(status = MediaStatus.MISSING.id, sha256 = null, fileName = null)
        b.db.mediaDao().insertAll(listOf(lost))

        val controller = b.controller(backgroundScope)
        val ready = assertIs<RestorePreparation.Ready>(controller.pick(zip))
        assertEquals(2, ready.preview.photosToDownload)
        controller.merge()
        controller.finished()

        assertEquals(MediaStatus.MISSING.id, b.photoStates()["mine-0"])
    }
}

/** 받은 사진 수 계산 */
private class CountingDownloads(private val real: DriveApi) : DriveApi by real {
    var downloads = 0
        private set

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        downloads++
        return real.download(fileId, to, fromByte)
    }
}

/** 로그인이 풀린 드라이브 */
private class DeniedDrive(private val real: DriveApi) : DriveApi by real {
    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long = throw DriveUnauthorized()
}

/** 첫 글자부터 쓰지 못하는 저장 위치 */
private class FailingSink : Sink {
    override fun write(source: Buffer, byteCount: Long) = throw IOException("저장 공간이 없다")

    override fun flush() = Unit

    override fun timeout() = Timeout.NONE

    override fun close() = Unit
}
