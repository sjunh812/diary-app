package org.sjhstudio.diary.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.EntryPhoto

class GalleryRatioTest {
    private fun photo(width: Int?, height: Int?) = EntryPhoto("p", "a.jpg", width, height)

    /** 한 장이면 사진 비율 그대로라 전체 보기와 같은 장면 */
    @Test
    fun one_photo_keeps_its_own_ratio() {
        assertEquals(4f / 3f, galleryRatio(listOf(photo(4000, 3000))), 0.001f)
        assertEquals(1f, galleryRatio(listOf(photo(1080, 1080))), 0.001f)
    }

    /** 세로 사진은 정사각형, 아주 넓은 사진은 1.91:1에서 멈춤. 본문을 밀어내지 않게 */
    @Test
    fun tall_or_very_wide_photo_is_clamped() {
        assertEquals(1f, galleryRatio(listOf(photo(3000, 4000))), 0.001f)
        assertEquals(1f, galleryRatio(listOf(photo(1080, 2400))), 0.001f)
        assertEquals(1.91f, galleryRatio(listOf(photo(4000, 1000))), 0.001f)
    }

    /** 크기를 모르는 사진과 여러 장은 지금 격자 비율 */
    @Test
    fun unknown_size_and_many_photos_use_the_grid_ratio() {
        assertEquals(GALLERY_RATIO, galleryRatio(listOf(photo(null, null))), 0.001f)
        assertEquals(GALLERY_RATIO, galleryRatio(listOf(photo(0, 100))), 0.001f)
        assertEquals(GALLERY_RATIO, galleryRatio(listOf(photo(1000, 3000), photo(3000, 1000))), 0.001f)
    }
}
