package org.sjhstudio.diary.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 광고 자리는 정한 규칙 그대로(docs/business.md)
class AdSlotRuleTest {
    @Test
    fun slots_follow_the_business_rule() {
        for (index in listOf(4, 14, 24, 34)) assertTrue(isAdSlot(index), "$index")
        for (index in listOf(0, 1, 3, 5, 13, 15, 23, 25)) assertFalse(isAdSlot(index), "$index")
    }

    @Test
    fun slot_numbers_count_from_zero() {
        assertEquals(0, adOrder(4))
        assertEquals(1, adOrder(14))
        assertEquals(2, adOrder(24))
    }
}
