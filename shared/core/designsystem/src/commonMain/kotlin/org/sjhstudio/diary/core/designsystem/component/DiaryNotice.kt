package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 한 줄 안내(Figma 예전 일기 보기·잠금 설정의 안내, `docs/design.md` 한 줄 안내).
 * 바탕 sunken · 모서리 12 · 안쪽 16 · 아이콘 16과 글 사이 8 · `caption`.
 * 아이콘은 첫 줄 가운데에 둔다.
 * 화면 위쪽이나 목록 아래에서 지금 화면의 규칙을 짧게 알린다.
 */
@Composable
fun DiaryNotice(text: String, modifier: Modifier = Modifier, icon: ImageVector = DiaryIcons.Info) {
    val colors = DiaryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().clip(DiaryShapes.r12).background(colors.sunken).padding(DiarySpacing.x4),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.Top
    ) {
        FirstLineIcon(icon, DiaryTheme.typography.caption, colors.textSecondary)
        BasicText(text, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
    }
}
