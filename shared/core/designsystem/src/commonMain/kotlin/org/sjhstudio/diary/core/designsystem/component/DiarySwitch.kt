package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 스위치 모양(`docs/design.md` 스위치, SEED switchmark 32).
 * 트랙 52×32, 안쪽 3, 손잡이 26. 누르는 곳은 줄 전체라 이 모양은 누르기를 받지 않는다([DiarySettingsSwitchRow]).
 * 꺼진 트랙은 비텍스트 대비 3:1을 넘는 textTertiary다.
 */
@Composable
fun DiarySwitchMark(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = DiaryTheme.colors
    val knobOffset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(DiaryMotion.QUICK_MILLIS, easing = DiaryMotion.decelerate))
    Box(
        modifier =
            modifier
                .size(width = 52.dp, height = 32.dp)
                .alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                .clip(DiaryShapes.full)
                .background(if (checked) colors.accent else colors.textTertiary)
                .padding(3.dp)
    ) {
        Box(Modifier.offset(x = knobOffset).size(26.dp).clip(DiaryShapes.full).background(colors.surface))
    }
}
