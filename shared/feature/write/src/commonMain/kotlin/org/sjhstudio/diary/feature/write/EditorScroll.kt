package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp

/**
 * 본문 스크롤이 커서나 선택 끝을 보이게 할 때 가장자리에서 [margin]만큼 띄운다.
 * 편집기는 스스로 스크롤하지 않고 바깥 칸이 스크롤한다.
 * 기본값은 커서를 가장자리에 딱 붙여서, 쓰는 줄이 도구 줄에 붙고 선택 핸들이 도구 줄·키보드에 걸려 더 끌 수 없었다(2026-11-10 사용자 지적).
 * 띄워 두면 핸들을 아래로 끄는 동안 선택 끝이 바뀔 때마다 본문이 조금씩 따라 올라온다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EditorScrollMargin(margin: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val px = with(LocalDensity.current) { margin.toPx() }
    val spec = remember(px) { MarginSpec(px) }
    Box(modifier) { CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content) }
}

@OptIn(ExperimentalFoundationApi::class)
private class MarginSpec(private val margin: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        scrollDistance(offset, size, containerSize, margin)
}

/**
 * 보일 곳의 시작 [offset]과 크기 [size]를 화면 높이 [containerSize] 안에 [margin]을 두고 넣는 데 필요한 스크롤 거리.
 * 이미 보이면 0, 위로 넘치면 음수, 아래로 넘치면 양수다.
 * 여백까지 다 넣을 수 없으면 기본 규칙(가장자리에 맞춤)을 따른다.
 */
internal fun scrollDistance(offset: Float, size: Float, containerSize: Float, margin: Float): Float {
    val room = if (size + margin * 2 <= containerSize) margin else 0f
    val start = offset - room
    val end = offset + size + room
    return when {
        start >= 0f && end <= containerSize -> 0f
        start < 0f && end > containerSize -> 0f
        start < 0f -> start
        else -> end - containerSize
    }
}

/**
 * 선택에서 움직인 끝의 위치.
 * 핸들 하나를 끌면 한쪽 끝만 바뀐다.
 * 커서에서 새로 고른 낱말(길게 누르기)이면 끝 쪽을 본다.
 * 이미 고른 상태에서 두 끝이 함께 바뀌면(모두 선택) 화면을 옮기지 않는다. 옮기면 글 끝으로 튄다.
 * 커서만 있거나 바뀐 것이 없으면 null이다.
 */
internal fun movingEdge(before: TextRange, after: TextRange): Int? = when {
    after == before || after.collapsed -> null
    after.start != before.start && after.end == before.end -> after.start
    after.end != before.end && after.start == before.start -> after.end
    before.collapsed -> after.end
    else -> null
}

/**
 * 본문 칸을 누른 자리를 글자 위치로 바꾸는 데 쓰는 배치 정보.
 * [viewport]는 누름을 받는 스크롤 칸, [field]는 입력칸, [layout]은 그 글 배치다.
 */
internal class EditorGeometry {
    var viewport: LayoutCoordinates? = null
    var field: LayoutCoordinates? = null
    var layout: TextLayoutResult? = null

    /**
     * 스크롤 칸에서 누른 [tap]에 가장 가까운 글자 위치.
     * 글 아래 빈 곳이면 null이라 부르는 쪽이 글 끝에 둔다.
     */
    fun offsetAt(tap: Offset): Int? {
        val viewport = viewport?.takeIf { it.isAttached }
        val field = field?.takeIf { it.isAttached }
        val layout = layout
        if (viewport == null || field == null || layout == null) return null
        val local = field.localPositionOf(viewport, tap)
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()
        return if (local.y >
            height
        ) {
            null
        } else {
            layout.getOffsetForPosition(Offset(local.x.coerceIn(0f, width), local.y.coerceIn(0f, height)))
        }
    }
}
