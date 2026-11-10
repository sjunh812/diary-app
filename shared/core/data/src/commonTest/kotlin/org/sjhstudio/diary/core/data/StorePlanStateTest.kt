package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.Plan

// 산 사람에게 광고가 스치지 않게 하는 규칙. 결제 라이브러리 밖에서 전이를 봄
class StorePlanStateTest {
    private val stored = ArrayList<Plan>()

    private fun state(saved: Plan?) = StorePlanState(saved) { stored += it }

    @Test
    fun a_reinstall_stays_unknown_until_the_store_answers() {
        val state = state(null)
        assertTrue(state.onAnswer(Plan.FREE))
        assertFalse(state.known.value)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun a_reinstalled_buyer_gets_the_plan_after_the_sync() {
        val state = state(null)
        state.onAnswer(Plan.FREE)
        state.onSynced(Plan.PLUS)
        assertEquals(Plan.PLUS, state.plan.value)
        assertTrue(state.known.value)
        assertEquals(listOf(Plan.PLUS), stored)
    }

    @Test
    fun a_failed_sync_saves_nothing_so_the_next_launch_tries_again() {
        val state = state(null)
        assertTrue(state.onAnswer(Plan.FREE))
        state.onUnreachable()
        assertTrue(state.known.value)
        assertTrue(stored.isEmpty())
        assertTrue(state(null).onAnswer(Plan.FREE))
    }

    @Test
    fun a_free_user_after_the_sync_is_remembered() {
        val state = state(null)
        state.onSynced(Plan.FREE)
        assertEquals(listOf(Plan.FREE), stored)
        assertFalse(state.onAnswer(Plan.FREE))
    }

    @Test
    fun a_store_that_knows_the_buyer_skips_the_sync() {
        val state = state(null)
        assertFalse(state.onAnswer(Plan.PLUS))
        assertEquals(Plan.PLUS, state.plan.value)
    }

    @Test
    fun offline_keeps_the_saved_plan() {
        val state = state(Plan.PLUS)
        assertTrue(state.known.value)
        state.onUnreachable()
        assertEquals(Plan.PLUS, state.plan.value)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun a_refund_drops_the_plan_and_the_saved_value() {
        val state = state(Plan.PLUS)
        assertFalse(state.onAnswer(Plan.FREE))
        assertEquals(Plan.FREE, state.plan.value)
        assertEquals(listOf(Plan.FREE), stored)
    }
}
