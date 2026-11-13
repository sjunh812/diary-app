package org.sjhstudio.diary.feature.write

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 사진 줄에서 길게 눌러 끄는 동안의 상태
 * Compose foundation의 지연 목록 끌어 옮기기 예제(LazyColumnDragAndDropDemo)와 같은 방식
 * 끄는 칸의 가운데가 옆 칸에 들어가면 자리를 바꿈
 * 줄 끝에 닿아 있는 동안은 손을 멈춰도 줄을 계속 밀어 화면 밖 자리까지 옮길 수 있음
 */
internal class PhotoDrag(
    private val list: LazyListState,
    private val scope: CoroutineScope,
    private val count: () -> Int,
    private val onSwap: (from: Int, to: Int) -> Unit,
    private val onDrop: (from: Int, to: Int) -> Unit
) {
    /** 끄는 칸의 지금 자리. 끌지 않으면 null */
    var dragging by mutableStateOf<Int?>(null)
        private set

    /** 놓은 뒤 제자리로 돌아오는 칸 */
    var settling by mutableStateOf<Int?>(null)
        private set

    val settleOffset: Float get() = settle.value

    /** 끄는 칸을 지금 자리에서 손 위치까지 옮길 거리 */
    val offset: Float get() = draggingInfo?.let { startOffset + dragged - it.offset } ?: 0f

    private var startIndex = 0
    private var key: Any? = null
    private var startOffset by mutableIntStateOf(0)
    private var dragged by mutableFloatStateOf(0f)
    private val settle = Animatable(0f)

    // 자리가 바뀌어도 같은 사진을 따라가게 키로 찾음
    private val draggingInfo: LazyListItemInfo? get() = key?.let { key -> list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } }

    /** 누른 곳에 사진이 있으면 끌기 시작. [x]는 줄의 왼쪽 끝부터 잰 거리 */
    fun start(x: Float): Boolean {
        // 칸 위치는 안쪽 여백 뒤부터 측정
        val at = x.toInt() + list.layoutInfo.viewportStartOffset
        val item =
            list.layoutInfo.visibleItemsInfo.firstOrNull { it.index < count() && at in it.offset..it.offset + it.size } ?: return false
        key = item.key
        dragging = item.index
        startIndex = item.index
        startOffset = item.offset
        dragged = 0f
        return true
    }

    fun dragBy(amount: Float) {
        dragged += amount
        swapIfOver()
    }

    /** 끄는 칸의 가운데가 다른 사진 위에 있으면 자리를 바꿈. 줄이 밀려 칸이 손 밑으로 들어온 때도 부름 */
    private fun swapIfOver() {
        // 바꾼 자리가 아직 배치되지 않았으면 기다림. 옛 배치로 또 바꾸면 끄는 사진이 손에서 달아남
        val item = draggingInfo?.takeIf { it.index == dragging } ?: return
        val middle = (item.offset + offset + item.size / 2f).toInt()
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index != item.index && it.index < count() && middle in it.offset..it.offset + it.size
        } ?: return
        // 맨 앞에 보이는 칸이 바뀌면 목록이 그 칸을 따라가 줄이 튐. 보던 자리를 그대로 둠
        val first = list.firstVisibleItemIndex
        if (item.index == first || target.index == first) list.requestScrollToItem(first, list.firstVisibleItemScrollOffset)
        onSwap(item.index, target.index)
        dragging = target.index
    }

    /**
     * 끄는 칸이 줄 끝의 밀기 구간에 들어간 거리. 오른쪽은 양수, 왼쪽은 음수
     * 구간은 칸의 절반. 칸이 줄 밖으로 다 나가야 밀리면 손가락이 화면 끝에 닿아야 함
     */
    private fun overEdge(): Float {
        val item = draggingInfo ?: return 0f
        val start = item.offset + offset
        val end = start + item.size
        val zone = item.size / 2f
        val info = list.layoutInfo
        // 끈 쪽으로만 밀어냄. 줄 끝에 있던 사진을 집자마자 밀리지 않게
        return when {
            dragged > 0 && end > info.viewportEndOffset - zone -> end - (info.viewportEndOffset - zone)
            dragged < 0 && start < info.viewportStartOffset + zone -> start - (info.viewportStartOffset + zone)
            else -> 0f
        }
    }

    /** 끄는 동안 프레임마다 부름. 줄 밖으로 나간 만큼 빠르게 밀고, 밀린 뒤 손 밑의 칸과 자리를 바꿈 */
    suspend fun scrollAtEdge() {
        val over = overEdge()
        if (over == 0f) return
        val moved = list.scrollBy((over * EDGE_SPEED).coerceIn(-EDGE_STEP_MAX, EDGE_STEP_MAX))
        if (moved != 0f) swapIfOver()
    }

    /** 놓거나 끌기가 끊김. 옮긴 자리를 알리고 칸을 제자리로 */
    fun drop() {
        val to = dragging ?: return
        val from = startIndex
        val left = offset
        dragging = null
        key = null
        dragged = 0f
        settling = to
        scope.launch {
            settle.snapTo(left)
            settle.animateTo(0f)
            settling = null
        }
        if (from != to) onDrop(from, to)
    }
}

/** 줄 밖으로 나간 거리 가운데 한 프레임에 미는 몫 */
private const val EDGE_SPEED = 0.25f

/** 한 프레임에 미는 거리의 상한(px). 너무 빨라 자리를 지나치지 않게 */
private const val EDGE_STEP_MAX = 40f

@Composable
internal fun rememberPhotoDrag(
    list: LazyListState,
    count: () -> Int,
    onSwap: (from: Int, to: Int) -> Unit,
    onDrop: (from: Int, to: Int) -> Unit
): PhotoDrag {
    val scope = rememberCoroutineScope()
    val currentCount by rememberUpdatedState(count)
    val swap by rememberUpdatedState(onSwap)
    val dropped by rememberUpdatedState(onDrop)
    val drag = remember(list) {
        PhotoDrag(list, scope, { currentCount() }, { from, to -> swap(from, to) }, { from, to -> dropped(from, to) })
    }
    val active = drag.dragging != null
    LaunchedEffect(drag, active) {
        while (active) {
            withFrameNanos { }
            drag.scrollAtEdge()
        }
    }
    return drag
}
