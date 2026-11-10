package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryPhoto

// 두 번 눌러 키우고 되돌림, 원래 크기일 때만 넘김(스펙 002 FR-020)
@OptIn(ExperimentalTestApi::class)
class PhotoViewerUiTest {
    private val photos = List(PAGES) { EntryPhoto("p$it", null, 2000, 1000) }
    private lateinit var pager: PagerState
    private val zooms = HashMap<Int, PhotoZoom>()

    private fun ComposeUiTest.viewer() {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.size(400.dp, 800.dp).testTag(VIEWER)) {
                    pager = rememberPagerState { photos.size }
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        val zoom = remember { PhotoZoom(Size(2000f, 1000f)) }
                        zooms[page] = zoom
                        ZoomablePhoto(photos[page], zoom)
                    }
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.doubleTap() {
        onNodeWithTag(VIEWER).performTouchInput { doubleClick(center) }
        waitForIdle()
    }

    private fun ComposeUiTest.swipe() {
        onNodeWithTag(VIEWER).performTouchInput { swipeLeft() }
        waitForIdle()
    }

    @Test
    fun double_tap_zooms_in_and_double_tap_again_goes_back() = runComposeUiTest {
        viewer()

        doubleTap()
        assertEquals(DOUBLE_TAP_ZOOM, zooms.getValue(0).scale)

        doubleTap()
        assertFalse(zooms.getValue(0).zoomed)
    }

    @Test
    fun swiping_at_its_own_size_turns_the_page() = runComposeUiTest {
        viewer()

        swipe()

        assertEquals(1, pager.currentPage)
    }

    @Test
    fun swiping_a_zoomed_photo_moves_the_photo_not_the_page() = runComposeUiTest {
        viewer()
        doubleTap()
        val zoom = zooms.getValue(0)

        swipe()

        assertEquals(0, pager.currentPage)
        assertTrue(zoom.zoomed)
        // 손을 뗀 뒤에도 사진은 범위 안
        assertTrue(zoom.offset.x.isFinite() && zoom.offset.x < 0f, "${zoom.offset}")
    }

    @Test
    fun after_zooming_back_out_the_page_turns_again() = runComposeUiTest {
        viewer()
        doubleTap()
        swipe()
        assertEquals(0, pager.currentPage)

        doubleTap()
        swipe()

        assertEquals(1, pager.currentPage)
    }

    private companion object {
        const val VIEWER = "viewer"
        const val PAGES = 3
    }
}
