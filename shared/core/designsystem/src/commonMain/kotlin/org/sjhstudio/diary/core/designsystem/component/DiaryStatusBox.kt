package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 상태 상자(`docs/design.md` 상태 상자, Figma "permission-needed"). 안내 상자와 한 집안이다.
 * 바탕 sunken · 모서리 16 · 안쪽 20 · 머리와 설명 사이 4 · 버튼은 설명과 사이 16(`docs/design.md` 상자 여백).
 * 머리 줄은 아이콘 16 + 사이 8 + `bodyStrong`, 아래 설명 `body`, 있으면 작은 약한 버튼.
 */
@Composable
fun DiaryStatusBox(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DiaryShapes.r16)
                .background(colors.sunken)
                .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.Top) {
                FirstLineIcon(icon, typography.bodyStrong, colors.textPrimary)
                BasicText(title, style = typography.bodyStrong.copy(color = colors.textPrimary))
            }
            BasicText(body, style = typography.body.copy(color = colors.textSecondary))
        }
        if (actionLabel != null && onAction != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryButton(actionLabel, onClick = onAction, size = DiaryButtonSize.Small, kind = DiaryButtonKind.Weak)
                if (secondaryLabel != null && onSecondary != null) {
                    DiaryButton(secondaryLabel, onClick = onSecondary, size = DiaryButtonSize.Small, kind = DiaryButtonKind.Weak)
                }
            }
        }
    }
}
