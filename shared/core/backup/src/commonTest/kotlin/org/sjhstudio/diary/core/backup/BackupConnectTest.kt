package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class BackupConnectTest {
    private val entry =
        BackupEntry("s", "d", "Pixel", "android", Instant.parse("2026-11-05T09:25:00Z"), 12, 3, 0, "2.0.0", BackupEntryState.Ready)
    private val scheduler = FakeBackupScheduler().apply { takesNow = true }
    private val dataStore = InMemoryDataStore(emptyPreferences())
    private val store = BackupStore(dataStore)
    private val authorizer = FakeDriveAuthorizer()

    private var running = false

    private fun controller(found: BackupEntry? = null) = BackupController(
        store,
        lazy { error("쓰지 않는다") },
        lazy { error("쓰지 않는다") },
        authorizer,
        scheduler,
        FakeBackupNotifier(),
        CoroutineScope(Dispatchers.Unconfined),
        findBackup = { found },
        running = { running }
    )

    @Test
    fun no_backup_on_drive_starts_the_first_backup() = runTest {
        val controller = controller(found = null)

        controller.connect()

        assertEquals(1, scheduler.startedNow)
        assertNull(controller.foundBackup.value)
    }

    @Test
    fun a_backup_on_drive_waits_for_the_answer() = runTest {
        val controller = controller(found = entry)

        controller.connect()

        assertEquals(0, scheduler.startedNow)
        assertEquals(entry, controller.foundBackup.value)
    }

    /** 안내를 고르지 않고 앱을 다시 열어도 이 기기 일기가 먼저 올라가지 않는다. */
    @Test
    fun a_found_backup_still_waits_after_reopening_the_app() = runTest {
        controller(found = entry).connect()

        val reopened = controller(found = entry)
        reopened.backUpIfDue()

        assertEquals(0, scheduler.startedNow)
        assertEquals(entry, reopened.foundBackup.value)
    }

    @Test
    fun later_starts_this_phones_first_backup() = runTest {
        val controller = controller(found = entry)
        controller.connect()

        controller.skipFoundBackup()

        assertEquals(1, scheduler.startedNow)
        assertNull(controller.foundBackup.value)
    }

    @Test
    fun restoring_does_not_start_a_backup() = runTest {
        val controller = controller(found = entry)
        controller.connect()

        controller.takeFoundBackup()

        assertEquals(0, scheduler.startedNow)
        assertNull(controller.foundBackup.value)
    }

    @Test
    fun the_first_connection_turns_auto_backup_on() = runTest {
        controller().connect()

        assertTrue(store.current().autoEnabled)
        assertTrue(scheduler.daily)
    }

    /** 끊긴 연결을 다시 이어도 사용자가 꺼 둔 자동 백업을 말없이 켜지 않는다. */
    @Test
    fun reconnecting_keeps_auto_backup_off() = runTest {
        store.update { it.copy(accountKey = "sub-1", autoEnabled = false, lastFailure = BackupFailure.AUTH) }

        controller().connect()

        assertFalse(store.current().autoEnabled)
        assertFalse(scheduler.daily)
        assertNull(store.current().lastFailure)
    }

    @Test
    fun reconnecting_keeps_auto_backup_on() = runTest {
        store.update { it.copy(accountKey = "sub-1", autoEnabled = true, lastFailure = BackupFailure.AUTH) }

        controller().connect()

        assertTrue(store.current().autoEnabled)
        assertTrue(scheduler.daily)
    }

    private suspend fun backedUp(account: String = "sub-1") = store.update {
        it.copy(
            accountKey = account,
            accountLabel = "old@gmail.com",
            rootFolderId = "folder",
            autoEnabled = true,
            lastSuccessAt = entry.createdAt,
            lastSuccessSnapshot = "snapshot",
            lastSuccessFingerprint = "fingerprint",
            lastAttemptAt = entry.createdAt,
            photosNotBackedUp = 3,
            bytesShort = 10,
            restoredBackupAt = entry.createdAt
        )
    }

    private fun assertNoBackupRecord(prefs: BackupPrefs) {
        assertNull(prefs.lastSuccessAt)
        assertNull(prefs.lastSuccessSnapshot)
        assertNull(prefs.lastSuccessFingerprint)
        assertNull(prefs.lastAttemptAt)
        assertNull(prefs.restoredBackupAt)
        assertNull(prefs.rootFolderId)
        assertEquals(0, prefs.photosNotBackedUp)
        assertEquals(0, prefs.bytesShort)
    }

    /** 끊은 뒤 다른 계정을 연결해도 옛 계정의 백업 시각이 보이지 않는다. */
    @Test
    fun disconnecting_forgets_the_last_backup() = runTest {
        backedUp()

        controller().disconnect()

        assertNoBackupRecord(store.current())
    }

    /** 다시 연결하며 다른 계정을 골라도 같다. */
    @Test
    fun reconnecting_with_another_account_forgets_the_last_backup() = runTest {
        backedUp()
        authorizer.account = DriveAccount("sub-2", "new@gmail.com")

        controller().connect()

        assertNoBackupRecord(store.current())
        assertEquals("sub-2", store.current().accountKey)
    }

    @Test
    fun reconnecting_with_the_same_account_keeps_the_last_backup() = runTest {
        backedUp()

        controller().connect()

        assertEquals(entry.createdAt, store.current().lastSuccessAt)
        assertEquals("folder", store.current().rootFolderId)
    }

    /** 재시도를 기다리는 동안 다시 눌러도 그 요청과 "이번만 모바일 데이터"가 시스템까지 간다. */
    @Test
    fun every_press_reaches_the_system_with_its_own_choice() {
        val controller = controller()

        controller.backUpNow()
        controller.backUpNow(cellularOnce = true)

        assertEquals(2, scheduler.startedNow)
        assertTrue(scheduler.lastCellular)
    }

    /** 시스템 작업은 새 요청이 앞선 작업을 대신한다. 도는 백업을 끊지 않게 넘기지 않는다. */
    @Test
    fun a_running_backup_is_not_interrupted() {
        val controller = controller()
        running = true

        controller.backUpNow()

        assertEquals(0, scheduler.startedNow)
    }

    /** 일기가 없거나 복원 중이면 올리지 않고 끝난다. 눌렀는데 반응이 없으면 안 된다. */
    @Test
    fun a_press_that_uploads_nothing_is_told_once() = runTest {
        val controller = controller()
        val told = mutableListOf<BackupSkip>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.skipped.toList(told) }

        controller.nowFinished(BackupOutcome.Empty)
        controller.nowFinished(BackupOutcome.Busy)
        controller.nowFinished(BackupOutcome.Done(0, 0))
        controller.nowFinished(BackupOutcome.Failed(BackupFailure.OFFLINE))

        assertEquals(listOf(BackupSkip.NO_ENTRIES, BackupSkip.BUSY), told)
    }

    private val legacy = entry.copy(snapshotName = "legacy:1", platform = PLATFORM_LEGACY, entryCount = 0, photoCount = 0)

    /** 옛 note.db는 편수를 몰라 0편이다. 그래도 새 폰 사용자에게는 있다고 알려야 한다. */
    @Test
    fun an_old_backup_is_offered_to_a_new_phone() {
        assertEquals(legacy, backupToOffer(listOf(legacy), migrated = false))
    }

    /** 이 폰에서 이미 옮겨 온 일기와 같은 백업이라 다시 권하지 않는다. */
    @Test
    fun an_old_backup_is_not_offered_after_migrating_here() {
        assertNull(backupToOffer(listOf(legacy), migrated = true))
    }

    @Test
    fun a_new_backup_comes_before_an_old_one() {
        assertEquals(entry, backupToOffer(listOf(legacy, entry), migrated = false))
    }

    @Test
    fun empty_or_damaged_backups_are_not_offered() {
        val empty = entry.copy(entryCount = 0)
        val damaged = entry.copy(state = BackupEntryState.Damaged(DamageReason.MANIFEST))

        assertNull(backupToOffer(listOf(empty, damaged), migrated = false))
        assertNull(backupToOffer(null, migrated = false))
    }

    /** 실패는 종류와 함께 화면까지 간다. */
    @Test
    fun a_failed_connection_is_kept_for_the_screen() = runTest {
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.OFFLINE)
        val controller = controller()

        controller.connect()

        assertEquals(BackupFailure.OFFLINE, controller.connectFailure.value)
        assertNull(store.current().accountKey)
    }

    /** 사용자가 물러난 것은 실패가 아니다. */
    @Test
    fun cancelling_says_nothing() = runTest {
        authorizer.connectResult = ConnectResult.Cancelled
        val controller = controller()

        controller.connect()

        assertNull(controller.connectFailure.value)
    }

    @Test
    fun a_crash_while_connecting_counts_as_a_failure() = runTest {
        val controller = BackupController(
            store,
            lazy { error("쓰지 않는다") },
            lazy { error("쓰지 않는다") },
            object : DriveAuthorizer by authorizer {
                override suspend fun connect(hint: String?): ConnectResult = error("연결할 수 없다")
            },
            scheduler,
            FakeBackupNotifier(),
            CoroutineScope(Dispatchers.Unconfined),
            running = { false }
        )

        assertEquals(ConnectResult.Failed(BackupFailure.UNKNOWN), controller.connect())
        assertEquals(BackupFailure.UNKNOWN, controller.connectFailure.value)
    }

    /** 공간이 꽉 차 연결을 적지 못하면 연결된 것이 아니다. 꺼지지 않고 실패로 알린다. */
    @Test
    fun a_connection_that_cannot_be_saved_is_a_failure() = runTest {
        dataStore.failWrites = true
        val controller = controller()

        assertEquals(ConnectResult.Failed(BackupFailure.UNKNOWN), controller.connect())

        assertEquals(BackupFailure.UNKNOWN, controller.connectFailure.value)
        assertEquals(0, scheduler.startedNow)
    }

    @Test
    fun trying_again_clears_the_old_failure() = runTest {
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.AUTH)
        val controller = controller()
        controller.connect()

        authorizer.connectResult = null
        controller.connect()

        assertNull(controller.connectFailure.value)
    }

    @Test
    fun closing_the_notice_clears_the_failure() = runTest {
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.AUTH)
        val controller = controller()
        controller.connect()

        controller.connectFailureShown()

        assertNull(controller.connectFailure.value)
    }
}
