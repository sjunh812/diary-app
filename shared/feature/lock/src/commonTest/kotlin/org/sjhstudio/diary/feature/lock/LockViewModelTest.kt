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
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 잠금 화면과 비밀번호 정하기의 입력 흐름(스펙 001 T061).
@OptIn(ExperimentalCoroutinesApi::class)
class LockViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = DefaultLockRepository(LockStore(InMemoryDataStore<Preferences>(emptyPreferences())), FakePinKeyStore())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 기다림이 시험 시계와 함께 흐르게 가상 시각을 벽시계로 쓴다. */
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
        // 틀리면 점은 바로 비고, 흔들림 신호(wrongCount)가 는다.
        assertEquals(LockUiState(message = LockMessage.Wrong(4), wrongCount = 1), vm.state.value)

        // 새로 누르면 안내를 지운다.
        vm.onDigit('0')
        assertEquals(LockUiState(entered = 1, wrongCount = 1), vm.state.value)
        vm.type("427")
        advanceUntilIdle()
        assertEquals(listOf<LockEvent>(LockEvent.Unlocked), events)
        assertEquals(LockUiState(), vm.state.value)
    }

    /** 5번 틀리면 30초 동안 숫자를 받지 않고, 남은 초를 세다가 다시 받는다. */
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

    /** 두 번째 입력에서 뒤로 가면 첫 입력부터 다시 한다. */
    @Test
    fun back_on_second_entry_restarts() = runTest(dispatcher) {
        val vm = PinSetupViewModel(repository)
        "1234".forEach(vm::onDigit)
        assertEquals(true, vm.back())
        assertEquals(PinSetupUiState(), vm.state.value)
        assertEquals(false, vm.back())
    }
}
