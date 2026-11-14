package org.sjhstudio.diary.app.ads

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// 광고 없애기 시트 차례와 보상 광고 결과
@OptIn(ExperimentalCoroutinesApi::class)
class AdRemovalViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun the_close_button_opens_the_sheet_and_the_day_off_choice_follows() {
        val viewModel = AdRemovalViewModel { true }

        viewModel.open()
        assertEquals(AdRemovalSheet.Remove, viewModel.state.value.sheet)

        viewModel.offerReward()
        assertEquals(AdRemovalSheet.Reward, viewModel.state.value.sheet)

        viewModel.dismiss()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    @Test
    fun the_sheet_waits_while_the_video_plays_and_closes_when_earned() = runTest(dispatcher) {
        val result = CompletableDeferred<Boolean>()
        val viewModel = AdRemovalViewModel { result.await() }
        viewModel.open()
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()
        assertEquals(AdRemovalState(AdRemovalSheet.Reward, watching = true), viewModel.state.value)

        result.complete(true)
        advanceUntilIdle()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    @Test
    fun an_unearned_reward_says_so() = runTest(dispatcher) {
        val viewModel = AdRemovalViewModel { false }
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()

        assertEquals(AdRemovalState(AdRemovalSheet.RewardFailed), viewModel.state.value)
    }

    /** 광고 쪽이 예외를 던져도 진행 표시가 남지 않음 */
    @Test
    fun a_failing_reward_ends_as_not_earned() = runTest(dispatcher) {
        val viewModel = AdRemovalViewModel { error("광고를 받지 못함") }
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()

        assertEquals(AdRemovalState(AdRemovalSheet.RewardFailed), viewModel.state.value)
    }

    @Test
    fun a_second_tap_while_watching_does_not_start_another_video() = runTest(dispatcher) {
        var started = 0
        val result = CompletableDeferred<Boolean>()
        val viewModel = AdRemovalViewModel {
            started++
            result.await()
        }

        viewModel.watch()
        viewModel.watch()
        advanceUntilIdle()

        assertEquals(1, started)
    }
}
