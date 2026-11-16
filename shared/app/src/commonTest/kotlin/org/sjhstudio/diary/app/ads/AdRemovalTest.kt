package org.sjhstudio.diary.app.ads

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.data.RewardResult

// 광고 없애기 시트 차례와 보상 광고 결과
@OptIn(ExperimentalCoroutinesApi::class)
class AdRemovalTest {
    private val dispatcher = StandardTestDispatcher()

    // 시험 시계로 도는 앱 범위. 시험이 끝나도 기다리지 않음
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun the_close_button_opens_the_sheet_and_the_day_off_choice_follows() = runTest(dispatcher) {
        val viewModel = AdRemoval(scope) { RewardResult.Earned }

        viewModel.open()
        assertEquals(AdRemovalSheet.Remove, viewModel.state.value.sheet)

        viewModel.offerReward()
        assertEquals(AdRemovalSheet.Reward, viewModel.state.value.sheet)

        viewModel.dismiss()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    @Test
    fun the_sheet_waits_while_the_video_plays_and_closes_when_earned() = runTest(dispatcher) {
        val result = CompletableDeferred<RewardResult>()
        val viewModel = AdRemoval(scope) { result.await() }
        viewModel.open()
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()
        assertEquals(AdRemovalState(AdRemovalSheet.Reward, watching = true), viewModel.state.value)

        result.complete(RewardResult.Earned)
        advanceUntilIdle()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    @Test
    fun an_unearned_reward_says_so() = runTest(dispatcher) {
        val viewModel = AdRemoval(scope) { RewardResult.NotEarned }
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()

        assertEquals(AdRemovalState(AdRemovalSheet.RewardFailed), viewModel.state.value)
    }

    /** 영상을 받는 동안 시트를 닫으면 받기를 멈추고 시트도 다시 열리지 않음. 다른 화면 위에 광고나 실패 시트가 뜨지 않게 */
    @Test
    fun closing_the_sheet_while_loading_stops_the_video_and_stays_closed() = runTest(dispatcher) {
        val loading = CompletableDeferred<RewardResult>()
        var cancelled = false
        val viewModel =
            AdRemoval(scope) {
                try {
                    loading.await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cancelled = true
                    throw e
                }
            }
        viewModel.offerReward()
        viewModel.watch()
        advanceUntilIdle()

        viewModel.dismiss()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    /** 앱이 잠기면 시트를 닫고 받던 영상도 멈춤. 잠금 화면 위에 광고가 뜨지 않게 */
    @Test
    fun locking_closes_the_sheet_and_stops_loading() = runTest(dispatcher) {
        val locked = MutableStateFlow(false)
        val loading = CompletableDeferred<RewardResult>()
        val viewModel = AdRemoval(scope, locked) { loading.await() }
        viewModel.offerReward()
        viewModel.watch()
        advanceUntilIdle()

        locked.value = true
        advanceUntilIdle()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }

    /** 광고 쪽이 예외를 던져도 진행 표시가 남지 않음 */
    @Test
    fun a_failing_reward_ends_as_not_earned() = runTest(dispatcher) {
        val viewModel = AdRemoval(scope) { error("광고를 받지 못함") }
        viewModel.offerReward()

        viewModel.watch()
        advanceUntilIdle()

        assertEquals(AdRemovalState(AdRemovalSheet.RewardFailed), viewModel.state.value)
    }

    @Test
    fun a_second_tap_while_watching_does_not_start_another_video() = runTest(dispatcher) {
        var started = 0
        val result = CompletableDeferred<RewardResult>()
        val viewModel = AdRemoval(scope) {
            started++
            result.await()
        }

        viewModel.watch()
        viewModel.watch()
        advanceUntilIdle()

        assertEquals(1, started)
    }

    /** 이미 띄운 영상은 시트를 닫아도 끝까지 감. 그동안 보는 중으로 남아 앱 틀이 전면 광고를 미룸 */
    @Test
    fun closing_the_sheet_while_the_video_plays_keeps_watching_until_it_ends() = runTest(dispatcher) {
        val playing = CompletableDeferred<RewardResult>()
        val viewModel = AdRemoval(scope) { withContext(NonCancellable) { playing.await() } }
        viewModel.offerReward()
        viewModel.watch()
        advanceUntilIdle()

        viewModel.dismiss()
        advanceUntilIdle()
        assertEquals(AdRemovalState(watching = true), viewModel.state.value)

        playing.complete(RewardResult.Earned)
        advanceUntilIdle()
        assertEquals(AdRemovalState(), viewModel.state.value)
    }
}
