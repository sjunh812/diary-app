package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitedResultTest {
    @Test
    fun the_result_reaches_whoever_is_waiting() = runTest {
        val result = AwaitedResult<String>()
        var launched = 0

        val waiting = async { result.await { launched++ } }
        runCurrent()
        result.finish("동의함")

        assertEquals("동의함", waiting.await())
        assertEquals(1, launched)
    }

    /** 동의 창을 보는 동안 화면을 돌리면 결과는 새 화면으로 온다. 옛 화면의 코루틴이 기다리고 있어도 받아야 한다. */
    @Test
    fun a_result_from_a_rebuilt_screen_still_arrives() = runTest {
        val shared = AwaitedResult<String>()
        val beforeRotating = async { shared.await {} }
        runCurrent()

        val afterRotating: (String) -> Unit = shared::finish
        afterRotating("동의함")

        assertEquals("동의함", beforeRotating.await())
    }

    @Test
    fun a_second_wait_does_not_leave_the_first_stuck() = runTest {
        val result = AwaitedResult<String>()
        val first = async { result.await {} }
        runCurrent()

        val second = async { result.await {} }
        runCurrent()
        result.finish("둘째")

        assertNull(first.await())
        assertEquals("둘째", second.await())
    }

    @Test
    fun a_result_nobody_waits_for_is_dropped() = runTest {
        val result = AwaitedResult<String>()

        result.finish("늦은 결과")

        val next = async { result.await {} }
        runCurrent()
        result.finish("새 결과")
        assertEquals("새 결과", next.await())
    }
}
