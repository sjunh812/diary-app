package org.sjhstudio.diary.core.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.model.AccountChange
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestorePreview
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

    private val gate = BackupGate()

    private fun controller(found: BackupEntry? = null, find: suspend () -> BackupEntry? = { found }, backupStore: BackupStore = store) =
        BackupController(
            backupStore,
            lazy { error("쓰지 않는다") },
            lazy { error("쓰지 않는다") },
            authorizer,
            scheduler,
            FakeBackupNotifier(),
            CoroutineScope(Dispatchers.Unconfined),
            findBackup = find,
            running = { running },
            gate = gate
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

    /** 안내를 고르지 않고 앱을 다시 열어도 이 기기 일기가 먼저 올라가지 않음 */
    @Test
    fun a_found_backup_still_waits_after_reopening_the_app() = runTest {
        controller(found = entry).connect()

        val reopened = controller(found = entry)
        reopened.backUpIfDue()

        assertEquals(0, scheduler.startedNow)
        assertEquals(entry, reopened.foundBackup.value)
    }

    /** 드라이브를 못 읽으면 없음으로 보지 않음. 다른 기기 백업을 묻기 전에 올리지 않게(US2 시나리오 7) */
    @Test
    fun a_drive_that_cannot_be_read_does_not_start_a_backup() = runTest {
        val controller = controller(find = { throw DriveException(BackupFailure.OFFLINE) })

        controller.connect()

        assertEquals(0, scheduler.startedNow)
        assertTrue(store.current().foundBackupWaiting)
        // 멈춘 까닭을 화면에 둬 지금 백업으로 다시 찾게 함
        assertEquals(BackupFailure.OFFLINE, store.current().lastFailure)
    }

    /** 다시 찾기에 성공하면 찾기가 남긴 끊김 안내를 지움. 권유가 떠도 화면이 끊겼다고 하지 않게 */
    @Test
    fun a_search_that_succeeds_clears_its_offline_notice() = runTest {
        var reads = 0
        val controller = controller(find = { if (reads++ == 0) throw DriveException(BackupFailure.OFFLINE) else entry })
        controller.connect()

        controller.backUpNow()

        assertEquals(entry, controller.foundBackup.value)
        assertNull(store.current().lastFailure)
        assertNull(store.current().failingSince)
    }

    /** 찾지 못했던 드라이브는 지금 백업을 누르면 다시 찾아 권함. 눌러도 아무 일이 없지 않게 */
    @Test
    fun backing_up_after_a_failed_search_searches_again() = runTest {
        var reads = 0
        val controller = controller(find = { if (reads++ == 0) error("오프라인") else entry })
        controller.connect()

        controller.backUpNow()

        assertEquals(entry, controller.foundBackup.value)
        assertEquals(0, scheduler.startedNow)
    }

    /** 새 찾기는 앞 찾기를 멈춤. 앞 찾기는 바꾸기 전 계정일 수 있어 그 결과가 남지 않게 */
    @Test
    fun a_new_search_stops_the_one_before() = runTest {
        backedUp()
        store.update { it.copy(foundBackupWaiting = true) }
        val stuck = CompletableDeferred<BackupEntry?>()
        var reads = 0
        val controller = controller(find = { if (reads++ == 0) stuck.await() else null })
        launch(UnconfinedTestDispatcher(testScheduler)) { controller.backUpIfDue() }

        authorizer.account = DriveAccount("sub-2", "work@gmail.com")
        controller.connect()
        stuck.complete(entry)

        assertNull(controller.foundBackup.value)
        assertFalse(store.current().foundBackupWaiting)
        assertEquals(1, scheduler.startedNow)
    }

    /** 드라이브를 찾는 도중 연결을 끊으면 찾기도 멈춤. 끊은 계정의 권유가 뒤늦게 뜨지 않게 */
    @Test
    fun disconnecting_stops_the_search() = runTest {
        val searching = CompletableDeferred<BackupEntry?>()
        val controller = controller(find = { searching.await() })
        launch(UnconfinedTestDispatcher(testScheduler)) { controller.connect() }

        controller.disconnect()
        searching.complete(entry)

        assertNull(controller.foundBackup.value)
        assertFalse(store.current().foundBackupWaiting)
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

    /** 끊긴 연결을 다시 이어도 사용자가 꺼 둔 자동 백업을 말없이 켜지 않음 */
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

    /** 끊은 뒤 다른 계정을 연결해도 옛 계정의 백업 시각이 보이지 않음 */
    @Test
    fun disconnecting_forgets_the_last_backup() = runTest {
        backedUp()

        controller().disconnect()

        assertNoBackupRecord(store.current())
    }

    /** 다시 연결하며 다른 계정을 골라도 같음 */
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

    /** 백업 설정에서 다른 계정을 고르면 처음 연결처럼 이어짐(스펙 011 US2) */
    @Test
    fun changing_to_another_account_starts_over_on_that_drive() = runTest {
        backedUp()
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")

        assertEquals(AccountChange.CHANGED, controller().changeAccount())

        assertEquals("sub-2", store.current().accountKey)
        assertEquals("work@gmail.com", store.current().accountLabel)
        assertNoBackupRecord(store.current())
        assertTrue(store.current().autoEnabled)
        assertEquals(1, scheduler.startedNow)
    }

    @Test
    fun changing_to_an_account_with_a_backup_asks_before_uploading() = runTest {
        backedUp()
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")
        val controller = controller(found = entry)

        controller.changeAccount()

        assertEquals(entry, controller.foundBackup.value)
        assertTrue(store.current().foundBackupWaiting)
        assertEquals(0, scheduler.startedNow)
    }

    /** 꺼 둔 자동 백업은 계정을 바꿔도 꺼진 채 */
    @Test
    fun changing_accounts_keeps_auto_backup_off() = runTest {
        backedUp()
        store.update { it.copy(autoEnabled = false) }
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")

        controller().changeAccount()

        assertFalse(store.current().autoEnabled)
        assertFalse(scheduler.daily)
    }

    @Test
    fun picking_the_same_account_changes_nothing() = runTest {
        backedUp()

        assertEquals(AccountChange.KEPT, controller().changeAccount())

        assertEquals(entry.createdAt, store.current().lastSuccessAt)
        assertEquals("folder", store.current().rootFolderId)
        assertEquals(0, scheduler.startedNow)
    }

    @Test
    fun cancelling_a_change_keeps_the_old_account() = runTest {
        backedUp()
        authorizer.connectResult = ConnectResult.Cancelled
        val controller = controller()

        assertEquals(AccountChange.KEPT, controller.changeAccount())

        assertEquals("sub-1", store.current().accountKey)
        assertEquals(entry.createdAt, store.current().lastSuccessAt)
        assertNull(controller.connectFailure.value)
        assertNull(store.current().lastFailure)
    }

    @Test
    fun a_failed_change_keeps_the_old_account() = runTest {
        backedUp()
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.OFFLINE)
        val controller = controller()

        controller.changeAccount()

        assertEquals("sub-1", store.current().accountKey)
        assertEquals(BackupFailure.OFFLINE, controller.connectFailure.value)
        assertNull(store.current().lastFailure)
    }

    /** iOS: 드라이브 권한을 뺀 새 계정이 앞 계정 로그인을 덮으면 연결된 척하지 않음 */
    @Test
    fun a_change_that_lost_the_old_sign_in_asks_to_reconnect() = runTest {
        backedUp()
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.NO_DRIVE_ACCESS)
        authorizer.account = null
        val controller = controller()

        controller.changeAccount()

        assertEquals("sub-1", store.current().accountKey)
        assertEquals(BackupFailure.AUTH, store.current().lastFailure)
    }

    /** 백업·복원이 잠금을 쥐고 있으면 계정 선택 창을 띄우지 않음 */
    @Test
    fun a_running_backup_blocks_the_change() = runTest {
        backedUp()
        assertTrue(gate.tryEnter())

        assertEquals(AccountChange.BUSY, controller().changeAccount())

        assertEquals(0, authorizer.connects)
        assertEquals("sub-1", store.current().accountKey)
        gate.leave()
    }

    /** 계정을 고르는 동안 깬 백업은 기다림. 앞 계정 폴더에 새 계정으로 올리지 않음 */
    @Test
    fun a_backup_waits_while_the_account_changes() = runTest {
        backedUp()
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")
        var backupGotIn = true
        authorizer.whilePicking = {
            backupGotIn = gate.tryEnter()
            if (backupGotIn) gate.leave()
        }

        controller().changeAccount()

        assertFalse(backupGotIn)
        assertTrue(gate.tryEnter())
        gate.leave()
    }

    /** 새 계정 드라이브에서 백업을 찾는 동안 또 바꾸면 앞 계정의 결과가 새 계정에 섞임 */
    @Test
    fun a_change_waits_while_the_new_drive_is_searched() = runTest {
        backedUp()
        val searching = CompletableDeferred<BackupEntry?>()
        val controller = controller(find = { searching.await() })
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")
        assertEquals(AccountChange.CHANGED, controller.changeAccount())

        authorizer.account = DriveAccount("sub-3", "home@gmail.com")
        assertEquals(AccountChange.BUSY, controller.changeAccount())

        assertEquals(1, authorizer.connects)
        assertEquals("sub-2", store.current().accountKey)
        searching.complete(null)
    }

    /** 찾은 백업에 답하기 전에는 계정을 바꾸지 않음. 답하지 않은 권유가 다른 계정에 남지 않게 */
    @Test
    fun a_change_waits_for_the_found_backup_answer() = runTest {
        backedUp()
        val controller = controller(found = entry)
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")
        controller.changeAccount()

        authorizer.account = DriveAccount("sub-3", "home@gmail.com")
        assertEquals(AccountChange.BUSY, controller.changeAccount())
        assertEquals("sub-2", store.current().accountKey)
    }

    /**
     * 오프라인이라 찾기가 끝나지 못해 남은 '찾은 백업 기다림'은 계정을 막지 않음
     * 막으면 아무 일도 돌지 않는데 "끝나면 바꿀 수 있어요"만 뜸. 새 계정은 바꾼 뒤 그 드라이브를 다시 찾음
     */
    @Test
    fun an_unfinished_search_left_from_before_does_not_block_the_change() = runTest {
        backedUp()
        store.update { it.copy(foundBackupWaiting = true) }
        authorizer.account = DriveAccount("sub-2", "work@gmail.com")

        assertEquals(AccountChange.CHANGED, controller().changeAccount())

        assertEquals("sub-2", store.current().accountKey)
        assertEquals(1, scheduler.startedNow)
    }

    /** 드라이브를 찾는 중에 백업 화면을 나가도 찾기와 첫 백업은 끝까지 감. 멈추면 '찾은 백업 기다림'만 남아 백업과 계정 바꾸기가 막힘 */
    @Test
    fun leaving_the_screen_while_finding_still_finishes() = runTest {
        val searching = CompletableDeferred<BackupEntry?>()
        val controller = controller(find = { searching.await() })

        val screen = launch(UnconfinedTestDispatcher(testScheduler)) { controller.connect() }
        screen.cancel()
        searching.complete(null)

        assertFalse(store.current().foundBackupWaiting)
        assertEquals(1, scheduler.startedNow)
    }

    /** 설정을 못 읽어도 잠금은 풀림. 안 풀리면 앱을 다시 켤 때까지 백업·복원이 멈춤 */
    @Test
    fun a_change_that_cannot_read_settings_releases_the_lock() = runTest {
        val broken = BackupStore(object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException("읽을 수 없음") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = error("쓰지 않음")
        })

        assertFails { controller(backupStore = broken).changeAccount() }

        assertTrue(gate.tryEnter())
        gate.leave()
    }

    /** 같은 계정을 다시 골라도 공간 부족 같은 드라이브 실패는 그대로. 다시 연결로 풀리는 실패만 지움 */
    @Test
    fun picking_the_same_account_keeps_a_drive_failure() = runTest {
        backedUp()
        val since = Instant.parse("2026-11-01T00:00:00Z")
        store.update { it.copy(lastFailure = BackupFailure.NO_SPACE, failingSince = since) }

        controller().changeAccount()

        assertEquals(BackupFailure.NO_SPACE, store.current().lastFailure)
        assertEquals(since, store.current().failingSince)
    }

    @Test
    fun picking_the_same_account_clears_a_sign_in_failure() = runTest {
        backedUp()
        store.update { it.copy(lastFailure = BackupFailure.AUTH, failingSince = Instant.parse("2026-11-01T00:00:00Z")) }

        controller().changeAccount()

        assertNull(store.current().lastFailure)
        assertNull(store.current().failingSince)
    }

    /** 재시도를 기다리는 동안 다시 눌러도 그 요청과 "이번만 모바일 데이터"가 시스템까지 감 */
    @Test
    fun every_press_reaches_the_system_with_its_own_choice() {
        val controller = controller()

        controller.backUpNow()
        controller.backUpNow(cellularOnce = true)

        assertEquals(2, scheduler.startedNow)
        assertTrue(scheduler.lastCellular)
    }

    /** 시스템 작업은 새 요청이 앞선 작업을 대신함. 도는 백업을 끊지 않게 넘기지 않음 */
    @Test
    fun a_running_backup_is_not_interrupted() {
        val controller = controller()
        running = true

        controller.backUpNow()

        assertEquals(0, scheduler.startedNow)
    }

    /** 까닭을 모르는 찾기 실패는 기록하지 않고 누른 사람에게 한 번 알림. 눌렀는데 반응이 없으면 안 됨 */
    @Test
    fun a_search_failing_for_an_unknown_reason_is_told_to_whoever_pressed() = runTest {
        val controller = controller(find = { error("로그인 페이지") })
        val told = mutableListOf<BackupSkip>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.skipped.toList(told) }
        controller.connect()

        controller.backUpNow()

        assertNull(store.current().lastFailure)
        assertEquals(listOf(BackupSkip.NOT_CHECKED, BackupSkip.NOT_CHECKED), told)
    }

    /** 앱을 열 때 다시 찾다 실패하면 누른 사람이 없어 알리지 않음 */
    @Test
    fun a_search_at_app_open_fails_quietly() = runTest {
        backedUp()
        store.update { it.copy(foundBackupWaiting = true) }
        val controller = controller(find = { error("로그인 페이지") })
        val told = mutableListOf<BackupSkip>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.skipped.toList(told) }

        controller.backUpIfDue()

        assertTrue(told.isEmpty())
    }

    /** 일기가 없거나 복원 중이면 올리지 않고 끝남. 눌렀는데 반응이 없으면 안 됨 */
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

    /** 옛 note.db는 편수를 몰라 0편. 그래도 새 휴대폰 사용자에게는 있다고 알려야 함 */
    @Test
    fun an_old_backup_is_offered_to_a_new_phone() {
        assertEquals(legacy, backupToOffer(listOf(legacy), migrated = false))
    }

    /** 이 기기에서 이미 옮겨 온 일기와 같은 백업이라 다시 권하지 않음 */
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

    /** 실패는 종류와 함께 화면까지 감 */
    @Test
    fun a_failed_connection_is_kept_for_the_screen() = runTest {
        authorizer.connectResult = ConnectResult.Failed(BackupFailure.OFFLINE)
        val controller = controller()

        controller.connect()

        assertEquals(BackupFailure.OFFLINE, controller.connectFailure.value)
        assertNull(store.current().accountKey)
    }

    /** 사용자가 물러난 것은 실패가 아님 */
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

    /** 공간이 꽉 차 연결을 적지 못하면 연결된 것이 아님. 꺼지지 않고 실패로 알림 */
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

    private fun preview(added: Int = 0, updated: Int = 0, keptBoth: Int = 0, photos: Int = 0) = RestorePreview(
        entriesAdded = added,
        entriesUpdated = updated,
        entriesKeptBoth = keptBoth,
        entriesUnchanged = 3,
        photosToDownload = photos,
        bytesToDownload = 0,
        photosNotInBackup = 0,
        settingsChange = true,
        deviceBytesFree = 0
    )

    /** 이 기기가 올린 백업처럼 합쳐도 달라질 것이 없으면 "백업이 있어요"를 띄우지 않음 */
    @Test
    fun a_backup_with_nothing_new_is_not_offered() = runTest {
        assertNull(offerIfNew(entry) { RestorePreparation.Ready(entry, preview()) })
    }

    @Test
    fun a_backup_with_new_diaries_or_photos_is_offered() = runTest {
        assertEquals(entry, offerIfNew(entry) { RestorePreparation.Ready(entry, preview(added = 1)) })
        assertEquals(entry, offerIfNew(entry) { RestorePreparation.Ready(entry, preview(updated = 1)) })
        assertEquals(entry, offerIfNew(entry) { RestorePreparation.Ready(entry, preview(keptBoth = 1)) })
        assertEquals(entry, offerIfNew(entry) { RestorePreparation.Ready(entry, preview(photos = 1)) })
    }

    /** 받아 보지 못한 백업은 숨기지 않고 권해 복원 화면에서 까닭을 보게 함 */
    @Test
    fun a_backup_that_could_not_be_checked_is_still_offered() = runTest {
        assertEquals(entry, offerIfNew(entry) { RestorePreparation.Failed(RestoreFailure.OTHER) })
        assertEquals(entry, offerIfNew(entry) { error("오프라인") })
    }

    /** 옛 백업은 받아 보지 않고 그대로 권함 */
    @Test
    fun an_old_backup_is_offered_without_checking() = runTest {
        assertEquals(legacy, offerIfNew(legacy) { error("받지 않는다") })
        assertNull(offerIfNew(null) { error("받지 않는다") })
    }
}
