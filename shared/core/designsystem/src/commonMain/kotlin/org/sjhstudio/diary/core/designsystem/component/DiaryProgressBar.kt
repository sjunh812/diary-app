package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 진행 막대(`docs/design.md` 진행 막대, TDS ProgressBar bold 8). [progress]는 0~1. */
@Composable
fun DiaryProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(DiaryShapes.full)
                .background(DiaryTheme.colors.sunken)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }
    ) {
        Box(Modifier.fillMaxWidth(value).fillMaxHeight().clip(DiaryShapes.full).background(DiaryTheme.colors.accent))
    }
}

/** 끝을 모르는 진행 막대(사본을 만드는 동안). 채움 조각이 왼쪽에서 오른쪽으로 흐른다. */
@Composable
fun DiaryIndeterminateProgressBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = DiaryMotion.decelerate))
    )
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(8.dp).clip(DiaryShapes.full).background(DiaryTheme.colors.sunken)
    ) {
        Box(
            Modifier
                .offset { IntOffset((constraints.maxWidth * offset).toInt(), 0) }
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .clip(DiaryShapes.full)
                .background(DiaryTheme.colors.accent)
        )
    }
}
