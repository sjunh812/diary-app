package org.sjhstudio.diary.core.ui

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

// 안전망은 앱을 끄지 않되 받은 예외를 보고로 넘겨야 고칠 곳을 알 수 있음
class SafetyNetTest {
    private val reported = mutableListOf<Exception>()

    init {
        SafetyNet.report = { reported += it }
    }

    @AfterTest
    fun reset() {
        SafetyNet.report = {}
    }

    @Test
    fun a_failing_launch_is_reported_and_still_handed_to_the_caller() = runTest {
        var handled: Exception? = null

        launchSafely(onFailure = { handled = it }) { error("빠뜨린 예외") }.join()

        assertEquals(1, reported.size)
        assertEquals(reported.single(), handled)
    }

    @Test
    fun a_failing_flow_is_reported_and_keeps_the_last_value() = runTest(UnconfinedTestDispatcher()) {
        val state = flow<Int> { throw IllegalStateException("읽기 실패") }
            .stateInSafely(backgroundScope, SharingStarted.Eagerly, 7)

        assertEquals(7, state.value)
        assertEquals(1, reported.size)
    }

    @Test
    fun a_success_reports_nothing() = runTest {
        launchSafely { MutableStateFlow(1).value }.join()

        assertEquals(0, reported.size)
    }
}
