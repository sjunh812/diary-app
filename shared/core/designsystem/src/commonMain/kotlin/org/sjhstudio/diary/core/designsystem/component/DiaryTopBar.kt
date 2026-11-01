package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_back
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 상단 바 오른쪽 글자 동작(저장 같은). 채운 버튼이 아니라 글자 버튼이다. */
class DiaryTopBarAction(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

/**
 * 상단 바(`docs/design.md` 컴포넌트 규격, SEED TopNavigation 56).
 * 높이 56, 가운데 제목 headline, 왼쪽 뒤로 가기 칸 48, 오른쪽은 글자 동작.
 * 앱 틀 맨 위 화면(설정 같은)은 상단 바 대신 큰 제목을 쓴다.
 */
@Composable
fun DiaryTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, action: DiaryTopBarAction? = null) {
    val colors = DiaryTheme.colors
    Box(modifier = modifier.fillMaxWidth().height(56.dp)) {
        if (onBack != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = DiarySpacing.x1)
                        .size(DiarySpacing.minTouchTarget)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = PressOverlay(colors.textPrimary),
                            role = Role.Button,
                            onClick = onBack
                        ),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.ChevronLeft, contentDescription = stringResource(Res.string.ds_back), tint = colors.textPrimary)
            }
        }
        BasicText(
            text = title,
            style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = DiarySpacing.x14).semantics { heading() }
        )
        if (action != null) {
            BasicText(
                text = action.label,
                style = DiaryTheme.typography.headline.copy(color = colors.accentText),
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = DiarySpacing.x2)
                        .alpha(if (action.enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = PressOverlay(colors.textPrimary),
                            enabled = action.enabled,
                            role = Role.Button,
                            onClick = action.onClick
                        ).padding(horizontal = DiarySpacing.x3, vertical = DiarySpacing.x3)
            )
        }
    }
}
