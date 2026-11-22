package org.sjhstudio.diary.feature.plus

import kotlin.test.Test
import kotlin.test.assertEquals

// 결제 화면 혜택 묶음(스펙 008 US6)
class PlusBenefitTest {
    @Test
    fun core_is_ads_photos_stats_theme_in_order() {
        assertEquals(listOf(PlusBenefit.AD_FREE, PlusBenefit.PHOTOS, PlusBenefit.STATS, PlusBenefit.THEME), coreBenefits)
    }

    @Test
    fun entered_other_benefit_is_shown() {
        assertEquals(PlusBenefit.LIST_FORMATTING, enteredOther(PlusBenefit.LIST_FORMATTING))
    }

    @Test
    fun core_or_no_entry_shows_nothing_extra() {
        assertEquals(null, enteredOther(PlusBenefit.THEME))
        assertEquals(null, enteredOther(null))
    }
}
