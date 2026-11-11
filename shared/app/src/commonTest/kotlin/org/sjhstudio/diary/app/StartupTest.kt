package org.sjhstudio.diary.app

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.DiaryFont
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

    /** 알림이나 업데이트로 앱이 깨면 앱과 수신기가 거의 함께 시작을 부름. 옛 설정 잇기와 뒤 일은 한 번만 */
    @Test
    fun starting_twice_runs_each_step_once() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        var prepared = 0
        var after = 0
        val startup = Startup({ prepared++ }, settings, lock, this, afterReady = { after++ })

        startup.start()
        startup.start()
        testScheduler.advanceUntilIdle()

        assertEquals(1, prepared)
        assertEquals(1, after)
    }

    /** 옛 잠금을 이은 뒤에 잠금을 읽어야 첫 화면이 잠금 화면 */
    @Test
    fun legacy_lock_is_bootstrapped_before_reading_lock() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup =
            Startup({
            }, settings, lock, this, bootstrapLegacyLock = {
                lock.update { it.copy(enabled = true, pinMac = "bWFj", pinSalt = "c2FsdA==") }
            })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue((startup.state.value as StartupState.Ready).lock.hasPin)
    }

    /** 잇기가 실패하면 잠근 채 새 비밀번호를 정하게 함. 앱이 열린 채로 시작하지 않음 */
    @Test
    fun failed_bootstrap_starts_locked_without_pin() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup = Startup({}, settings, lock, this, bootstrapLegacyLock = { error("pref.xml을 읽지 못함") })

        startup.start()
        testScheduler.advanceUntilIdle()

        val ready = startup.state.value as StartupState.Ready
        assertTrue(ready.lock.enabled)
        assertFalse(ready.lock.hasPin)
    }

    /** 설정 잇기가 실패해도 잠금을 건드리지 않고 기본 설정으로 엶 */
    @Test
    fun a_failed_storage_step_still_opens_the_app() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup = Startup({ error("폴더를 만들지 못함") }, settings, lock, this)

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
    }

    @Test
    fun failed_settings_bootstrap_keeps_lock_off() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup = Startup({}, settings, lock, this, bootstrapLegacySettings = { error("pref2.xml을 읽지 못함") })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertFalse((startup.state.value as StartupState.Ready).lock.enabled)
    }

    /** 고른 글꼴을 미리 읽는 동안에도 시작 화면은 기다리지 않음 */
    @Test
    fun font_warmup_runs_alongside_and_does_not_hold_the_splash() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        settings.update { it.copy(font = DiaryFont.NANUM_CHEER) }
        val warming = CompletableDeferred<Unit>()
        var warmed: DiaryFont? = null
        val startup =
            Startup({}, settings, lock, this, warmFont = {
                warmed = it.font
                warming.await()
            })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
        assertEquals(DiaryFont.NANUM_CHEER, warmed)
        warming.complete(Unit)
    }

    /** 미리 읽기가 실패해도 앱은 열림 */
    @Test
    fun a_failed_font_warmup_still_opens_the_app() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup = Startup({}, settings, lock, this, warmFont = { error("글꼴을 읽지 못함") })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
    }

    /** 결제 준비와 글꼴 확인은 첫 화면 전에 뒤에서 끝냄. 화면 스레드가 디스크를 읽지 않게 */
    @Test
    fun platform_work_and_font_check_finish_before_ready() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val done = mutableListOf<String>()
        val startup =
            Startup({}, settings, lock, this, platformWork = { done += "billing" }, knowFont = { done += "font" })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
        assertEquals(listOf("font", "billing"), done)
    }

    @Test
    fun failed_platform_work_still_opens_the_app() = runTest {
        val settings = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val lock = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        val startup = Startup({}, settings, lock, this, platformWork = { error("결제를 준비하지 못함") })

        startup.start()
        testScheduler.advanceUntilIdle()

        assertTrue(startup.ready)
    }
}
