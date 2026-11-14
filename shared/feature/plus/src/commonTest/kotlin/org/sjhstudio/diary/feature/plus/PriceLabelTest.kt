package org.sjhstudio.diary.feature.plus

import kotlin.test.Test
import kotlin.test.assertEquals

class PriceLabelTest {
    @Test
    fun groups_every_three_digits() {
        assertEquals("900", groupThousands(900))
        assertEquals("9,900", groupThousands(9_900))
        assertEquals("14,900", groupThousands(14_900))
        assertEquals("1,234,567", groupThousands(1_234_567))
    }
}
