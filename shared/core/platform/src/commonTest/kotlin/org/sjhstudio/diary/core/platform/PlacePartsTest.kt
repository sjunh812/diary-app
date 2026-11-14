package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 기기 지오코더가 실제로 돌려준 값으로 시험 */
class PlacePartsTest {
    @Test
    fun seoul_uses_gu_and_dong() {
        assertEquals("마포구 서교동", PlaceParts("KR", "서울특별시", null, "마포구", "서교동").label())
    }

    @Test
    fun city_without_gu_uses_city_and_dong() {
        assertEquals("제주시 이도이동", PlaceParts("KR", "제주특별자치도", "제주시", null, "이도이동").label())
    }

    @Test
    fun county_uses_gun_and_eup() {
        assertEquals("담양군 담양읍", PlaceParts("KR", "전라남도", "담양군", null, "담양읍").label())
    }

    @Test
    fun city_with_gu_keeps_the_smaller_area() {
        assertEquals("수정구 태평동", PlaceParts("KR", "경기도", "성남시", "수정구", "태평동").label())
    }

    @Test
    fun ios_seoul_has_no_gu_and_uses_city() {
        assertEquals("서울특별시 서초동", PlaceParts("KR", "서울특별시", "서울특별시", null, "서초동").label())
    }

    @Test
    fun road_name_is_not_a_neighborhood() {
        assertEquals("강릉시", PlaceParts("KR", "강원특별자치도", "강릉시", null, "중앙시장2길").label())
    }

    @Test
    fun only_region_is_still_a_place() {
        assertEquals("인천광역시", PlaceParts("KR", "인천광역시").label())
    }

    @Test
    fun korean_address_in_english_keeps_dong() {
        assertEquals("Mapo-gu Seogyo-dong", PlaceParts("KR", "Seoul", null, "Mapo-gu", "Seogyo-dong").label())
    }

    @Test
    fun abroad_uses_two_small_areas() {
        assertEquals("Manhattan, New York", PlaceParts("US", "New York", "New York", "Manhattan", "West 34th Street").label())
        assertEquals("Chiyoda City, Tokyo", PlaceParts("JP", "Tokyo", "Chiyoda City", null, "1-chōme").label())
    }

    @Test
    fun nothing_known_is_no_name() {
        assertNull(PlaceParts("KR").label())
        assertNull(PlaceParts(" ", " ", " ", " ", " ").label())
    }
}
