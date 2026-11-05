package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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

/** 버튼 글의 줄 수 끝. */
private const val BUTTON_MAX_LINES = 2

private fun DiaryButtonSize.spec() = when (this) {
    DiaryButtonSize.XLarge -> ButtonSpec(56.dp, DiarySpacing.x4, DiaryShapes.r16)
    DiaryButtonSize.Large -> ButtonSpec(48.dp, DiarySpacing.x4, DiaryShapes.r14)
    DiaryButtonSize.Medium -> ButtonSpec(38.dp, DiarySpacing.x4, DiaryShapes.r10)
    DiaryButtonSize.Small -> ButtonSpec(32.dp, 10.dp, DiaryShapes.r8)
}

/**
 * 버튼 하나. 글은 문자열 리소스에서 받아 넘긴다.
 * 보이는 높이가 48보다 작아도 누르는 영역은 48이다.
 * 높이는 최소 규격이라 큰 글자에서 글이 두 줄이 되면 버튼이 늘어난다. 잘리지 않게 위아래 4를 둔다.
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
                .heightIn(min = spec.height)
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
                ).padding(horizontal = spec.horizontal, vertical = DiarySpacing.x1),
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
        BasicText(
            text = text,
            style = style.copy(color = content, textAlign = TextAlign.Center),
            maxLines = BUTTON_MAX_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 나란한 두 버튼. 폭은 늘 같고 주 버튼이 오른쪽이다.
 * 어느 한쪽 글이 반 칸에 들어가지 않으면 잘리는 대신 위아래로 쌓고 주 버튼을 위에 둔다.
 * 언어와 글자 크기에 따라 글 길이가 달라져서 넓이를 재고 정한다(M3 대화상자의 쌓인 버튼과 같다).
 */
@Composable
fun DiaryButtonPair(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    size: DiaryButtonSize = DiaryButtonSize.Large,
    secondaryKind: DiaryButtonKind = DiaryButtonKind.Secondary,
    primaryKind: DiaryButtonKind = DiaryButtonKind.Primary,
    primaryEnabled: Boolean = true
) {
    Layout(
        content = {
            DiaryButton(secondary, onSecondary, Modifier.fillMaxWidth(), size = size, kind = secondaryKind)
            DiaryButton(primary, onPrimary, Modifier.fillMaxWidth(), size = size, kind = primaryKind, enabled = primaryEnabled)
        },
        modifier = modifier.padding(top = LocalSheetButtonGap.current).fillMaxWidth()
    ) { (second, first), constraints ->
        val gap = DiarySpacing.x2.roundToPx()
        val half = (constraints.maxWidth - gap) / 2
        val sideBySide = maxOf(second.maxIntrinsicWidth(Constraints.Infinity), first.maxIntrinsicWidth(Constraints.Infinity)) <= half
        val width = if (sideBySide) half else constraints.maxWidth
        val fixed = Constraints.fixedWidth(width)
        val left = second.measure(fixed)
        val right = first.measure(fixed)
        if (sideBySide) {
            layout(constraints.maxWidth, maxOf(left.height, right.height)) {
                left.placeRelative(0, 0)
                right.placeRelative(constraints.maxWidth - width, 0)
            }
        } else {
            layout(constraints.maxWidth, right.height + gap + left.height) {
                right.placeRelative(0, 0)
                left.placeRelative(0, right.height + gap)
            }
        }
    }
}
