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
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class BackupConnectTest {
    private val entry =
        BackupEntry("s", "d", "Pixel", "android", Instant.parse("2026-11-05T09:25:00Z"), 12, 3, 0, "2.0.0", BackupEntryState.Ready)
    private val scheduler = FakeBackupScheduler().apply { takesNow = true }
    private val store = BackupStore(InMemoryDataStore(emptyPreferences()))
    private val authorizer = FakeDriveAuthorizer()

    private fun controller(found: BackupEntry? = null) = BackupController(
        store,
        lazy { error("쓰지 않는다") },
        lazy { error("쓰지 않는다") },
        authorizer,
        scheduler,
        FakeBackupNotifier(),
        CoroutineScope(Dispatchers.Unconfined),
        findBackup = { found }
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
}
