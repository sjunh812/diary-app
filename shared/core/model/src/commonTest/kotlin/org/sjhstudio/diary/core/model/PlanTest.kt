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
        assertFalse(Plan.FREE.checklist)
    }

    @Test
    fun ad_free_only_removes_ads() {
        assertFalse(Plan.AD_FREE.showsAds)
        assertEquals(Plan.FREE.photoLimit, Plan.AD_FREE.photoLimit)
        assertFalse(Plan.AD_FREE.advancedStats)
        assertFalse(Plan.AD_FREE.checklist)
    }

    @Test
    fun plus_has_everything() {
        assertFalse(Plan.PLUS.showsAds)
        assertEquals(30, Plan.PLUS.photoLimit)
        assertTrue(Plan.PLUS.advancedStats)
        assertTrue(Plan.PLUS.checklist)
    }

    @Test
    fun ad_free_buys_only_the_ads() {
        assertFalse(Plan.AD_FREE.showsAds)
        assertEquals(Plan.FREE.photoLimit, Plan.AD_FREE.photoLimit)
        assertEquals(Plan.FREE.checklist, Plan.AD_FREE.checklist)
        assertEquals(Plan.FREE.advancedStats, Plan.AD_FREE.advancedStats)
    }

    @Test
    fun plus_holds_everything_paid() {
        assertFalse(Plan.PLUS.showsAds)
        assertEquals(PhotoLimit.PLUS, Plan.PLUS.photoLimit)
        assertTrue(Plan.PLUS.checklist && Plan.PLUS.advancedStats)
    }

    @Test
    fun free_keeps_ten_photos() {
        assertEquals(10, Plan.FREE.photoLimit)
        assertEquals(30, Plan.PLUS.photoLimit)
    }
}
