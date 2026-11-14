package org.sjhstudio.diary.feature.home

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min

internal const val MAX_ZOOM = 4f
internal const val DOUBLE_TAP_ZOOM = 2.5f

/** 이보다 작게 키운 채 손을 떼면 원래 크기로 */
private const val SETTLE_ZOOM = 1.05f

/**
 * 사진 한 장의 크기와 위치. 가장자리가 화면 안으로 들어오지 않을 만큼만 움직임
 * [photo]는 원래 크기, 모르면 화면을 채운다고 봄
 */
@Stable
internal class PhotoZoom(private val photo: Size? = null) {
    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** 사진이 그려지는 칸의 크기 */
    var container = Size.Zero

    val zoomed: Boolean get() = scale > 1f

    /** 두 손가락 움직임 하나 반영. 손가락 사이 지점이 제자리에 남게 옮김 */
    fun transform(zoomChange: Float, pan: Offset, centroid: Offset) {
        val next = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        val focus = centroid - container.center
        val moved = (offset - focus) * (next / scale) + focus + pan
        scale = next
        offset = bounded(moved, next)
    }

    /** 손을 뗐을 때. 살짝 커진 채로 남아 넘기기가 막히지 않게 */
    fun settle() {
        if (scale < SETTLE_ZOOM) snapTo(1f, Offset.Zero)
    }

    /** 두 번 눌렀을 때 갈 크기와 위치. 키웠으면 되돌리고, 아니면 누른 곳을 키움 */
    fun doubleTapTarget(tap: Offset): Pair<Float, Offset> {
        if (zoomed) return 1f to Offset.Zero
        val focus = tap - container.center
        return DOUBLE_TAP_ZOOM to bounded(focus * (1 - DOUBLE_TAP_ZOOM), DOUBLE_TAP_ZOOM)
    }

    fun snapTo(targetScale: Float, targetOffset: Offset) {
        scale = targetScale
        offset = targetOffset
    }

    suspend fun animateTo(target: Pair<Float, Offset>, durationMillis: Int) {
        val fromScale = scale
        val fromOffset = offset
        val (toScale, toOffset) = target
        animate(0f, 1f, animationSpec = tween(durationMillis)) { fraction, _ ->
            snapTo(lerp(fromScale, toScale, fraction), lerp(fromOffset, toOffset, fraction))
        }
    }

    private fun bounded(wanted: Offset, atScale: Float): Offset {
        val fitted = fittedSize()
        val x = max((fitted.width * atScale - container.width) / 2, 0f)
        val y = max((fitted.height * atScale - container.height) / 2, 0f)
        return Offset(wanted.x.coerceIn(-x, x), wanted.y.coerceIn(-y, y))
    }

    /** 사진 전체가 보이게 맞췄을 때 화면에서 차지하는 크기 */
    private fun fittedSize(): Size {
        val size = photo?.takeIf { it.width > 0f && it.height > 0f } ?: return container
        val ratio = min(container.width / size.width, container.height / size.height)
        return Size(size.width * ratio, size.height * ratio)
    }
}
