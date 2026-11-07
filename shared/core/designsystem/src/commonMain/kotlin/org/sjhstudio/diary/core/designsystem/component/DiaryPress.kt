package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha

private const val PRESS_MILLIS = 80
private const val RELEASE_MILLIS = 180

// 버튼·칩·키는 6%까지 줄이고, 화면 폭만 한 줄은 양옆이 6dp씩만 들어오게 줄인다(2026-11-03 사용자 결정, 토스 0.96보다 조금 더).
// 같은 비율로 줄이면 긴 줄이 눈에 띄게 오그라든다.
private const val MAX_SHRINK_RATIO = 0.06f
private val MaxShrink = 12.dp
private val FocusGap = 2.dp
private val FocusWidth = 3.dp

// 뗄 때 크기는 살짝 튕기며 돌아온다(토스 키패드의 bounce).
// 면은 튕기지 않고 그냥 옅어진다.
private val ReleaseBounce = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)

/**
 * 누름 효과(`docs/design.md` 상태).
 * 누르는 즉시 [shape] 모양으로 옅은 면(`color` 10%, 가리키면 8%)을 깔고
 * 살짝 줄였다가 손을 떼면 살짝 튕기며 되돌린다.
 * 토스·당근처럼 누른 것이 바로 반응해 보이게 하려고 물결은 쓰지 않는다
 * (물결은 누른 뒤 천천히 퍼져 늦게 반응하는 것처럼 보인다, 2026-11-02 사용자 결정).
 *
 * 칠한 면까지 같이 줄도록 `clip`·`background`보다 앞에 달고, 누르기는 같은 [interactionSource]의
 * `clickable(indication = null)`이 받는다.
 */
fun Modifier.diaryPress(interactionSource: InteractionSource, color: Color, shape: Shape = RectangleShape): Modifier =
    this then DiaryPressElement(interactionSource, color, shape)

/**
 * 보이는 크기대로 자리를 차지하고 누르는 곳만 넓힌다.
 * 작은 부품의 터치 영역을 48로 넓혀도 옆 내용과의 간격은 규격 그대로다.
 * 이 수식어 뒤에 `clickable`을 달고, 그 안쪽에서 같은 값만큼 패딩을 주어 보이는 면을 제자리에 둔다.
 */
fun Modifier.diaryTouchOutset(horizontal: Dp = Dp.Hairline, vertical: Dp = Dp.Hairline): Modifier = layout { measurable, constraints ->
    val sides = horizontal.roundToPx()
    val ends = vertical.roundToPx()
    val placeable = measurable.measure(constraints.offset(sides * 2, ends * 2))
    layout(placeable.width - sides * 2, placeable.height - ends * 2) { placeable.placeRelative(-sides, -ends) }
}

private data class DiaryPressElement(val interactionSource: InteractionSource, val color: Color, val shape: Shape) :
    ModifierNodeElement<DiaryPressNode>() {
    override fun create(): DiaryPressNode = DiaryPressNode(interactionSource, color, shape)

    override fun update(node: DiaryPressNode) = node.update(interactionSource, color, shape)

    override fun InspectorInfo.inspectableProperties() {
        name = "diaryPress"
    }
}

private class DiaryPressNode(private var interactionSource: InteractionSource, private var color: Color, private var shape: Shape) :
    Modifier.Node(),
    DrawModifierNode {
    private var pressed = Animatable(0f)
    private var shrink = Animatable(0f)
    private var hovered = false
    private var focused = false
    private var pressing: Job? = null
    private var releasing: Job? = null
    private var collecting: Job? = null

    fun update(source: InteractionSource, color: Color, shape: Shape) {
        this.color = color
        this.shape = shape
        if (source != interactionSource) {
            interactionSource = source
            if (isAttached) collect()
        }
        invalidateDraw()
    }

    override fun onAttach() = collect()

    // 목록에서 줄이 화면 밖으로 나가면 애니메이션이 도중에 끊긴다.
    // 다시 쓰일 때 줄어든 채로 나오지 않게 되돌린다.
    override fun onDetach() {
        pressed = Animatable(0f)
        shrink = Animatable(0f)
        hovered = false
        focused = false
    }

    private fun collect() {
        collecting?.cancel()
        collecting =
            coroutineScope.launch {
                val presses = mutableListOf<PressInteraction.Press>()
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> presses += interaction
                        is PressInteraction.Release -> presses -= interaction.press
                        is PressInteraction.Cancel -> presses -= interaction.press
                        is HoverInteraction.Enter -> hovered = true
                        is HoverInteraction.Exit -> hovered = false
                        is FocusInteraction.Focus -> focused = true
                        is FocusInteraction.Unfocus -> focused = false
                        else -> return@collect
                    }
                    if (presses.isNotEmpty()) press() else release()
                    invalidateDraw()
                }
            }
    }

    private fun press() {
        releasing?.cancel()
        pressing =
            coroutineScope.launch {
                launch { shrink.animateTo(1f, tween(PRESS_MILLIS)) }
                pressed.animateTo(1f, tween(PRESS_MILLIS)) { invalidateDraw() }
            }
    }

    private fun release() {
        val pressedJob = pressing
        releasing =
            coroutineScope.launch {
                // 톡 치고 바로 떼면 누른 모습이 한 번도 안 보인다.
                // 다 눌린 뒤에 돌아오게 한다.
                pressedJob?.join()
                launch { shrink.animateTo(0f, ReleaseBounce) { invalidateDraw() } }
                pressed.animateTo(0f, tween(RELEASE_MILLIS)) { invalidateDraw() }
            }
    }

    override fun ContentDrawScope.draw() {
        val amount = pressed.value
        val squeeze = shrink.value
        val hover = if (hovered) DiaryStateAlpha.HOVERED else 0f
        if (amount == 0f && squeeze == 0f && hover == 0f) {
            drawContent()
            if (focused) drawFocusRing()
            return
        }
        val ratio = min(MAX_SHRINK_RATIO, MaxShrink.toPx() / max(size.width, size.height))
        // 튕길 때는 squeeze가 잠깐 0 아래로 내려가 원래보다 아주 조금 커진다.
        scale(1f - ratio * squeeze, pivot = center) {
            this@draw.drawContent()
            if (focused) drawFocusRing()
            drawOutline(shape.createOutline(size, layoutDirection, this), color, alpha = max(hover, DiaryStateAlpha.PRESSED * amount))
        }
    }

    /** 하드웨어 키보드로 옮겨 다닐 때 지금 자리를 알린다. 터치에서는 포커스가 생기지 않아 보이지 않는다. */
    private fun DrawScope.drawFocusRing() {
        val gap = FocusGap.toPx()
        val outline = shape.createOutline(Size(size.width + gap * 2, size.height + gap * 2), layoutDirection, this)
        translate(-gap, -gap) { drawOutline(outline, color, style = Stroke(FocusWidth.toPx())) }
    }
}
