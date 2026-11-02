package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 버튼 크기 넷(`docs/design.md` 컴포넌트 규격, TDS 버튼 xlarge·large·medium·small). */
enum class DiaryButtonSize {
    /** 56 · 좌우 28 · 모서리 16 · headline */
    XLarge,

    /** 48 · 좌우 16 · 모서리 14 · headline */
    Large,

    /** 38 · 좌우 16 · 모서리 10 · bodyStrong */
    Medium,

    /** 32 · 좌우 10 · 모서리 8 · captionStrong */
    Small
}

/** 버튼 종류(TDS 버튼 색 종류). 한 화면에 [Primary]는 하나만 둔다. */
enum class DiaryButtonKind { Primary, Secondary, Weak, Text, Danger }

private class ButtonSpec(val height: Dp, val horizontal: Dp, val shape: Shape)

private fun DiaryButtonSize.spec() = when (this) {
    DiaryButtonSize.XLarge -> ButtonSpec(56.dp, 28.dp, DiaryShapes.r16)
    DiaryButtonSize.Large -> ButtonSpec(48.dp, DiarySpacing.x4, DiaryShapes.r14)
    DiaryButtonSize.Medium -> ButtonSpec(38.dp, DiarySpacing.x4, DiaryShapes.r10)
    DiaryButtonSize.Small -> ButtonSpec(32.dp, 10.dp, DiaryShapes.r8)
}

/**
 * 버튼 하나. 글은 문자열 리소스에서 받아 넘긴다.
 * 보이는 높이가 48보다 작아도 누르는 영역은 48이다.
 */
@Composable
fun DiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DiaryButtonSize = DiaryButtonSize.Large,
    kind: DiaryButtonKind = DiaryButtonKind.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = DiaryTheme.colors
    val (container, content) =
        when (kind) {
            DiaryButtonKind.Primary -> colors.accent to colors.onAccent
            DiaryButtonKind.Secondary -> colors.sunken to colors.textPrimary
            DiaryButtonKind.Weak -> colors.selected to colors.accentText
            DiaryButtonKind.Text -> Color.Transparent to colors.accentText
            DiaryButtonKind.Danger -> colors.danger to colors.onDanger
        }
    val style: TextStyle =
        when (size) {
            DiaryButtonSize.XLarge, DiaryButtonSize.Large -> DiaryTheme.typography.headline
            DiaryButtonSize.Medium -> DiaryTheme.typography.bodyStrong
            DiaryButtonSize.Small -> DiaryTheme.typography.captionStrong
        }
    val spec = size.spec()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .height(spec.height)
                .alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                .diaryPress(interaction, colors.textPrimary, spec.shape)
                .clip(spec.shape)
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ).padding(horizontal = spec.horizontal),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            DiaryIcon(
                leadingIcon,
                contentDescription = null,
                tint = content,
                size = if (size == DiaryButtonSize.Small) DiaryIconSize.i16 else DiaryIconSize.i20
            )
        }
        BasicText(text = text, style = style.copy(color = content), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
