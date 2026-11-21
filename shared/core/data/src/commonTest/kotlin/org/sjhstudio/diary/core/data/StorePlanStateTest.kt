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

    private fun state(saved: Plan?) = StorePlanState(saved?.let(::SavedPlan)) { stored += it.plan }

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
        state.onAnswer(Plan.PLUS, at(2))
        assertFalse(state.onAnswer(Plan.FREE, at(1)))
        assertEquals(Plan.PLUS, state.plan.value)
        assertEquals(listOf(Plan.PLUS), stored)
        assertTrue(state.onAnswer(Plan.FREE, at(3)))
        assertEquals(Plan.FREE, state.plan.value)
    }

    /** 결제의 답도 같은 규칙. 서버 시각이 없는 답(기기에서 계산)은 받되 기준을 옮기지 않음 */
    @Test
    fun an_answer_without_server_time_neither_blocks_nor_moves_the_order() {
        val state = state(Plan.FREE)
        state.onAnswer(Plan.FREE, at(10))
        assertTrue(state.onAnswer(Plan.PLUS, null))
        assertEquals(Plan.PLUS, state.plan.value)
        assertFalse(state.onAnswer(Plan.FREE, at(9)))
        assertTrue(state.onRecovered(Plan.AD_FREE, at(11)))
        assertEquals(Plan.AD_FREE, state.plan.value)
    }

    /** 다시 켜도 남긴 기준 시각으로 옛 답을 가림. 예전 버전이 남긴 값(시각 없음)은 지금처럼 받음 */
    @Test
    fun the_saved_answer_time_survives_a_restart() {
        val saved = ArrayList<SavedPlan>()
        StorePlanState(SavedPlan(Plan.FREE)) { saved += it }.onAnswer(Plan.PLUS, at(5))
        assertEquals(SavedPlan(Plan.PLUS, at(5)), saved.last())

        val restarted = StorePlanState(saved.last()) { saved += it }
        assertFalse(restarted.onAnswer(Plan.FREE, at(4)))
        assertEquals(Plan.PLUS, restarted.plan.value)

        val old = StorePlanState(SavedPlan(Plan.PLUS)) { saved += it }
        assertTrue(old.onAnswer(Plan.FREE, at(4)))
    }

    private fun at(second: Int) = Instant.fromEpochSeconds(1_800_000_000L + second)
}
