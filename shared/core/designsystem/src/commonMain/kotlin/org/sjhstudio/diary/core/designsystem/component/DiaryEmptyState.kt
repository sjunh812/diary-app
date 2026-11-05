package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 빈 상태(`docs/design.md` 빈 상태, SEED Result Section, 토스 UX 가이드).
 * 아이콘 하나 · 제목 title2 · 설명 body · 버튼 하나. 문구는 긍정형 해요체로 쓴다.
 */
@Composable
fun DiaryEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryIcon(icon, contentDescription = null, tint = colors.textTertiary, size = DiaryIconSize.i32)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(text = title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        if (description != null) {
            BasicText(
                text = description,
                style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(DiarySpacing.x2))
            DiaryButton(text = actionLabel, onClick = onAction, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Weak)
        }
    }
}

/**
 * 빈 상태를 화면 칸에 놓는다. 눈에는 정가운데가 처져 보여서 위 2, 아래 3으로 조금 올린다(Figma "S1 · 빈 상태" 자리).
 * 위아래 칸이 없으면 줄어들어 큰 글씨에서도 잘리지 않는다.
 */
@Composable
fun DiaryEmptyArea(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(2f))
        content()
        Spacer(Modifier.weight(3f))
    }
}
