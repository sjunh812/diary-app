package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanTest {
    @Test
    fun free_shows_ads_with_ten_photos() {
        assertTrue(Plan.FREE.showsAds)
        assertEquals(10, Plan.FREE.photoLimit)
        assertFalse(Plan.FREE.advancedStats)
    }

    @Test
    fun ad_free_only_removes_ads() {
        assertFalse(Plan.AD_FREE.showsAds)
        assertEquals(Plan.FREE.photoLimit, Plan.AD_FREE.photoLimit)
        assertFalse(Plan.AD_FREE.advancedStats)
    }

    @Test
    fun plus_has_everything() {
        assertFalse(Plan.PLUS.showsAds)
        assertEquals(30, Plan.PLUS.photoLimit)
        assertTrue(Plan.PLUS.advancedStats)
    }
}
