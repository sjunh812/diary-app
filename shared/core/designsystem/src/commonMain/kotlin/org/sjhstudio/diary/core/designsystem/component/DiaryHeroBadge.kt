package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

enum class DiaryBadgeTone { Accent, Danger }

/**
 * 결과·진행 화면 위의 큰 배지(Figma badge).
 * 원 56 · 그림 32.
 */
@Composable
fun DiaryHeroBadge(icon: ImageVector, modifier: Modifier = Modifier, tone: DiaryBadgeTone = DiaryBadgeTone.Accent) {
    val colors = DiaryTheme.colors
    Box(
        modifier = modifier.size(56.dp).clip(DiaryShapes.full).background(
            if (tone ==
                DiaryBadgeTone.Accent
            ) {
                colors.selected
            } else {
                colors.sunken
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(
            icon,
            contentDescription = null,
            tint = if (tone ==
                DiaryBadgeTone.Accent
            ) {
                colors.accentText
            } else {
                colors.danger
            },
            size = DiaryIconSize.i32
        )
    }
}
