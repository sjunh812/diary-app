package org.sjhstudio.diary.core.data.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

// 목록 감시가 실패해도 다시 붙고, 그사이 바뀐 값을 놓치지 않는지
class WatchRejoinTest {
    @Test
    fun the_first_value_is_the_baseline_and_only_changes_come_out() = runTest {
        val changes = flow { listOf(1, 1, 2, 2, 3).forEach { emit(it) } }.changesSinceFirst().toList()

        assertEquals(listOf(2, 3), changes)
    }

    /** 첫 값 전에 실패하면 기준이 없어, 다시 붙은 뒤 이미 바뀐 값도 냄 */
    @Test
    fun a_failure_before_the_first_value_keeps_the_next_value() = runTest {
        var calls = 0
        val source =
            flow {
                calls++
                if (calls == 1) error("첫 읽기 실패")
                emit(2)
            }

        assertEquals(listOf(2), source.changesSinceFirst().take(1).toList())
    }

    /** 첫 값 뒤에 실패해 다시 붙으면 같은 값은 거르고 바뀐 값만 냄 */
    @Test
    fun rejoining_after_the_baseline_skips_the_same_value() = runTest {
        var calls = 0
        val source =
            flow {
                calls++
                emit(1)
                if (calls == 1) error("중간에 실패")
                emit(2)
            }

        assertEquals(listOf(2), source.changesSinceFirst().take(1).toList())
    }

    /** 기준을 잡은 뒤 돌아오고, 기준 값은 바뀐 것으로 보지 않음 */
    @Test
    fun the_list_starts_after_the_baseline_and_reloads_only_on_change() = runTest {
        val source = MutableStateFlow(1)
        var reloads = 0

        backgroundScope.watchFromBaseline({ source.changesSinceFirst(it) }) { reloads++ }.await()
        testScheduler.runCurrent()
        assertEquals(0, reloads)

        source.value = 2
        testScheduler.runCurrent()
        assertEquals(1, reloads)
    }

    /** 첫 읽기가 실패해도 목록은 바로 시작하고, 다시 붙어 받은 값으로 다시 읽음 */
    @Test
    fun a_failed_first_read_starts_the_list_and_reloads_after_rejoining() = runTest {
        var calls = 0
        val source =
            flow {
                calls++
                if (calls == 1) error("첫 읽기 실패")
                emit(2)
                awaitCancellation()
            }
        var reloads = 0

        backgroundScope.watchFromBaseline({ source.changesSinceFirst(it) }) { reloads++ }.await()
        assertEquals(0, reloads)

        // 다시 붙기까지 기다림. 감시는 배경에서 돌아 시간을 직접 넘김
        advanceTimeBy(6.seconds)
        runCurrent()
        assertEquals(1, reloads)
    }

    /** 첫 값 전의 실패는 화면에 알리고, 첫 값 뒤의 실패는 조용히 다시 붙음 */
    @Test
    fun a_failure_before_the_first_value_reaches_the_screen() = runTest {
        val failed = runCatching { flow<Int> { error("읽지 못함") }.rejoiningAfterFirst().toList() }
        assertEquals(true, failed.isFailure)
    }

    @Test
    fun a_failure_after_the_first_value_rejoins_quietly() = runTest {
        var calls = 0
        val source =
            flow {
                calls++
                emit(calls)
                if (calls == 1) error("중간에 실패")
            }

        assertEquals(listOf(1, 2), source.rejoiningAfterFirst().take(2).toList())
    }
}
