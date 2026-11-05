package org.sjhstudio.diary.app.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class AppScopeTest {
    @Test
    fun a_failed_job_is_noted_by_kind_and_the_message_stays_out() = runTest {
        val reports = mutableListOf<String>()
        val scope = appCoroutineScope(UnconfinedTestDispatcher(testScheduler)) { reports += it }

        scope.launch { throw IllegalStateException("일기 내용") }.join()

        assertEquals(listOf("IllegalStateException"), reports)
    }

    @Test
    fun one_failed_job_leaves_the_others_running() = runTest {
        val scope = appCoroutineScope(UnconfinedTestDispatcher(testScheduler)) {}
        var ran = false

        scope.launch { error("실패") }.join()
        scope.launch { ran = true }.join()

        assertTrue(ran)
    }

    @Test
    fun stopping_a_job_is_not_a_failure() = runTest {
        val reports = mutableListOf<String>()
        val scope = appCoroutineScope(UnconfinedTestDispatcher(testScheduler)) { reports += it }

        scope.launch { throw CancellationException("멈춤") }.join()

        assertTrue(reports.isEmpty())
    }
}
