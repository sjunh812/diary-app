package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
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

/**
 * 열리는 진행 값의 구간. 덮개가 넘어가고 잠깐 뒤에 편지지가 천천히 올라온다.
 * 편지지는 다 올라오기 전부터 펼쳐져서 멈춘 데 없이 이어진다.
 */
private const val FLAP_END = 0.28f
private const val RISE_START = 0.34f
private const val RISE_END = 0.74f
private const val UNFOLD_START = 0.62f

/** 편지지가 반쯤 나오면 글자가 드러난다. */
private const val BLANK_END = 0.56f

/** 편지지가 봉투 높이의 얼마만큼 올라오는가. */
private const val RISE_SHARE = 0.55f

/** 머리 글은 편지지가 올라올 자리를 비켜 준다. */
private const val HEAD_FADE_END = 0.2f

/** 봉투는 내려가면서 끝 무렵에 사라진다. */
private const val FADE_START = 0.85f

private val Float.halfTurn get() = this * PI.toFloat()

/** 진행 값 한 구간을 0에서 1로 펴고 시작과 끝을 부드럽게 한다. */
private fun Float.between(start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((this - start) / (end - start)).coerceIn(0f, 1f))

/**
 * 도착한 봉투(Figma W0). 덮개가 열리면 진짜 편지지가 줄어든 채 봉투에서 올라오고, 봉투가 내려가며 제자리로 펼쳐진다.
 * 뒷면, 편지지, 앞면을 겹쳐 그려서 편지지가 앞면 뒤에서 나온다.
 * [progress]는 0에서 1까지 열린 정도다. 값을 그리는 단계에서 읽어 화면을 다시 짜지 않는다.
 * [letter]는 받은 수식어를 편지지 카드에 달아야 한다.
 */
@Composable
internal fun LetterStage(progress: () -> Float, onOpen: () -> Unit, modifier: Modifier = Modifier, letter: @Composable (Modifier) -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val arrive = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) { if (!still) arrive.animateTo(1f, tween(ARRIVE_MILLIS)) }
    val pen = rememberVectorPainter(DiaryIcons.Pen)
    var stage by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var envelope by remember { mutableStateOf(Rect.Zero) }
    var card by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier
            .fillMaxWidth()
            .onPlaced { stage = it }
            .graphicsLayer {
                alpha = arrive.value
                translationY = (1f - arrive.value) * DiarySpacing.x6.toPx()
            }
    ) {
        val fade = Modifier.matchParentSize().graphicsLayer { alpha = 1f - progress().between(FADE_START, 1f) }
        Canvas(fade) {
            translate(envelope.left, envelope.top + drop(progress(), envelope)) { drawBack(envelope.size, progress(), colors) }
        }
        Box(
            Modifier
                .matchParentSize()
                .clearAndSetSemantics { }
                .drawWithContent {
                    // 봉투 아래로는 편지지가 비치지 않는다. 봉투가 내려가는 만큼 드러난다.
                    clipRect(bottom = envelope.bottom + drop(progress(), envelope)) { this@drawWithContent.drawContent() }
                }
        ) {
            letter(
                Modifier
                    .onPlaced { placed -> stage?.let { card = it.localBoundingBoxOf(placed) } }
                    .graphicsLayer {
                        val open = progress()
                        val grow = open.between(UNFOLD_START, 1f)
                        val fitted = if (card.width > 0) (envelope.width - DiarySpacing.x4.toPx() * 2) / card.width else 1f
                        val top = envelope.top + DiarySpacing.x3.toPx() - open.between(RISE_START, RISE_END) * envelope.height * RISE_SHARE
                        alpha = if (open > 0f) 1f else 0f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleX = lerp(fitted, 1f, grow)
                        scaleY = scaleX
                        translationY = (top - card.top) * (1f - grow)
                    }
            )
        }
        // 덮개 틈으로 글자 조각이 비치지 않게 편지지가 나올 때까지 빈 종이로 덮어 둔다.
        Canvas(fade) {
            val open = progress()
            val blank = 1f - open.between(RISE_START, BLANK_END)
            translate(envelope.left, envelope.top - open.between(RISE_START, RISE_END) * envelope.height * RISE_SHARE) {
                drawBlankPaper(envelope.size, blank, colors)
            }
        }
        Canvas(fade) {
            translate(envelope.left, envelope.top + drop(progress(), envelope)) { drawFront(envelope.size, progress(), colors, pen) }
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = DiarySpacing.screenCompact),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier.graphicsLayer { alpha = 1f - progress().between(0f, HEAD_FADE_END) },
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
            Spacer(
                Modifier
                    .widthIn(max = DiaryArtSize.envelope)
                    .fillMaxWidth()
                    .aspectRatio(ENVELOPE_RATIO)
                    .onPlaced { placed -> stage?.let { envelope = it.localBoundingBoxOf(placed) } }
                    .clickable(interactionSource = null, indication = null, onClick = onOpen)
                    .clearAndSetSemantics { }
            )
        }
    }
}

