package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Checklist

private const val CORNER = 0.25f
private val GAP = 6.dp
private const val CHECK_START_X = 0.26f
private const val CHECK_START_Y = 0.52f
private const val CHECK_MIDDLE_X = 0.43f
private const val CHECK_MIDDLE_Y = 0.69f
private const val CHECK_END_X = 0.75f
private const val CHECK_END_Y = 0.33f

/**
 * 체크리스트 표시를 앱의 칸으로 덮어 그린다.
 * 표시 글자는 플랫폼마다 모양이 달라서(iOS의 `☑`은 그림 문자다), 글자는 저장용으로 두고 화면은 같게 맞춘다.
 * [text]는 줄바꿈이 살아 있는 글이다.
 */
@Composable
fun Modifier.checklistMarks(text: () -> String, layout: () -> TextLayoutResult?, background: Color = DiaryTheme.colors.canvas): Modifier {
    val colors = DiaryTheme.colors
    return drawWithContent {
        drawContent()
        val result = layout() ?: return@drawWithContent
        val length = result.layoutInput.text.length
        var start = 0
        for (line in text().split('\n')) {
            if (Checklist.isItem(line) && start < length) {
                val glyph = result.getBoundingBox(start)
                // 글꼴마다 표시와 칸의 폭이 달라서, 글과 닿지 않는 크기로 맞춘다.
                val advance = result.getBoundingBox(start + 1).right - glyph.left
                val room = advance - GAP.toPx()
                val side = minOf(result.layoutInput.style.fontSize.toPx(), room)
                val topLeft = Offset(glyph.left, glyph.center.y - side / 2)
                // 표시 글자는 뒤의 칸까지 덮어야 넓은 그림 문자가 비치지 않는다.
                drawRect(background, glyph.topLeft, Size(advance, glyph.height))
                if (line[0] == Checklist.DONE) {
                    drawRoundRect(colors.accent, topLeft, Size(side, side), CornerRadius(side * CORNER))
                    drawCheck(topLeft, side, colors.onAccent)
                } else {
                    val inset = 0.75.dp.toPx()
                    drawRoundRect(
                        colors.textSecondary,
                        topLeft + Offset(inset, inset),
                        Size(side - inset * 2, side - inset * 2),
                        CornerRadius(side * CORNER),
                        style = Stroke(inset * 2)
                    )
                }
            }
            start += line.length + 1
        }
    }
}

private fun DrawScope.drawCheck(topLeft: Offset, side: Float, color: Color) {
    val path =
        Path().apply {
            moveTo(topLeft.x + side * CHECK_START_X, topLeft.y + side * CHECK_START_Y)
            lineTo(topLeft.x + side * CHECK_MIDDLE_X, topLeft.y + side * CHECK_MIDDLE_Y)
            lineTo(topLeft.x + side * CHECK_END_X, topLeft.y + side * CHECK_END_Y)
        }
    drawPath(path, color, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * [position]이 체크리스트 표시 위면 그 줄의 시작 자리를 돌려준다.
 * 표시는 작아서 [touch] 폭까지 누른 것으로 친다. 줄이 접힌 둘째 줄은 항목이 아니다.
 * [text]는 줄바꿈이 살아 있는 글이다. 에디터는 문단 사이를 칸으로 이어 그려서 따로 넘긴다.
 */
fun TextLayoutResult.checkLineAt(position: Offset, touch: Float, text: String = layoutInput.text.text): Int? {
    if (position.y < 0 || position.y > size.height) return null
    val line = getLineForVerticalPosition(position.y)
    val start = getLineStart(line)
    val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
    if (start > 0 && text[start - 1] != '\n') return null
    if (!Checklist.isItem(text.substring(start, end))) return null
    return start.takeIf { position.x - getLineLeft(line) <= touch }
}
