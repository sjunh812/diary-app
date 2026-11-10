package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.Plan

// 재설치 직후 스토어 답이 오기 전에는 요금제를 확정하지 않음
// 사진을 고른 Plus 사용자에게 10장 한도를 대지 않고, 광고와 Plus 기능도 확정된 값만 봄
class ConfirmedPlanTest {
    private class Plans(start: Plan, known: Boolean) : PlanRepository {
        override val plan = MutableStateFlow(start)
        val knownState = MutableStateFlow(known)
        override val known: StateFlow<Boolean> get() = knownState
    }

    @Test
    fun waits_for_the_store_before_deciding_the_limit() = runTest {
        val plans = Plans(Plan.FREE, known = false)

        val limit = async { plans.awaitConfirmed() }
        runCurrent()
        plans.plan.value = Plan.PLUS
        plans.knownState.value = true

        assertEquals(PhotoLimit.PLUS, limit.await().photoLimit)
    }

    @Test
    fun an_unknown_plan_is_not_confirmed() = runTest {
        val plans = Plans(Plan.PLUS, known = false)

        assertEquals(null, plans.confirmedNow)
        plans.knownState.value = true
        assertEquals(Plan.PLUS, plans.confirmedNow)
    }

    @Test
    fun falls_back_to_the_current_plan_when_the_store_never_answers() = runTest {
        val plans = Plans(Plan.FREE, known = false)

        val limit = async { plans.awaitConfirmed(wait = 3.seconds) }
        advanceTimeBy(3.seconds.inWholeMilliseconds + 1)

        assertEquals(PhotoLimit.FREE, limit.await().photoLimit)
    }
}
