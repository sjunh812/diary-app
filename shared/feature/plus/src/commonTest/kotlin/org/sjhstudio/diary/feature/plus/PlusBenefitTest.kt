package org.sjhstudio.diary.feature.plus

import kotlin.test.Test
import kotlin.test.assertEquals

// 결제 화면 혜택 묶음(스펙 008 US6)
class PlusBenefitTest {
    @Test
    fun core_is_ads_photos_stats_in_order() {
        assertEquals(listOf(PlusBenefit.AD_FREE, PlusBenefit.PHOTOS, PlusBenefit.STATS), coreBenefits)
    }

    @Test
    fun other_keeps_order_without_entry() {
        assertEquals(listOf(PlusBenefit.CHECKLIST, PlusBenefit.LIST_FORMATTING), otherBenefits(from = null))
    }

    @Test
    fun entry_benefit_comes_first() {
        assertEquals(listOf(PlusBenefit.LIST_FORMATTING, PlusBenefit.CHECKLIST), otherBenefits(from = PlusBenefit.LIST_FORMATTING))
    }

    @Test
    fun core_entry_does_not_move_other() {
        assertEquals(listOf(PlusBenefit.CHECKLIST, PlusBenefit.LIST_FORMATTING), otherBenefits(from = PlusBenefit.PHOTOS))
    }

    @Test
    fun lines_are_capped_and_keep_the_entry() {
        assertEquals(listOf(PlusBenefit.LIST_FORMATTING), otherBenefits(from = PlusBenefit.LIST_FORMATTING, limit = 1))
    }
}
