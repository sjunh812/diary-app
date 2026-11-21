package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.ceil

private const val ELLIPSIS = "…"

private fun Float.ceilToInt(): Int = ceil(this).toInt()

/**
 * 줄 수를 넘으면 "…"로 끝나는 일기 미리보기
 * iOS(Skia)는 마지막 보이는 줄 뒤가 쓴 줄바꿈이면 "…"를 그리지 않아 그 줄 끝에 직접 붙임
 * 배치 단계에서 정해 한 번에 그려서 "…"가 나중에 붙어 보이지 않음
 * 글 선택, 기준선 맞춤은 없음. 목록 미리보기 전용
 *
 * @param onClip 줄 수를 넘어 잘렸는지 알림. 구성·배치 중에 불려 상태를 쓰지 않음
 */
@Composable
fun DiaryPreviewText(
    text: AnnotatedString,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier,
    onClip: ((Boolean) -> Unit)? = null
) {
    val measurer = rememberTextMeasurer()
    Box(modifier.then(PreviewTextElement(text, style, maxLines, measurer, onClip)))
}

/** 보일 글과 잘렸는지. 줄 끝에 직접 "…"를 붙인 때도 잘린 것 */
internal fun TextMeasurer.preview(
    text: AnnotatedString,
    style: TextStyle,
    maxLines: Int,
    maxWidth: Int,
    density: Density,
    layoutDirection: LayoutDirection,
    minWidth: Int = 0
): Pair<TextLayoutResult, Boolean> {
    fun measure(shown: AnnotatedString) = measure(
        shown,
        style,
        TextOverflow.Ellipsis,
        maxLines = maxLines,
        constraints = Constraints(minWidth = minWidth, maxWidth = maxWidth),
        layoutDirection = layoutDirection,
        density = density
    )
    val full = measure(text)
    if (!full.hasVisualOverflow) return full to false
    val end = full.getLineEnd(full.lineCount - 1, visibleEnd = true)
    // 화면 폭에서 접힌 줄은 두 플랫폼 모두 "…"를 그림
    // "…"가 다음 줄로 넘어가면 접힌 줄이 되어 플랫폼이 다시 줄임
    val hardBreak = text.text.substring(end).takeWhile(Char::isWhitespace).contains('\n')
    val shown = if (hardBreak) {
        measure(
            buildAnnotatedString {
                append(text.subSequence(0, end))
                append(ELLIPSIS)
            }
        )
    } else {
        full
    }
    return shown to true
}

private data class PreviewTextElement(
    val text: AnnotatedString,
    val style: TextStyle,
    val maxLines: Int,
    val measurer: TextMeasurer,
    val onClip: ((Boolean) -> Unit)?
) : ModifierNodeElement<PreviewTextNode>() {
    override fun create() = PreviewTextNode(text, style, maxLines, measurer, onClip)

    override fun update(node: PreviewTextNode) = node.update(text, style, maxLines, measurer, onClip)
}

private class PreviewTextNode(
    var text: AnnotatedString,
    var style: TextStyle,
    var maxLines: Int,
    var measurer: TextMeasurer,
    var onClip: ((Boolean) -> Unit)?
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    SemanticsModifierNode {
    private var shown: TextLayoutResult? = null
    private var clipped = false

    // 다시 구성할 때마다 새 알림 함수가 와도 다시 재지 않음
    override val shouldAutoInvalidate = false

    fun update(text: AnnotatedString, style: TextStyle, maxLines: Int, measurer: TextMeasurer, onClip: ((Boolean) -> Unit)?) {
        val changed = text != this.text || style != this.style || maxLines != this.maxLines || measurer != this.measurer
        if (text != this.text) invalidateSemantics()
        this.text = text
        this.style = style
        this.maxLines = maxLines
        this.measurer = measurer
        if (onClip !== this.onClip && !changed && shown != null) onClip?.invoke(clipped)
        this.onClip = onClip
        if (changed) invalidateMeasurement()
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val (result, clipped) = measurer.preview(text, style, maxLines, constraints.maxWidth, this, layoutDirection, constraints.minWidth)
        if (result != shown) {
            shown = result
            invalidateDraw()
        }
        this@PreviewTextNode.clipped = clipped
        onClip?.invoke(clipped)
        val width = constraints.constrainWidth(result.size.width)
        val height = constraints.constrainHeight(result.size.height)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) { placeable.place(0, 0) }
    }

    // 고유 크기를 물을 때는 그릴 글을 바꾸지 않고 잘림도 알리지 않음
    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        unbounded().multiParagraph.intrinsics.minIntrinsicWidth.ceilToInt()

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        unbounded().multiParagraph.intrinsics.maxIntrinsicWidth.ceilToInt()

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurer.preview(text, style, maxLines, width, this, layoutDirection).first.size.height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurer.preview(text, style, maxLines, width, this, layoutDirection).first.size.height

    private fun IntrinsicMeasureScope.unbounded(): TextLayoutResult =
        measurer.measure(text, style, maxLines = maxLines, layoutDirection = layoutDirection, density = this)

    // 넘칠 때만 칸 밖을 자름. 늘 자르면 큰 글자에서 글자 끝이 깎일 수 있음
    override fun ContentDrawScope.draw() {
        shown?.let { if (it.hasVisualOverflow) clipRect { drawText(it) } else drawText(it) }
        drawContent()
    }

    // 낭독기는 잘리기 전 미리보기 전체를 읽음
    override fun SemanticsPropertyReceiver.applySemantics() {
        text = this@PreviewTextNode.text
    }
}
