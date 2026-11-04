package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 쓰기 버튼(`docs/design.md` 쓰기 버튼, M3 FAB). 56 · 모서리 16 · 화면 끝 20 · 탭 위 16. */
@Composable
fun DiaryWriteButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(DiarySpacing.x14)
                .diaryPress(interaction, colors.onAccent, DiaryShapes.r16)
                .clip(DiaryShapes.r16)
                .background(colors.accent)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(DiaryIcons.Pen, contentDescription = contentDescription, tint = colors.onAccent)
    }
}
