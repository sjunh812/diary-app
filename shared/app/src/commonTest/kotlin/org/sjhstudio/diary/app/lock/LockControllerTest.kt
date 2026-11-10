package org.sjhstudio.diary.app.lock

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.ExpectedLeave
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.TestClock

// 다시 잠그기(스펙 001 T053, contracts/lock.md)
class LockControllerTest {
    private val clock = TestClock()
    private val keys = FakePinKeyStore()
    private val repository =
        DefaultLockRepository(LockStore(InMemoryDataStore<Preferences>(emptyPreferences())), keys, keyWork = Dispatchers.Unconfined)

    private suspend fun TestScope.started(relockAfter: RelockAfter): LockController {
        repository.setPin(checkNotNull(Pin.of("2580")))
        repository.setRelockAfter(relockAfter)
        val controller = LockController(repository, backgroundScope, clock)
        controller.onColdStart(repository.settings.first())
        testScheduler.runCurrent()
        assertEquals(LockState.Locked, controller.state.value)
        controller.onUnlocked()
        return controller
    }

    private fun LockController.awayFor(seconds: Int) {
        onBackground()
        clock.advance(seconds.seconds)
        onForeground()
    }

    @Test
    fun immediately_locks_on_every_return() = runTest {
        val controller = started(RelockAfter.IMMEDIATELY)
        controller.awayFor(0)
        assertEquals(LockState.Locked, controller.state.value)
    }

    @Test
    fun one_minute_and_five_minutes() = runTest {
        val oneMinute = started(RelockAfter.ONE_MINUTE)
        oneMinute.awayFor(59)
        assertEquals(LockState.Unlocked, oneMinute.state.value)
        oneMinute.awayFor(60)
        assertEquals(LockState.Locked, oneMinute.state.value)

        val fiveMinutes = started(RelockAfter.FIVE_MINUTES)
        fiveMinutes.awayFor(299)
        assertEquals(LockState.Unlocked, fiveMinutes.state.value)
        fiveMinutes.awayFor(300)
        assertEquals(LockState.Locked, fiveMinutes.state.value)
    }

    /** 앱을 새로 열 때만: 오래 떠나 있다 와도 프로세스가 살아 있으면 잠그지 않음 */
    @Test
    fun cold_start_only_locks_on_a_new_process() = runTest {
        val controller = started(RelockAfter.COLD_START)
        controller.awayFor(3600)
        assertEquals(LockState.Unlocked, controller.state.value)

        val newProcess = LockController(repository, backgroundScope, clock)
        newProcess.onColdStart(repository.settings.first())
        assertEquals(LockState.Locked, newProcess.state.value)
    }

    /** 기기 시각을 앞뒤로 바꿔도 단조 시계로 잼 */
    @Test
    fun wall_clock_changes_do_not_matter() = runTest {
        val controller = started(RelockAfter.FIVE_MINUTES)
        controller.onBackground()
        clock.setWallClock(Instant.parse("2020-01-01T00:00:00Z"))
        controller.onForeground()
        assertEquals(LockState.Unlocked, controller.state.value)

        controller.onBackground()
        clock.setWallClock(Instant.parse("2030-01-01T00:00:00Z"))
        controller.onForeground()
        assertEquals(LockState.Unlocked, controller.state.value)
    }

    /** 폰 화면 잠금을 확인하러 다녀온 동안에는 다시 잠그지 않음 */
    @Test
    fun device_credential_round_trip_does_not_relock() = runTest {
        val controller = started(RelockAfter.IMMEDIATELY)
        controller.withoutRelock { controller.awayFor(10) }
        assertEquals(LockState.Unlocked, controller.state.value)
    }

    /** 바로 잠금을 켜도 사진을 고르고 돌아오면 쓰던 화면 그대로. 잠기면 고른 사진을 받을 곳이 없어짐 */
    @Test
    fun coming_back_from_the_photo_picker_keeps_the_screen() = runTest {
        val controller = started(RelockAfter.IMMEDIATELY)
        ExpectedLeave.mark()
        controller.awayFor(30)
        assertEquals(LockState.Unlocked, controller.state.value)
        assertFalse(ExpectedLeave.marked)
    }

