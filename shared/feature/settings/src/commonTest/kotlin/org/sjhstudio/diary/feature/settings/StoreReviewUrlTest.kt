package org.sjhstudio.diary.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 설정 "리뷰 남기기"가 기기마다 제 스토어로 가는지 */
class StoreReviewUrlTest {
    @Test
    fun androidOpensPlayListing() {
        assertEquals("https://play.google.com/store/apps/details?id=org.sjhstudio.diary", storeReviewUrl("android"))
    }

    @Test
    fun iosOpensAppStoreReview() {
        assertEquals("https://apps.apple.com/app/id6835116731?action=write-review", storeReviewUrl("ios"))
    }

    @Test
    fun unknownPlatformHidesRow() {
        assertNull(storeReviewUrl(""))
    }
}
