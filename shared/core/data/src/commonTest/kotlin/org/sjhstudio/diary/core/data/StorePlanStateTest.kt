package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.sjhstudio.diary.core.model.Plan

// 산 사람에게 광고가 스치지 않게 하는 규칙. 결제 라이브러리 밖에서 전이를 봄
class StorePlanStateTest {
    private val stored = ArrayList<Plan>()

    private fun state(saved: Plan?) = StorePlanState(saved) { stored += it }

    @Test
    fun a_reinstall_stays_unknown_until_the_store_answers() {
        val state = state(null)
        state.onAnswer(Plan.FREE)
        assertFalse(state.known.value)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun a_reinstalled_buyer_gets_the_plan_after_the_sync() {
        val state = state(null)
        state.onAnswer(Plan.FREE)
        state.onRecovered(Plan.PLUS)
        assertEquals(Plan.PLUS, state.plan.value)
        assertTrue(state.known.value)
        assertEquals(listOf(Plan.PLUS), stored)
    }

    @Test
    fun a_free_user_after_the_sync_is_remembered_and_later_answers_count() {
        val state = state(null)
        state.onRecovered(Plan.FREE)
        assertEquals(listOf(Plan.FREE), stored)
        state.onAnswer(Plan.AD_FREE)
        state.onAnswer(Plan.FREE)
        assertEquals(listOf(Plan.FREE, Plan.AD_FREE, Plan.FREE), stored)
    }

    @Test
    fun a_store_that_knows_the_buyer_needs_no_sync() {
        val state = state(null)
        state.onAnswer(Plan.PLUS)
        assertEquals(Plan.PLUS, state.plan.value)
        assertTrue(state.known.value)
    }

    @Test
    fun offline_keeps_the_saved_plan() {
        val state = state(Plan.PLUS)
        assertTrue(state.known.value)
        assertEquals(Plan.PLUS, state.plan.value)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun a_refund_drops_the_plan_and_the_saved_value() {
        val state = state(Plan.PLUS)
        state.onAnswer(Plan.FREE)
        assertEquals(Plan.FREE, state.plan.value)
        assertEquals(listOf(Plan.FREE), stored)
    }

    @Test
    fun an_answer_the_server_gave_before_the_last_one_is_dropped() {
        val state = state(Plan.FREE)
        state.onPurchased(Plan.PLUS, at(2))
        assertFalse(state.onAnswer(Plan.FREE, at(1)))
        assertEquals(Plan.PLUS, state.plan.value)
        assertEquals(listOf(Plan.PLUS), stored)
        assertTrue(state.onAnswer(Plan.FREE, at(3)))
        assertEquals(Plan.FREE, state.plan.value)
    }

    /** 기기 시계로 매긴 오프라인 답이 앞서 있어도 결제와 복원의 답은 잃지 않음 */
    @Test
    fun a_purchase_or_restore_wins_over_an_answer_stamped_later() {
        val state = state(Plan.FREE)
        state.onAnswer(Plan.FREE, at(10))
        state.onPurchased(Plan.AD_FREE, at(5))
        assertEquals(Plan.AD_FREE, state.plan.value)
        assertFalse(state.onAnswer(Plan.FREE, at(4)))
        state.onRecovered(Plan.PLUS, at(1))
        assertEquals(Plan.PLUS, state.plan.value)
    }

    private fun at(second: Int) = Instant.fromEpochSeconds(1_800_000_000L + second)
}
