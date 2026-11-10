package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

// 저장 공간이 찬 기기에서 쓰기가 실패해도 앱이 꺼지지 않고 알림이 감
@OptIn(ExperimentalCoroutinesApi::class)
class WriteFailuresTest {
    @Test
    fun a_failed_write_is_reported_and_does_not_throw() = runTest(UnconfinedTestDispatcher()) {
        var reported = 0
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            WriteFailures.events.first()
            reported++
        }

        launchWrite { error("공간 없음") }.join()
        advanceUntilIdle()

        watcher.join()
        assertEquals(1, reported)
    }

    @Test
    fun cancelling_is_not_a_failure() = runTest(UnconfinedTestDispatcher()) {
        val job = launchWrite { throw CancellationException("화면을 떠남") }
        job.join()

        assertTrue(job.isCancelled)
    }
}