    /** 사진 고르기를 띄운 채 자리를 오래 비우면 평소대로 잠김 */
    @Test
    fun a_long_stay_in_the_picker_still_locks() = runTest {
        val controller = started(RelockAfter.IMMEDIATELY)
        ExpectedLeave.mark()
        controller.awayFor(600)
        assertEquals(LockState.Locked, controller.state.value)
        assertFalse(ExpectedLeave.marked)
    }

    /** 표시는 한 번만 씀. 다음에 나갔다 오면 평소대로 잠김 */
    @Test
    fun the_picker_mark_is_used_once() = runTest {
        val controller = started(RelockAfter.IMMEDIATELY)
        ExpectedLeave.mark()
        controller.awayFor(10)
        controller.awayFor(10)
        assertEquals(LockState.Locked, controller.state.value)
    }

    @Test
    fun lock_off_never_locks() = runTest {
        val controller = LockController(repository, backgroundScope, clock)
        controller.onColdStart(LockSettings())
        controller.awayFor(600)
        assertEquals(LockState.Unlocked, controller.state.value)
    }

    /** 확인값이 없거나 기기 키를 잃었으면 숫자판 대신 새 비밀번호 정하기로 감 */
    @Test
    fun missing_pin_or_key_needs_a_new_pin() = runTest {
        val withoutPin = LockController(repository, backgroundScope, clock)
        withoutPin.onColdStart(LockSettings(enabled = true))
        assertEquals(LockState.NeedsNewPin, withoutPin.state.value)

        repository.setPin(checkNotNull(Pin.of("2580")))
        keys.lose(LockSettings.DEFAULT_KEY_ALIAS)
        val keyLost = LockController(repository, backgroundScope, clock)
        keyLost.onColdStart(repository.settings.first())
        testScheduler.runCurrent()
        assertEquals(LockState.NeedsNewPin, keyLost.state.value)
    }

    /** 화면을 다시 그릴 때마다 불려도 듣는 곳은 하나. 두 곳에서 들으면 돌아온 시각을 두 번 잼 */
    @Test
    fun watching_starts_once_however_often_the_gate_appears() = runTest {
        var watchers = 0
        val controller = LockController(repository, backgroundScope, clock) { _, _ -> watchers++ }

        repeat(3) { controller.watchForeground() }

        assertEquals(1, watchers)
    }

    /** Android 11까지는 맨 앞 화면에서 뒤로 가면 화면이 사라지고 나서 앱이 뒤로 갔다고 알림. 그래도 다시 잠겨야 함 */
    @Test
    fun leaving_is_heard_through_what_was_watched() = runTest {
        var away: () -> Unit = {}
        var back: () -> Unit = {}
        repository.setPin(checkNotNull(Pin.of("2580")))
        repository.setRelockAfter(RelockAfter.ONE_MINUTE)
        val controller =
            LockController(repository, backgroundScope, clock) { onBackground, onForeground ->
                away = onBackground
                back = onForeground
            }
        controller.onColdStart(repository.settings.first())
        testScheduler.runCurrent()
        controller.onUnlocked()
        controller.watchForeground()

        away()
        clock.advance(1.minutes)
        back()

        assertEquals(LockState.Locked, controller.state.value)
    }

    @Test
    fun relock_waits_for_the_chosen_time_even_after_ten_minutes_of_use() = runTest {
        val controller = started(RelockAfter.ONE_MINUTE)
        clock.advance(10.minutes)
        controller.awayFor(30)
        assertEquals(LockState.Unlocked, controller.state.value)
    }

    /** 복구 길이 막혀도 앱을 열지 않고 잠금 화면으로 돌아감 */
    @Test
    fun keeping_locked_never_unlocks() = runTest {
        repository.setPin(checkNotNull(Pin.of("2580")))
        val controller = LockController(repository, backgroundScope, clock)
        controller.onColdStart(repository.settings.first())
        testScheduler.runCurrent()
        controller.onKeyMissing()
        assertEquals(LockState.NeedsNewPin, controller.state.value)

        controller.onKeepLocked()

        assertEquals(LockState.Locked, controller.state.value)
    }
}
