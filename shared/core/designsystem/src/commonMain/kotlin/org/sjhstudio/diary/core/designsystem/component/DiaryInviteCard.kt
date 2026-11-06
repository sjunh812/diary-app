package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * Plus로 가는 입구(`docs/design.md` 초대 카드). 카드 전체를 누른다.
 * 바탕 selected 40%(글이 묻히지 않게 편지 바탕과 같은 농도로 둔다) · 모서리 16 · 안쪽 20 · 제목 bodyStrong · 설명 caption · 동작 captionStrong accentText.
 */
@Composable
fun DiaryInviteCard(title: String, body: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val press = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .diaryPress(press, colors.textPrimary, DiaryShapes.r16)
            .clip(DiaryShapes.r16)
            .background(colors.canvas)
            .background(colors.selected.copy(alpha = TINT))
            .clickable(interactionSource = press, indication = null, role = Role.Button, onClick = onClick)
            .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(title, style = typography.bodyStrong.copy(color = colors.textPrimary))
        BasicText(body, style = typography.caption.copy(color = colors.textSecondary))
        BasicText(action, Modifier.padding(top = DiarySpacing.x2), style = typography.captionStrong.copy(color = colors.accentText))
    }
}

private const val TINT = 0.4f
