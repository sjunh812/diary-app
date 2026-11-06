package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import kotlin.math.cos
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_arrived
import org.sjhstudio.diary.feature.welcome.resources.letter_to

private const val ARRIVE_MILLIS = 450

/** 봉투 비율. 편지 봉투 C6(162×114)에 가깝다. */
private const val ENVELOPE_RATIO = 1.42f

/** 덮개가 내려오는 깊이와 앞면이 파인 깊이. 봉투 높이에 대한 비율이다. */
private const val FLAP_DEPTH = 0.58f
private const val POCKET_DEPTH = 0.5f

/** 열리는 진행 값에서 덮개가 끝나는 때, 편지지가 다 올라온 때, 봉투가 사라지기 시작하는 때. */
private const val FLAP_END = 0.35f
private const val RISE_END = 0.8f
private const val FADE_START = 0.75f

/** 편지지가 봉투 높이의 몇 배만큼 올라오는가. */
private const val PAPER_RISE = 0.62f

/** 머리 글은 편지지가 올라올 자리를 비켜 준다. */
private const val HEAD_FADE_END = 0.25f

/** 편지지에 비치는 글 세 줄의 폭. */
private val PAPER_LINES = listOf(0.82f, 0.9f, 0.56f)

/**
 * 도착한 봉투(Figma W0). 누르면 덮개가 열리고 편지지가 올라온다.
 * [progress]는 0에서 1까지 열린 정도다. 값을 그리는 단계에서 읽어 화면을 다시 짜지 않는다.
 */
@Composable
internal fun LetterEnvelope(progress: () -> Float, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val arrive = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) { if (!still) arrive.animateTo(1f, tween(ARRIVE_MILLIS)) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = DiarySpacing.screenCompact)
            .graphicsLayer {
                val opened = ((1f - progress()) / (1f - FADE_START)).coerceAtMost(1f)
                alpha = arrive.value * opened
                translationY = (1f - arrive.value) * DiarySpacing.x6.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            Modifier.graphicsLayer { alpha = 1f - (progress() / HEAD_FADE_END).coerceAtMost(1f) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                stringResource(Res.string.letter_arrived),
                Modifier.semantics { heading() },
                style = typography.title1.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(DiarySpacing.x2))
            BasicText(
                stringResource(Res.string.letter_to),
                style = typography.label.copy(color = colors.accentText, textAlign = TextAlign.Center)
            )
        }
        Spacer(Modifier.height(DiarySpacing.x10))
        val pen = rememberVectorPainter(DiaryIcons.Pen)
        Canvas(
            Modifier
                .widthIn(max = DiaryArtSize.envelope)
                .fillMaxWidth()
                .aspectRatio(ENVELOPE_RATIO)
                .clickable(interactionSource = null, indication = null, onClick = onOpen)
                .clearAndSetSemantics { }
        ) { drawEnvelope(progress(), colors, pen) }
    }
}

/** 앞에서부터 쌓는다. 덮개는 열리기 전에는 맨 앞, 다 넘어간 뒤에는 편지지 뒤에 있다. */
private fun DrawScope.drawEnvelope(progress: Float, colors: DiaryColors, pen: VectorPainter) {
    val width = size.width
    val height = size.height
    val line = Stroke(DiarySpacing.hairline.toPx(), join = StrokeJoin.Round)
    val body = Path().apply { addRoundRect(RoundRect(0f, 0f, width, height, CornerRadius(DiarySpacing.x4.toPx()))) }
    val flapTip = FLAP_DEPTH * height * cos((progress / FLAP_END).coerceAtMost(1f) * PI.toFloat())
    val flap = Path().apply {
        moveTo(0f, 0f)
        lineTo(width / 2, flapTip)
        lineTo(width, 0f)
    }

    drawPath(body, colors.sunken)
    if (flapTip < 0) {
        drawPath(flap, colors.selected)
        drawPath(flap, colors.lineStrong, style = line)
    }
    drawPaper(((progress - FLAP_END) / (RISE_END - FLAP_END)).coerceIn(0f, 1f), colors)
    clipPath(body) {
        val pocket = Path().apply {
            moveTo(0f, 0f)
            lineTo(width / 2, POCKET_DEPTH * height)
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(pocket, colors.surface)
        val folds = Path().apply {
            moveTo(0f, 0f)
            lineTo(width / 2, POCKET_DEPTH * height)
            lineTo(width, 0f)
            moveTo(0f, height)
            lineTo(width / 2, POCKET_DEPTH * height)
            lineTo(width, height)
        }
        drawPath(folds, colors.lineStrong, style = line)
    }
    drawPath(body, colors.lineStrong, style = line)
    if (flapTip >= 0) {
        clipPath(body) { drawPath(flap, colors.selected) }
        drawPath(flap, colors.lineStrong, style = line)
        drawSeal(Offset(width / 2, flapTip), (flapTip / (FLAP_DEPTH * height)), colors, pen)
    }
}

/** 봉투 안의 편지지. 글은 읽을 수 없게 줄로만 비친다. */
private fun DrawScope.drawPaper(rise: Float, colors: DiaryColors) {
    val inset = DiarySpacing.x4.toPx()
    val top = DiarySpacing.x3.toPx() - rise * size.height * PAPER_RISE
    val paper = Size(size.width - inset * 2, size.height - inset)
    drawRoundRect(colors.surface, Offset(inset, top), paper, CornerRadius(DiarySpacing.x2.toPx()))
    PAPER_LINES.forEachIndexed { index, length ->
        val y = top + DiarySpacing.x6.toPx() + index * DiarySpacing.x4.toPx()
        val left = inset * 2
        drawLine(
            colors.line,
            Offset(left, y),
            Offset(left + (paper.width - inset * 2) * length, y),
            strokeWidth = DiarySpacing.x0_5.toPx()
        )
    }
}

/** 봉인. 덮개 끝을 따라 움직이고 덮개가 넘어갈수록 옅어진다. */
private fun DrawScope.drawSeal(center: Offset, alpha: Float, colors: DiaryColors, pen: VectorPainter) {
    val radius = DiarySpacing.x5.toPx()
    val icon = DiarySpacing.x5.toPx()
    drawCircle(colors.accent, radius, center, alpha)
    translate(center.x - icon / 2, center.y - icon / 2) {
        with(pen) { draw(Size(icon, icon), alpha, ColorFilter.tint(colors.onAccent)) }
    }
}