/** 편지지가 펼쳐지는 동안 봉투가 내려가는 거리. 화면 아래로 빠질 만큼이다. */
private fun DrawScope.drop(progress: Float, envelope: Rect): Float = progress.between(UNFOLD_START, 1f) * (size.height - envelope.top)

private fun DrawScope.flapTip(height: Float, progress: Float): Float = FLAP_DEPTH * height * cos(progress.between(0f, FLAP_END).halfTurn)

private fun flapPath(width: Float, tip: Float) = Path().apply {
    moveTo(0f, 0f)
    lineTo(width / 2, tip)
    lineTo(width, 0f)
}

private fun DrawScope.bodyPath(area: Size) =
    Path().apply { addRoundRect(RoundRect(0f, 0f, area.width, area.height, CornerRadius(DiarySpacing.x4.toPx()))) }

private fun DrawScope.outline() = Stroke(DiarySpacing.hairline.toPx(), join = StrokeJoin.Round)

/** 봉투 안쪽과 다 넘어간 덮개. 편지지보다 뒤에 있다. */
private fun DrawScope.drawBack(area: Size, progress: Float, colors: DiaryColors) {
    drawPath(bodyPath(area), colors.sunken)
    val tip = flapTip(area.height, progress)
    if (tip < 0) {
        val flap = flapPath(area.width, tip)
        drawPath(flap, colors.selected)
        drawPath(flap, colors.lineStrong, style = outline())
    }
}

/** 글자를 가리는 빈 종이. 편지지가 봉투 안에 있을 때의 자리와 같다. */
private fun DrawScope.drawBlankPaper(area: Size, alpha: Float, colors: DiaryColors) {
    val inset = DiarySpacing.x4.toPx()
    drawRoundRect(
        colors.paper,
        Offset(inset, DiarySpacing.x3.toPx()),
        Size(area.width - inset * 2, area.height),
        CornerRadius(DiarySpacing.x4.toPx()),
        alpha = alpha
    )
}

/** 봉투 앞면과 닫힌 덮개, 봉인. 편지지보다 앞에 있다. */
private fun DrawScope.drawFront(area: Size, progress: Float, colors: DiaryColors, pen: VectorPainter) {
    val width = area.width
    val height = area.height
    val body = bodyPath(area)
    clipPath(body) {
        val middle = POCKET_DEPTH * height
        val pocket = Path().apply {
            moveTo(0f, 0f)
            lineTo(width / 2, middle)
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(pocket, colors.paper)
        val folds = Path().apply {
            moveTo(0f, 0f)
            lineTo(width / 2, middle)
            lineTo(width, 0f)
            moveTo(0f, height)
            lineTo(width / 2, middle)
            lineTo(width, height)
        }
        drawPath(folds, colors.lineStrong, style = outline())
    }
    drawPath(body, colors.lineStrong, style = outline())
    val tip = flapTip(height, progress)
    if (tip >= 0) {
        val flap = flapPath(width, tip)
        clipPath(body) { drawPath(flap, colors.selected) }
        drawPath(flap, colors.lineStrong, style = outline())
        drawSeal(Offset(width / 2, tip), tip / (FLAP_DEPTH * height), colors, pen)
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
