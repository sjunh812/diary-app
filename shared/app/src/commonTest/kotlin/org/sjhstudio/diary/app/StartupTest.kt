package org.sjhstudio.diary.app

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class StartupTest {
    @Test
    fun reads_settings_and_lock_then_becomes_ready() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        settings.update { it.copy(themeMode = ThemeMode.DARK) }
        lock.update { it.copy(enabled = true) }
        var prepared = false
        val startup = Startup({ prepared = true }, settings, lock, this)

        assertFalse(startup.ready)
        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
        assertTrue(prepared)
        val ready = startup.state.value as StartupState.Ready
        assertEquals(ThemeMode.DARK, ready.settings.themeMode)
        assertTrue(ready.lock.enabled)
    }
}
