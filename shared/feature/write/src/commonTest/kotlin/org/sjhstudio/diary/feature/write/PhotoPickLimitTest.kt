package org.sjhstudio.diary.feature.write

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhotoPickLimitTest {
    @Test
    fun picker_that_limits_count_gets_the_remaining_slots() {
        assertEquals(3, pickMaxItems(limit = 10, attached = 7, pickerLimitsCount = true))
    }

    @Test
    fun never_asks_the_picker_for_zero() {
        assertEquals(1, pickMaxItems(limit = 10, attached = 10, pickerLimitsCount = true))
    }

    @Test
    fun picker_that_cannot_limit_gets_no_cap_so_overflow_is_noticed() {
        assertNull(pickMaxItems(limit = 10, attached = 7, pickerLimitsCount = false))
    }
}
