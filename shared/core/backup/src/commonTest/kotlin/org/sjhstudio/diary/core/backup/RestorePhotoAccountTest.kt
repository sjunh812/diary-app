package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.model.AccountChange
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * 복원 사진을 다 받기 전에 다른 구글 계정을 연결했을 때
 * 남은 사진은 그 사진이 온 계정 드라이브에만 있음
 */
class RestorePhotoAccountTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var firstDrive: FakeDrive
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone

    private val first = DriveAccount("sub-1", "first@gmail.com")
    private val second = DriveAccount("sub-2", "second@gmail.com")

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-photo-account-${Random.nextLong()}"
        firstDrive = FakeDrive(fs)
        a = TestPhone(dir / "a", firstDrive, name = "A")
        b = TestPhone(dir / "b", firstDrive, name = "B")
        b.auth.account = first
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    private fun backup() = BackupController(
        b.store,
        lazy { error("쓰지 않는다") },
        lazy { error("쓰지 않는다") },
        b.auth,
        FakeBackupScheduler().apply { takesNow = true },
        FakeBackupNotifier(),
        CoroutineScope(Dispatchers.Unconfined),
        findBackup = { null },
        running = { false },
        gate = b.gate
    )

    /** 첫 계정 드라이브에서 복원하다 사진 둘을 받지 못하고 끊긴 기기 */
    private suspend fun cutRestore(scope: CoroutineScope): RestoreResult {
        a.write("one")
        a.write("two")
        a.photo("one", 0, "첫째 사진")
        a.photo("two", 0, "둘째 사진")
        assertIs<BackupOutcome.Done>(a.runner().run())
        backup().connect()
        val cut = b.controller(scope, photos = CutDrive(firstDrive, after = 0))
        cut.prepare(cut.backups().entriesOrNull!!.first().snapshotName)
        cut.merge()
        val result = assertIs<RestoreState.Done>(cut.finished()).result
        cut.dismiss()
        gateFree()
        return result
    }

    private suspend fun gateFree() = until { b.gate.tryEnter().also { if (it) b.gate.leave() } }

    private suspend fun RestoreController.waiting() = waitingPhotos.first()

    private suspend fun switchTo(account: DriveAccount) {
        b.auth.account = account
        assertEquals(AccountChange.CHANGED, backup().changeAccount())
    }

    private suspend fun pending() = b.db.backupDao().pendingFiles().size

    private val waitingForFirst = WaitingPhotos(2, PhotoStop.OTHER_ACCOUNT, "first@gmail.com")

    @Test
    fun a_drive_restore_remembers_the_account_the_photos_came_from() = runTest {
        cutRestore(backgroundScope)

        val prefs = b.store.current()
        assertEquals("sub-1", prefs.photoAccount)
        assertEquals("first@gmail.com", prefs.photoAccountLabel)
    }

    @Test
    fun changing_the_account_keeps_the_photos_waiting_for_the_first_account() = runTest {
        cutRestore(backgroundScope)
        val waits = b.scheduler.photoWaits.size
        switchTo(second)
        val secondDrive = RequestCountingDrive(FakeDrive(fs))
        val controller = b.controller(backgroundScope, photos = secondDrive)

        assertFalse(controller.resumePhotos())

        assertEquals(0, secondDrive.requests, "다른 계정 드라이브에 사진을 찾았다")
        assertEquals(0, b.store.current().photosUnreachable, "백업에 없는 사진으로 셌다")
        assertEquals(2, pending())
        assertEquals(waitingForFirst, controller.waiting())
        assertEquals(PhotoStop.OTHER_ACCOUNT, b.store.current().photoStop)
        assertEquals(waits, b.scheduler.photoWaits.size, "받을 수 없는 계정에서 네트워크를 기다렸다")
    }

    @Test
    fun the_backup_screen_says_so_right_after_switching() = runTest {
        cutRestore(backgroundScope)

        switchTo(second)

        assertEquals(waitingForFirst, b.controller(backgroundScope).waiting())
    }

    @Test
    fun disconnecting_then_connecting_another_account_keeps_the_photos_for_the_first_account() = runTest {
        cutRestore(backgroundScope)

        backup().disconnect()
        assertEquals(waitingForFirst, b.controller(backgroundScope).waiting(), "연결이 없을 때 파일을 고르라고 했다")

        b.auth.account = second
        backup().connect()
        val secondDrive = RequestCountingDrive(FakeDrive(fs))
        val controller = b.controller(backgroundScope, photos = secondDrive)
        assertFalse(controller.resumePhotos())

        assertEquals(0, secondDrive.requests)
        assertEquals(2, pending())
        assertEquals(waitingForFirst, controller.waiting())
    }

    @Test
    fun reconnecting_with_another_account_keeps_the_photos_for_the_first_account() = runTest {
        cutRestore(backgroundScope)
        b.store.update { it.copy(lastFailure = BackupFailure.AUTH) }
        b.auth.account = second

        backup().connect(hint = first.email)
        val secondDrive = RequestCountingDrive(FakeDrive(fs))
        val controller = b.controller(backgroundScope, photos = secondDrive)
        assertFalse(controller.resumePhotos())

        assertEquals(0, secondDrive.requests)
        assertEquals(waitingForFirst, controller.waiting())
    }

    @Test
    fun going_back_to_the_first_account_receives_the_rest_and_forgets_it() = runTest {
        cutRestore(backgroundScope)
        switchTo(second)
        b.controller(backgroundScope, photos = RequestCountingDrive(FakeDrive(fs))).resumePhotos()

        switchTo(first)
        val controller = b.controller(backgroundScope)
        // 다른 계정이라 멈췄던 것은 이제 끊긴 것. 지금 받기나 앱을 다시 열면 이음
        assertEquals(WaitingPhotos(2, PhotoStop.INTERRUPTED), controller.waiting())
        assertFalse(controller.resumePhotos())

        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
        assertNull(controller.waiting())
        val prefs = b.store.current()
        assertNull(prefs.photoStop)
        assertNull(prefs.photoAccount)
        assertNull(prefs.photoAccountLabel)
    }

    /** 같은 계정은 전과 같이 목록 한 번과 사진마다 받기 한 번 */
    @Test
    fun the_same_account_receives_with_the_same_requests_as_before() = runTest {
        cutRestore(backgroundScope)
        val counting = RequestCountingDrive(firstDrive)

        assertFalse(b.controller(backgroundScope, photos = counting).resumePhotos())

        assertEquals(1, counting.lists)
        assertEquals(2, counting.downloads)
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
        assertNull(b.store.current().photoAccount)
    }

    /** 이 값이 생기기 전에 복원한 사람은 지금 계정에서 받음 */
    @Test
    fun someone_who_restored_before_this_update_receives_from_the_connected_account() = runTest {
        cutRestore(backgroundScope)
        b.store.update { it.copy(photoAccount = null, photoAccountLabel = null) }
        val controller = b.controller(backgroundScope)

        assertEquals(WaitingPhotos(2, PhotoStop.NETWORK), controller.waiting())
        assertFalse(controller.resumePhotos())

        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    @Test
    fun a_new_restore_from_the_second_account_remembers_that_account() = runTest {
        cutRestore(backgroundScope)
        switchTo(second)
        val secondDrive = FakeDrive(fs)
        val c = TestPhone(dir / "c", secondDrive, name = "C")
        try {
            c.write("three")
            c.photo("three", 0, "셋째 사진")
            assertIs<BackupOutcome.Done>(c.runner().run())
            val controller = b.controller(backgroundScope, photos = secondDrive, api = secondDrive)

            controller.prepare(controller.backups().entriesOrNull!!.first().snapshotName)
            controller.merge()
            assertIs<RestoreState.Done>(controller.finished())

            assertEquals("sub-2", b.store.current().photoAccount)
            assertEquals("second@gmail.com", b.store.current().photoAccountLabel)
        } finally {
            c.close()
        }
    }

    @Test
    fun a_file_restore_forgets_the_account() = runTest {
        cutRestore(backgroundScope)
        val zip = dir / "export.zip"
        a.exportTo(zip)
        val cut = b.controller(backgroundScope, files = JammedFiles(fs) { it.name.endsWith(".part") })

        assertIs<RestorePreparation.Ready>(cut.prepareFile(fs.source(zip)))
        cut.merge()
        assertIs<RestoreState.Done>(cut.finished())

        assertNull(b.store.current().photoAccount)
        assertNull(b.store.current().photoAccountLabel)
    }
}

/** 드라이브에 보낸 요청 수 */
private class RequestCountingDrive(private val real: DriveApi) : DriveApi by real {
    var lists = 0
        private set
    var downloads = 0
        private set
    val requests: Int
        get() = lists + downloads

    override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage {
        lists++
        return real.list(query, pageToken)
    }

    override suspend fun get(fileId: String): DriveFile {
        lists++
        return real.get(fileId)
    }

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        downloads++
        return real.download(fileId, to, fromByte)
    }
}
