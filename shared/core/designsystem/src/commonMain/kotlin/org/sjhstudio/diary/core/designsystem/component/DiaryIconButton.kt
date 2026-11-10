package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 아이콘만 있는 버튼(`docs/design.md` 아이콘 버튼, 상단 바 아이콘 칸 48, M3 터치 48).
 * 원 48 · 아이콘 24 · 누름 효과(물결 없음).
 * 글이 없어 [contentDescription]을 꼭 받음.
 */
@Composable
fun DiaryIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = DiaryTheme.colors.textPrimary,
    iconSize: Dp = DiaryIconSize.i24,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(DiarySpacing.minTouchTarget)
                .alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = contentDescription, tint = tint, size = iconSize)
    }
}
