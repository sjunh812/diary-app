package org.sjhstudio.diary.feature.plus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// "새 기능"은 그 혜택이 들어온 기능 업데이트 동안만
class NewBenefitTest {
    @Test
    fun mood_theme_is_new_through_the_2_3_releases() {
        assertEquals(setOf(PlusBenefit.MOOD_THEME), newBenefitsIn("2.3.0"))
        assertEquals(setOf(PlusBenefit.MOOD_THEME), newBenefitsIn("2.3.4"))
    }

    @Test
    fun nothing_is_new_before_or_after_its_release() {
        assertTrue(newBenefitsIn("2.2.1").isEmpty())
        assertTrue(newBenefitsIn("2.4.0").isEmpty())
        assertTrue(newBenefitsIn("3.3.0").isEmpty())
    }

    @Test
    fun an_unknown_version_shows_nothing_new() {
        assertTrue(newBenefitsIn("").isEmpty())
    }
}
