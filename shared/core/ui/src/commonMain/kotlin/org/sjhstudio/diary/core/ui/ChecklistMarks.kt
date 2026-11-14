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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.ChecklistItem
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.checklist_check
import org.sjhstudio.diary.core.ui.resources.checklist_uncheck

private const val CORNER = 0.25f

private val GAP = 6.dp

/** 글자 가운데가 기준선에서 위로 떨어진 거리. 글자 크기 대비 비율 */
private const val TEXT_MIDDLE = 0.37f

// 둥근 칸은 글줄보다 안쪽으로 보여 조금 내어 그림
// 그만큼 칸이 커지고 왼쪽 선은 그대로 맞아 보임
private val OUTDENT = 1.dp
private const val CHECK_START_X = 0.26f
private const val CHECK_START_Y = 0.52f
private const val CHECK_MIDDLE_X = 0.43f
private const val CHECK_MIDDLE_Y = 0.69f
private const val CHECK_END_X = 0.75f
private const val CHECK_END_Y = 0.33f

/**
 * 체크리스트 표시를 앱의 칸으로 덮어 그림
 * 표시 글자는 플랫폼마다 모양이 달라(iOS의 `☑`은 그림 문자) 글자는 저장용으로 두고 화면은 같게 맞춤
 * [text]는 줄바꿈이 살아 있는 글
 */
@Composable
fun Modifier.checklistMarks(text: () -> String, layout: () -> TextLayoutResult?, background: Color = DiaryTheme.colors.canvas): Modifier {
    val colors = DiaryTheme.colors
    val measurer = rememberTextMeasurer()
    return drawWithContent {
        drawContent()
        val result = layout() ?: return@drawWithContent
        val length = result.layoutInput.text.length
        var start = 0
        for (line in text().split('\n')) {
            if (Checklist.isItem(line) && start < length) {
                val glyph = result.getBoundingBox(start)
                // 덮개는 그 줄의 실제 표시 폭까지 덮어야 넓은 그림 문자가 비치지 않음
                val advance = result.getBoundingBox(start + 1).right - glyph.left
                val outdent = OUTDENT.toPx()
                val fontSize = result.layoutInput.style.fontSize
                // 칸 크기는 빈 표시와 띄어쓰기("☐ ")의 폭으로 정해 체크 전후가 같음
                // 그 줄의 표시 폭으로 재면 iOS의 체크한 표시(☑)가 그림 문자라 더 넓어 체크하면 칸이 커짐
                val room = measurer.measure(Checklist.PREFIX, result.layoutInput.style).size.width - GAP.toPx() + outdent
                val side = minOf(if (fontSize.isSp) fontSize.toPx() else glyph.height, room)
                // 줄 상자 가운데보다 글자 가운데에 맞춰야 칸이 글과 나란함
                val middle =
                    if (fontSize.isSp) {
                        result.getLineBaseline(result.getLineForOffset(start)) - fontSize.toPx() * TEXT_MIDDLE
                    } else {
                        glyph.center.y
                    }
                val topLeft = Offset(glyph.left - outdent, middle - side / 2)
                // 표시 글자는 뒤의 칸까지 덮어야 넓은 그림 문자가 비치지 않음
                drawRect(background, glyph.topLeft, Size(maxOf(advance, side + outdent), glyph.height))
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
 * 화면 낭독기로 체크를 바꾸는 동작
 * 칸은 글자 위에 그린 그림이라 낭독기가 누를 수 없음
 * [onToggle]은 항목 번호를 받음
 */
@Composable
fun checklistActions(text: String, onToggle: (ChecklistItem, Int) -> Unit): List<CustomAccessibilityAction> =
    Checklist.items(text).mapIndexed { index, item ->
        val label = stringResource(if (item.done) Res.string.checklist_uncheck else Res.string.checklist_check, item.label)
        CustomAccessibilityAction(label) {
            onToggle(item, index)
            true
        }
    }

/**
 * [position]이 체크리스트 표시 위면 그 줄의 시작 자리
 * 줄이 접힌 둘째 줄은 항목 아님
 * 읽기 화면은 글을 고칠 일이 없어 표시가 작아도 누르기 쉽게 [touch] 폭까지 받음
 * 쓰기 화면([editing])은 글자를 누르면 커서가 가야 해서 글이 시작하는 자리까지만 받음(Apple 메모, Notion과 같음)
 * [text]는 줄바꿈이 살아 있는 글
 * 에디터는 문단 사이를 칸으로 이어 그려서 따로 넘김
 */
fun TextLayoutResult.checkLineAt(position: Offset, touch: Float, text: String = layoutInput.text.text, editing: Boolean = false): Int? {
    if (position.y < 0 || position.y > size.height) return null
    val line = getLineForVerticalPosition(position.y)
    val start = getLineStart(line)
    val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
    if (start > 0 && text[start - 1] != '\n') return null
    if (!Checklist.isItem(text.substring(start, end))) return null
    val reach = if (editing) getBoundingBox(start + 1).right else getLineLeft(line) + touch
    return start.takeIf { position.x <= reach }
}
