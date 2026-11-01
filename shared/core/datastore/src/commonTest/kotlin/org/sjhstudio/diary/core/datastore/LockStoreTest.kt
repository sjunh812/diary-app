package org.sjhstudio.diary.core.datastore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter

class LockStoreTest {
    @Test
    fun empty_file_reads_as_unlocked_defaults() = runTest {
        TestDataStores().use { stores ->
            assertEquals(LockSettings(), LockStore(stores.factory.lock()).lock.first())
        }
    }

    @Test
    fun update_round_trips() = runTest {
        TestDataStores().use { stores ->
            val store = LockStore(stores.factory.lock())
            val changed =
                LockSettings(
                    enabled = true,
                    pinMac = "bWFj",
                    pinSalt = "c2FsdA==",
                    biometricEnabled = true,
                    relockAfter = RelockAfter.FIVE_MINUTES,
                    failedAttempts = 5,
                    retryAfter = Instant.fromEpochMilliseconds(1_762_000_000_000)
                )

            store.update { changed }
            assertEquals(changed, store.lock.first())

            store.update { it.copy(failedAttempts = 0, retryAfter = null) }
            val cleared = store.lock.first()
            assertEquals(0, cleared.failedAttempts)
            assertNull(cleared.retryAfter)
            assertTrue(cleared.hasPin)
        }
    }

    @Test
    fun broken_file_stays_locked_without_a_key() = runTest {
        TestDataStores().use { stores ->
            FileSystem.SYSTEM.write(stores.directory / DataStoreFactory.LOCK_FILE) { writeUtf8("not a protobuf") }

            val lock = LockStore(stores.factory.lock()).lock.first()

            assertTrue(lock.enabled)
            assertFalse(lock.hasPin)
        }
    }
}
