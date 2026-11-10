package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * 아이콘은 첫 줄 가운데.
 * 화면 위쪽이나 목록 아래에서 지금 화면의 규칙을 짧게 알림.
 */
@Composable
fun DiaryNotice(text: String, modifier: Modifier = Modifier, icon: ImageVector = DiaryIcons.Info, failure: Boolean = false) {
    val colors = DiaryTheme.colors
    // 실패는 안내와 같은 상자에 아이콘과 글만 바꿔 묻히지 않게 함(경고 아이콘, 진한 글).
    // 색을 더하지 않고 강조는 한 곳에만
    val tint = if (failure) colors.danger else colors.textSecondary
    val ink = if (failure) colors.textPrimary else colors.textSecondary
    Row(
        modifier = modifier.fillMaxWidth().clip(DiaryShapes.r12).background(colors.sunken).padding(DiarySpacing.x4),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.Top
    ) {
        FirstLineIcon(if (failure) DiaryIcons.CircleAlert else icon, DiaryTheme.typography.caption, tint)
        PhraseText(text, style = DiaryTheme.typography.caption.copy(color = ink))
    }
}
