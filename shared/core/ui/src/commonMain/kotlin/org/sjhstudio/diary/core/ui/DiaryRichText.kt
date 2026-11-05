package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.RichStyle
import org.sjhstudio.diary.core.model.parseRichText

/**
 * 일기 본문을 서식과 함께 그린다. [style]은 고른 글꼴과 글자 크기(`diaryBodyTextStyle`)다.
 * 글자색과 형광펜은 저장된 라이트 값 대신 지금 테마의 같은 토큰으로 그린다(contracts/rich-text-html.md).
 */
@Composable
fun DiaryRichText(html: String, style: TextStyle, modifier: Modifier = Modifier, onCheck: ((Int) -> Unit)? = null) {
    val colors = DiaryTheme.colors
    val text = remember(html, colors) { richAnnotated(html, colors) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val touch = with(LocalDensity.current) { DiarySpacing.minTouchTarget.toPx() }
    val check by rememberUpdatedState(onCheck)
    // 누를 수 없는 본문은 손길을 가로채지 않는다.
    val tappable =
        if (onCheck == null) {
            modifier
        } else {
            modifier.pointerInput(text) {
                detectTapGestures { position ->
                    val start = layout?.checkLineAt(position, touch) ?: return@detectTapGestures
                    val index = Checklist.indexOfItem(text.text, start) ?: return@detectTapGestures
                    check?.invoke(index)
                }
            }
        }
    BasicText(
        text,
        tappable.checklistMarks({ text.text }, { layout }),
        style = style.copy(color = colors.textPrimary),
        onTextLayout = { layout = it }
    )
}

internal fun richAnnotated(html: String, colors: DiaryColors): AnnotatedString {
    val parsed = parseRichText(html)
    val builder = AnnotatedString.Builder(parsed.text)
    for (span in parsed.spans) builder.addStyle(span.style.toSpan(colors), span.start, span.end)
    return builder.toAnnotatedString()
}

private fun RichStyle.toSpan(colors: DiaryColors): SpanStyle = when (this) {
    RichStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    RichStyle.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
    is RichStyle.Ink -> SpanStyle(color = color.token(colors))
    is RichStyle.Mark -> SpanStyle(background = color.token(colors))
}

private fun InkColor.token(colors: DiaryColors): Color = when (this) {
    InkColor.RED -> colors.ink.red
    InkColor.ORANGE -> colors.ink.orange
    InkColor.GREEN -> colors.ink.green
    InkColor.BLUE -> colors.ink.blue
    InkColor.GRAY -> colors.ink.gray
    InkColor.PURPLE -> colors.ink.purple
    InkColor.PINK -> colors.ink.pink
}

private fun MarkColor.token(colors: DiaryColors): Color = when (this) {
    MarkColor.YELLOW -> colors.mark.yellow
    MarkColor.GREEN -> colors.mark.green
    MarkColor.BLUE -> colors.mark.blue
    MarkColor.PURPLE -> colors.mark.purple
    MarkColor.PINK -> colors.mark.pink
}
