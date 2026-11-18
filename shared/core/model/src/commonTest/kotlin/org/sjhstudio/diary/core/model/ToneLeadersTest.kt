package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ToneLeadersTest {
    @Test
    fun picksRowWithMostOfEachTone() {
        val shares =
            listOf(
                ToneShare(good = 1, total = 9, hard = 4),
                ToneShare(good = 4, total = 7, hard = 2),
                ToneShare(good = 1, total = 5, hard = 3)
            )
        assertEquals(ToneLeaders(good = 1, hard = 2), ToneLeaders.of(shares))
    }

    @Test
    fun tieOnShownPercentNamesNoRow() {
        // 2/4와 3/6은 둘 다 50%로 보여 어느 쪽도 1등이 아님
        val shares = listOf(ToneShare(good = 2, total = 4, hard = 1), ToneShare(good = 3, total = 6, hard = 0))
        assertEquals(ToneLeaders(good = null, hard = 0), ToneLeaders.of(shares))
    }

    @Test
    fun zeroIsNotALeader() {
        val shares = listOf(ToneShare(good = 3, total = 5, hard = 0), ToneShare(good = 1, total = 5, hard = 0))
        assertEquals(ToneLeaders(good = 0, hard = null), ToneLeaders.of(shares))
    }

    @Test
    fun singleRowHasNothingToCompare() {
        assertEquals(ToneLeaders(null, null), ToneLeaders.of(listOf(ToneShare(good = 3, total = 5, hard = 1))))
        assertEquals(ToneLeaders(null, null), ToneLeaders.of(emptyList()))
    }
}
