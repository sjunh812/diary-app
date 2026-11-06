package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing

private const val DOTS = 3
private const val DIM = 0.3f
private const val FADE_MILLIS = 450
private const val STEP_MILLIS = 150

/**
 * 짧은 기다림을 알리는 점 세 개(`docs/design.md` 칩). 글로 이미 말했으니 화면 읽기에는 읽히지 않는다.
 * 동작 줄이기에서는 [animated]를 끄면 점이 그대로 있다.
 */
@Composable
fun DiaryLoadingDots(color: Color, modifier: Modifier = Modifier, animated: Boolean = true) {
    val transition = rememberInfiniteTransition()
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
        repeat(DOTS) { index ->
            val light =
                transition.animateFloat(
                    DIM,
                    1f,
                    infiniteRepeatable(tween(FADE_MILLIS), RepeatMode.Reverse, StartOffset(index * STEP_MILLIS))
                )
            Box(
                Modifier
                    .size(DiarySpacing.x1)
                    .graphicsLayer { alpha = if (animated) light.value else 1f }
                    .background(color, DiaryShapes.full)
            )
        }
    }
}
