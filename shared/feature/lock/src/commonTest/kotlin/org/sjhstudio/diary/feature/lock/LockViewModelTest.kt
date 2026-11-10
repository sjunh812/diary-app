package org.sjhstudio.diary.feature.lock

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.data.lock.VerifyResult
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 잠금 화면과 비밀번호 정하기의 입력 흐름(스펙 001 T061)
@OptIn(ExperimentalCoroutinesApi::class)
class LockViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val dataStore = InMemoryDataStore<Preferences>(emptyPreferences())
    private val keys = FakePinKeyStore()
    private val repository = DefaultLockRepository(LockStore(dataStore), keys)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 기다림이 시험 시계와 함께 흐르게 가상 시각을 벽시계로 씀. */
    private fun TestScope.clock() = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_793_500_000_000 + testScheduler.currentTime)
    }

    private fun LockViewModel.type(pin: String) = pin.forEach(::onDigit)

    @Test
    fun right_pin_unlocks_and_wrong_pin_shows_remaining() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        val vm = LockViewModel(repository, clock())
        val events = mutableListOf<LockEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

        vm.type("1111")
        advanceUntilIdle()
        // 틀리면 점은 바로 비고 흔들림 신호(wrongCount)가 늘어남
        assertEquals(LockUiState(message = LockMessage.Wrong(4), wrongCount = 1), vm.state.value)

        // 새로 누르면 안내 지움
        vm.onDigit('0')
        assertEquals(LockUiState(entered = 1, wrongCount = 1), vm.state.value)
        vm.type("427")
        advanceUntilIdle()
        assertEquals(listOf<LockEvent>(LockEvent.Unlocked), events)
        assertEquals(LockUiState(), vm.state.value)
    }

    /** 5번 틀리면 30초 동안 숫자를 받지 않고, 남은 초를 세다가 다시 받음. */
    @Test
    fun fifth_wrong_blocks_input_for_thirty_seconds() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        val vm = LockViewModel(repository, clock())
        repeat(5) {
            vm.type("1111")
            testScheduler.runCurrent()
        }
        assertEquals(LockMessage.Waiting(30), vm.state.value.message)

        vm.onDigit('0')
        assertEquals(0, vm.state.value.entered)

        testScheduler.advanceTimeBy(3_000)
        testScheduler.runCurrent()
        assertEquals(LockMessage.Waiting(27), vm.state.value.message)

        advanceUntilIdle()
        assertEquals(LockUiState(), vm.state.value)
    }

    @Test
    fun setup_needs_the_same_pin_twice() = runTest(dispatcher) {
        val vm = PinSetupViewModel(repository)
        var done = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.done.collect { done++ } }

        "1234".forEach(vm::onDigit)
        assertEquals(PinSetupUiState(again = true), vm.state.value)
        "1235".forEach(vm::onDigit)
        assertEquals(PinSetupUiState(mismatch = true, wrongCount = 1), vm.state.value)
        assertFalse(repository.settings.first().enabled)

        "2580".forEach(vm::onDigit)
        "2580".forEach(vm::onDigit)
        advanceUntilIdle()
        assertEquals(1, done)
        val lock = LockViewModel(repository, clock())
        lock.type("2580")
        advanceUntilIdle()
        assertEquals(LockUiState(), lock.state.value)
    }

    /** 두 번째 입력에서 뒤로 가면 첫 입력부터 다시 함. */
    @Test
    fun back_on_second_entry_restarts() = runTest(dispatcher) {
        val vm = PinSetupViewModel(repository)
        "1234".forEach(vm::onDigit)
        assertEquals(true, vm.back())
        assertEquals(PinSetupUiState(), vm.state.value)
        assertEquals(false, vm.back())
    }

    /** 기기 키를 못 만들면 꺼지지 않고 안내한 뒤 처음부터 다시 받음. 잠금은 켜지지 않음. */
    @Test
    fun a_pin_that_cannot_be_saved_says_so_and_starts_over() = runTest(dispatcher) {
        val vm = PinSetupViewModel(repository)
        var done = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.done.collect { done++ } }
        keys.failMaking = true

        "2580".forEach(vm::onDigit)
        "2580".forEach(vm::onDigit)
        advanceUntilIdle()

        assertEquals(0, done, "저장하지 못했으면 열면 안 된다")
        assertEquals(PinSetupUiState(saveFailed = true), vm.state.value)

        vm.onDigit('1')
        assertEquals(false, vm.state.value.saveFailed)
    }

    /** 공간이 꽉 차 비밀번호를 못 적어도 같음. */
    @Test
    fun a_full_disk_while_saving_the_pin_does_not_open_the_app() = runTest(dispatcher) {
        val vm = PinSetupViewModel(repository)
        var done = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.done.collect { done++ } }
        dataStore.failWrites = true

        "2580".forEach(vm::onDigit)
        "2580".forEach(vm::onDigit)
        advanceUntilIdle()

        assertEquals(0, done)
        assertEquals(true, vm.state.value.saveFailed)
    }

    /** 키를 쓸 수 없으면 열지 않고 복구 길로 보냄. */
    @Test
    fun a_keystore_crash_goes_to_recovery_and_never_unlocks() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        keys.failMac = true
        val vm = LockViewModel(repository, clock())
        val events = mutableListOf<LockEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

        vm.type("0427")
        advanceUntilIdle()

        assertEquals(listOf<LockEvent>(LockEvent.KeyMissing), events)
        assertEquals(LockMessage.KeyUnavailable, vm.state.value.message)
    }

    /** 저장소 구현이 예외를 던져도 숫자판이 멈추거나 꺼지지 않음. */
    @Test
    fun a_repository_crash_keeps_the_lock_screen_alive() = runTest(dispatcher) {
        val broken =
            object : LockRepository by repository {
                override suspend fun verify(pin: Pin, now: Instant): VerifyResult = error("읽을 수 없음")
            }
        val vm = LockViewModel(broken, clock())
        val events = mutableListOf<LockEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

        vm.type("0427")
        advanceUntilIdle()

        assertEquals(listOf<LockEvent>(LockEvent.KeyMissing), events)
        assertEquals(false, vm.state.value.checking)
    }

    @Test
    fun wrong_pin_on_a_full_disk_keeps_counting() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        dataStore.failWrites = true
        val vm = LockViewModel(repository, clock())

        vm.type("1111")
        advanceUntilIdle()

        assertEquals(LockMessage.Wrong(4), vm.state.value.message)
    }

    @Test
    fun turning_off_that_cannot_be_saved_says_so() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        dataStore.failWrites = true
        val vm = LockViewModel(repository, clock())
        val events = mutableListOf<LockEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }
        vm.start(PinCheck.Disable)

        vm.type("0427")
        advanceUntilIdle()

        assertEquals(emptyList<LockEvent>(), events)
        assertEquals(LockMessage.NotSaved, vm.state.value.message)
    }

    /** 끄지 못했는데 끈 것처럼 앱을 열면 안 됨. 그대로 잠근 채 알림. */
    @Test
    fun a_lock_that_cannot_be_turned_off_stays_locked_and_says_so() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        keys.failMac = true
        val vm = LockViewModel(repository, clock())

        assertEquals(false, vm.turnOffWithoutPin())
        assertEquals(LockMessage.KeyUnavailable, vm.state.value.message)
    }

    @Test
    fun a_full_disk_while_turning_off_does_not_crash() = runTest(dispatcher) {
        repository.setPin(checkNotNull(Pin.of("0427")))
        keys.lose(LockSettings.DEFAULT_KEY_ALIAS)
        dataStore.failWrites = true
        val vm = LockViewModel(repository, clock())

        assertEquals(false, vm.turnOffWithoutPin())
        assertEquals(LockMessage.KeyUnavailable, vm.state.value.message)
    }
}
