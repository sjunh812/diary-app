package org.sjhstudio.diary.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 사진 크게 보기의 크기와 위치(스펙 002 FR-020).
class PhotoZoomTest {
    private val screen = Size(400f, 800f)
    private val center = Offset(200f, 400f)

    /** 가로로 긴 사진. 화면에 맞추면 폭 400, 높이 200이다. */
    private fun landscape() = PhotoZoom(Size(2000f, 1000f)).apply { container = screen }

    @Test
    fun a_photo_starts_at_its_own_size() {
        val zoom = landscape()

        assertFalse(zoom.zoomed)
        assertEquals(Offset.Zero, zoom.offset)
    }

    @Test
    fun a_pan_stops_at_the_edge_of_the_photo() {
        val zoom = landscape()
        zoom.transform(2f, Offset.Zero, center)

        zoom.transform(1f, Offset(5000f, 0f), center)

        // 두 배면 폭 800이라 양쪽으로 200씩 움직일 수 있다.
        assertEquals(200f, zoom.offset.x)
    }

    @Test
    fun a_photo_shorter_than_the_screen_stays_centered() {
        val zoom = landscape()
        zoom.transform(2f, Offset.Zero, center)

        zoom.transform(1f, Offset(0f, 5000f), center)

        // 두 배여도 높이 400이라 화면보다 작다.
        assertEquals(0f, zoom.offset.y)
    }

    @Test
    fun a_photo_of_unknown_size_is_treated_as_filling_the_screen() {
        val zoom = PhotoZoom(null).apply { container = screen }
        zoom.transform(2f, Offset.Zero, center)

        zoom.transform(1f, Offset(5000f, 5000f), center)

        assertEquals(Offset(200f, 400f), zoom.offset)
    }

    @Test
    fun it_never_grows_past_the_limit_or_shrinks_below_its_own_size() {
        val zoom = landscape()

        zoom.transform(100f, Offset.Zero, center)
        assertEquals(MAX_ZOOM, zoom.scale)

        zoom.transform(0.001f, Offset.Zero, center)
        assertEquals(1f, zoom.scale)
        assertEquals(Offset.Zero, zoom.offset)
    }

    /** 손가락 사이 지점이 제자리에 남는다. */
    @Test
    fun pinching_keeps_the_point_between_the_fingers_in_place() {
        val zoom = PhotoZoom(null).apply { container = screen }
        val fingers = Offset(300f, 500f)

        zoom.transform(2f, Offset.Zero, fingers)

        // 가운데에서 (100, 100) 떨어진 곳이 두 배로 밀려나지 않게 반대로 옮긴다.
        assertEquals(Offset(-100f, -100f), zoom.offset)
    }

    @Test
    fun double_tap_zooms_in_around_the_tap_and_back_out() {
        val zoom = PhotoZoom(null).apply { container = screen }

        val (scale, offset) = zoom.doubleTapTarget(Offset(300f, 400f))
        zoom.snapTo(scale, offset)
        assertEquals(DOUBLE_TAP_ZOOM, zoom.scale)
        assertTrue(zoom.offset.x < 0f)

        val (back, origin) = zoom.doubleTapTarget(Offset(300f, 400f))
        zoom.snapTo(back, origin)
        assertEquals(1f, zoom.scale)
        assertEquals(Offset.Zero, zoom.offset)
    }

    /** 살짝 커진 채로 남으면 넘기기가 막힌다. */
    @Test
    fun letting_go_just_above_its_own_size_settles_back() {
        val zoom = landscape()
        zoom.transform(1.03f, Offset.Zero, center)

        zoom.settle()

        assertFalse(zoom.zoomed)
        assertEquals(Offset.Zero, zoom.offset)
    }

    @Test
    fun letting_go_well_above_its_own_size_stays() {
        val zoom = landscape()
        zoom.transform(1.5f, Offset.Zero, center)

        zoom.settle()

        assertTrue(zoom.zoomed)
    }
}
