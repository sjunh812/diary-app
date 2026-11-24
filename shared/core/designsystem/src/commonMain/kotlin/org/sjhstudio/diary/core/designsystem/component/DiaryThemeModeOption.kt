package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 화면 모드 고르기 칸(`docs/design.md` 테마 화면)
 * 작은 홈 그림 96×192가 곧 고르는 칸이고, 그림 아래 12에 동그라미 24와 이름
 * 그림은 [picture] 색(그 모드의 테마 색)으로 그리고 색이 바뀌면 부드럽게 바뀜
 * 고르면 그림에 3 띄운 고리 2, 동그라미에 체크, 이름은 굵게
 */
@Composable
fun DiaryThemeModeOption(
    name: String,
    picture: DiaryColors,
    layout: MiniHomeLayout,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val ring = colors.accentIcon
    Column(
        modifier
            .selectable(selected, role = Role.RadioButton, interactionSource = interaction, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MiniHome(
            picture,
            layout,
            Modifier.drawBehind {
                if (!selected) return@drawBehind
                val inset = (RING_GAP + RING_WIDTH / 2).toPx()
                drawRoundRect(
                    ring,
                    topLeft = Offset(-inset, -inset),
                    size = Size(size.width + inset * 2, size.height + inset * 2),
                    cornerRadius = CornerRadius((PICTURE_RADIUS + RING_GAP + RING_WIDTH / 2).toPx()),
                    style = Stroke(RING_WIDTH.toPx())
                )
            }
        )
        Spacer(Modifier.height(DiarySpacing.x3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioMark(selected)
            Spacer(Modifier.width(DiarySpacing.x2))
            BasicText(
                name,
                style = (if (selected) DiaryTheme.typography.bodyStrong else DiaryTheme.typography.body).copy(color = colors.textPrimary)
            )
        }
    }
}

/** 고르기 동그라미 24. 고르면 브랜드색 원에 체크 */
@Composable
private fun RadioMark(selected: Boolean) {
    val colors = DiaryTheme.colors
    val modifier = Modifier.size(DiaryIconSize.i24)
    if (selected) {
        Box(modifier.background(colors.accent, DiaryShapes.full), contentAlignment = Alignment.Center) {
            DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.onAccent, size = DiaryIconSize.i16)
        }
    } else {
        Box(modifier.border(RING_WIDTH, colors.lineStrong, DiaryShapes.full))
    }
}

/**
 * 우리 홈을 줄인 막대·상자 그림
 * 사진 칸은 회색 하나로 두고 고른 칩·별·쓰기 버튼만 테마 색(스펙 010 FR-014)
 */
@Composable
private fun MiniHome(c: DiaryColors, layout: MiniHomeLayout, modifier: Modifier = Modifier) {
    val accent by animated(c.accent)
    val star by animated(c.accentIcon)
    val onAccent by animated(c.onAccent)
    val selected by animated(c.selected)
    val accentText by animated(c.accentText)
    val card by animated(c.surfaceCard)
    val canvas by animated(c.canvas)
    val ink by animated(c.textPrimary)
    val soft by animated(c.lineStrong)
    val chip by animated(c.sunken)
    val frame by animated(c.line)
    val pen = rememberVectorPainter(DiaryIcons.Pen)
    Canvas(modifier.size(PICTURE_WIDTH, PICTURE_HEIGHT)) {
        val u = 1.dp.toPx()
        val radius = CornerRadius(PICTURE_RADIUS.toPx())
        drawRoundRect(canvas, cornerRadius = radius)
        drawRoundRect(frame, cornerRadius = radius, style = Stroke(u))
        bar(ink, 10f, 16f, 30f, 7f, u)
        bar(selected, 10f, 30f, 22f, 10f, u)
        bar(accentText, 15f, 34f, 12f, 2f, u)
        bar(chip, 35f, 30f, 20f, 10f, u)
        when (layout) {
            MiniHomeLayout.Feed -> {
                listOf(38f to 24f, 30f to 20f, 40f to 28f).forEachIndexed { i, (first, second) ->
                    val y = 52f + i * 26f
                    box(soft, 10f, y, 7f, 9f, 2f, u)
                    bar(ink, 21f, y, first, 4f, u)
                    bar(soft, 21f, y + 7f, second, 3f, u)
                }
                drawCircle(star, radius = 2.5f * u, center = Offset(83f * u, 54f * u))
                box(soft, 72f, 75f, 14f, 14f, 3f, u)
            }

            MiniHomeLayout.Card -> {
                for (i in 0 until 3) {
                    val y = 48f + i * 34f
                    box(card, 8f, y, 80f, 30f, 6f, u)
                    bar(ink, 14f, y + 7f, 34f, 4f, u)
                    bar(soft, 14f, y + 15f, if (i == 1) 44f else 54f, 3f, u)
                    bar(soft, 14f, y + 21f, 30f, 3f, u)
                }
                drawCircle(star, radius = 2.5f * u, center = Offset(80f * u, 57f * u))
                box(soft, 66f, 88f, 16f, 16f, 3f, u)
            }

            // 가운데 칸만 글 칸, 나머지는 사진
            MiniHomeLayout.Grid -> {
                for (i in 0 until 9) box(if (i == 4) canvas else soft, 9.5f + i % 3 * 27f, 48f + i / 3 * 27f, 25f, 25f, 2f, u)
                bar(ink, 39f, 85f, 16f, 3f, u)
                bar(soft, 39f, 91f, 12f, 2f, u)
            }
        }
        drawCircle(accent, radius = 10f * u, center = Offset(76f * u, 158f * u))
        translate(left = 71f * u, top = 153f * u) {
            with(pen) { draw(Size(10f * u, 10f * u), colorFilter = ColorFilter.tint(onAccent)) }
        }
        for (i in 0 until 4) drawCircle(if (i == 0) ink else soft, radius = 1.5f * u, center = Offset((30f + i * 12f) * u, 180f * u))
    }
}

@Composable
private fun animated(color: Color): State<Color> =
    animateColorAsState(color, tween(DiaryMotion.STANDARD_MILLIS, easing = DiaryMotion.decelerate))

private fun DrawScope.bar(color: Color, x: Float, y: Float, w: Float, h: Float, u: Float) =
    drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(h / 2 * u))

private fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float, u: Float) =
    drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(r * u))

private val PICTURE_WIDTH = 96.dp
private val PICTURE_HEIGHT = 192.dp
private val PICTURE_RADIUS = 16.dp
private val RING_GAP = 3.dp
private val RING_WIDTH = 2.dp
