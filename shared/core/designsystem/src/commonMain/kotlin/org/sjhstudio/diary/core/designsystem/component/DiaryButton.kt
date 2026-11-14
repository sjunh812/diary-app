package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
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

/** 버튼 크기 다섯(`docs/design.md` 컴포넌트 규격, TDS 버튼 xlarge·large·medium·small) */
enum class DiaryButtonSize {
    /** 56 · 좌우 16 · 모서리 16 · headline */
    XLarge,

    /** 48 · 좌우 16 · 모서리 14 · headline */
    Large,

    /** 38 · 좌우 16 · 모서리 10 · bodyStrong */
    Medium,

    /** 32 · 좌우 10 · 모서리 8 · captionStrong */
    Small,

    /** 32 · 좌우 10 · 모서리 8 · label. 위쪽 줄에 홀로 놓이는 글자 버튼용 */
    Header
}

/** 버튼 종류(TDS 버튼 색 종류). 한 화면에 [Primary]는 하나만 */
enum class DiaryButtonKind { Primary, Secondary, Weak, Text, QuietText, Danger }

private class ButtonSpec(val height: Dp, val horizontal: Dp, val shape: Shape)

/** 버튼 글의 줄 수 끝 */
private const val BUTTON_MAX_LINES = 2

private fun DiaryButtonSize.spec() = when (this) {
    DiaryButtonSize.XLarge -> ButtonSpec(56.dp, DiarySpacing.x4, DiaryShapes.r16)
    DiaryButtonSize.Large -> ButtonSpec(48.dp, DiarySpacing.x4, DiaryShapes.r14)
    DiaryButtonSize.Medium -> ButtonSpec(38.dp, DiarySpacing.x4, DiaryShapes.r10)
    DiaryButtonSize.Small, DiaryButtonSize.Header -> ButtonSpec(32.dp, 10.dp, DiaryShapes.r8)
}

/**
 * 버튼 하나. 글은 문자열 리소스에서 받음
 * 보이는 높이가 48보다 작아도 누르는 영역은 48.
 * 자리는 보이는 크기만 차지해 옆 내용과의 간격은 규격 그대로
 * 높이는 최소 규격이라 큰 글자로 두 줄이 되면 늘어남. 잘리지 않게 위아래 4.
 */
@Composable
fun DiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DiaryButtonSize = DiaryButtonSize.Large,
    kind: DiaryButtonKind = DiaryButtonKind.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    /** 누른 일이 끝나기를 기다리는 중. 글 자리는 그대로 두고 점을 보이며 다시 누를 수 없음(TDS·SEED Button loading) */
    loading: Boolean = false
) {
    val colors = DiaryTheme.colors
    val (container, content) =
        when (kind) {
            DiaryButtonKind.Primary -> colors.accent to colors.onAccent
            DiaryButtonKind.Secondary -> colors.sunken to colors.textPrimary
            DiaryButtonKind.Weak -> colors.selected to colors.accentText
            DiaryButtonKind.Text -> Color.Transparent to colors.accentText
            DiaryButtonKind.QuietText -> Color.Transparent to colors.textSecondary
            DiaryButtonKind.Danger -> colors.danger to colors.onDanger
        }
    val style: TextStyle =
        when (size) {
            DiaryButtonSize.XLarge, DiaryButtonSize.Large -> DiaryTheme.typography.headline
            DiaryButtonSize.Medium -> DiaryTheme.typography.bodyStrong
            DiaryButtonSize.Small -> DiaryTheme.typography.captionStrong
            DiaryButtonSize.Header -> DiaryTheme.typography.label
        }
    val spec = size.spec()
    val interaction = remember { MutableInteractionSource() }
    val touch = ((DiarySpacing.minTouchTarget - spec.height) / 2).coerceAtLeast(Dp.Hairline)
    // 바깥 칸 48이 누르기를 받고 안쪽 칸이 보이는 버튼을 그림
    Box(
        modifier
            .widthIn(min = DiarySpacing.minTouchTarget)
            .diaryTouchOutset(vertical = touch)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = touch),
        propagateMinConstraints = true
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = spec.height)
                    .alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                    .diaryPress(interaction, colors.textPrimary, spec.shape)
                    .clip(spec.shape)
                    .background(container)
                    .padding(horizontal = spec.horizontal, vertical = DiarySpacing.x1),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    Modifier.alpha(if (loading) 0f else 1f),
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
                if (loading) DiaryLoadingDots(content)
            }
        }
    }
}

/**
 * 나란한 두 버튼. 폭은 늘 같고 주 버튼이 오른쪽
 * 한쪽 글이라도 반 칸에 안 들어가면 자르지 않고 위아래로 쌓아 주 버튼을 위에 둠
 * 언어·글자 크기마다 글 길이가 달라 넓이를 재서 정함(M3 대화상자의 쌓인 버튼과 같음)
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
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false
) {
    Layout(
        content = {
            DiaryButton(secondary, onSecondary, Modifier.fillMaxWidth(), size = size, kind = secondaryKind)
            DiaryButton(
                primary,
                onPrimary,
                Modifier.fillMaxWidth(),
                size = size,
                kind = primaryKind,
                enabled = primaryEnabled,
                loading = primaryLoading
            )
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
