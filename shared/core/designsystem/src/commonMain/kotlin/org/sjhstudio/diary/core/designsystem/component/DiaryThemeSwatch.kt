package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.limitFontScale

/**
 * 테마 색 견본 칸(`docs/design.md` 테마 화면)
 * 칸 70, 원 44, 고르면 2 띄운 고리 54와 체크. [color]가 null이면 아직 고르지 않은 "직접 고르기"(색상 고리와 +)
 * [plusLabel]이 있으면 원 오른쪽 위에 Plus 칩. 무료 사용자의 직접 고르기 칸에만
 * [label]은 화면 읽기 프로그램이 읽는 이름(색 이름, Plus 여부)
 */
@Composable
fun DiaryThemeSwatch(
    name: String,
    label: String,
    color: Color?,
    checkColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plusLabel: String? = null,
    enabled: Boolean = true
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .width(CELL)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .selectable(
                selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(RING), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(RING).border(RING_WIDTH, colors.textPrimary, DiaryShapes.full))
            if (color == null) {
                EmptyCustom()
            } else {
                Box(Modifier.size(CIRCLE).background(color, DiaryShapes.full), contentAlignment = Alignment.Center) {
                    if (selected) DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = checkColor)
                }
            }
            if (plusLabel != null) {
                DiaryPlusChip(
                    plusLabel,
                    Modifier.align(Alignment.TopStart).offset(x = CHIP_X, y = CHIP_Y),
                    separator = DiaryTheme.colors.canvas
                )
            }
        }
        Spacer(Modifier.height(DiarySpacing.x1_5))
        // 칸 폭이 정해져 있어 탭 이름처럼 글자 크기는 1.3배까지만. 더 키우면 두 줄에도 이름 끝이 잘림
        PhraseText(
            name,
            style =
                (if (selected) DiaryTheme.typography.captionStrong else DiaryTheme.typography.caption).limitFontScale().copy(
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center
                ),
            maxLines = 2
        )
    }
}

/** 아직 고르지 않은 직접 고르기: 색상 고리 안에 + */
@Composable
private fun EmptyCustom() {
    val plus = DiaryTheme.colors.textTertiary
    Canvas(Modifier.size(CIRCLE)) {
        val stroke = RAINBOW_WIDTH.toPx()
        drawCircle(Brush.sweepGradient(RAINBOW), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        val arm = PLUS_ARM.toPx()
        val line = PLUS_WIDTH.toPx()
        drawLine(plus, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), line, StrokeCap.Round)
        drawLine(plus, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), line, StrokeCap.Round)
    }
}

private val CELL = 70.dp
private val CIRCLE = 44.dp
private val RING = 54.dp
private val RING_WIDTH = 2.dp
private val RAINBOW_WIDTH = 3.dp
private val PLUS_ARM = 8.dp
private val PLUS_WIDTH = DiarySpacing.x0_5

// 원(고리 칸 안 5, 5) 왼쪽에서 25, 위에서 -4. 테두리 포함 34×18
private val CHIP_X = 30.dp
private val CHIP_Y = 1.dp

private val RAINBOW =
    listOf(
        Color(0xFFFF5A5A),
        Color(0xFFFFC84B),
        Color(0xFF8DD63A),
        Color(0xFF30C46C),
        Color(0xFF12C4B4),
        Color(0xFF12A5E4),
        Color(0xFF3C60D9),
        Color(0xFF8E4EC6),
        Color(0xFFD6409F),
        Color(0xFFFF5A5A)
    )
