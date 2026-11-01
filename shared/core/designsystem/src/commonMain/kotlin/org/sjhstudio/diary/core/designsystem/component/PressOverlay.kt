package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha

/**
 * 누르면 내용 위에 [color]를 10%, 가리키면 8% 덮는다(`docs/design.md` 상태, M3 state layer).
 * 물결(ripple) 대신 이것을 쓴다. 모양은 앞에서 `clip`으로 자른다.
 */
internal data class PressOverlay(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressOverlayNode(interactionSource, color)
}

private class PressOverlayNode(private val interactionSource: InteractionSource, private val color: Color) :
    Modifier.Node(),
    DrawModifierNode {
    private var pressed = false
    private var hovered = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
                    is HoverInteraction.Enter -> hovered = true
                    is HoverInteraction.Exit -> hovered = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha =
            when {
                pressed -> DiaryStateAlpha.PRESSED
                hovered -> DiaryStateAlpha.HOVERED
                else -> 0f
            }
        if (alpha > 0f) drawRect(color.copy(alpha = alpha))
    }
}
