@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.mohamedrejeb.richeditor.clipboard.ClipboardEventEffect
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.adjustTextIndicatorOffset
import com.mohamedrejeb.richeditor.ui.drawRichSpanStyle
import org.sjhstudio.diary.core.ui.rememberChecklistIndent
import org.sjhstudio.diary.core.ui.withChecklistIndent

/**
 * 일기 본문 편집기
 * 라이브러리의 `BasicRichTextEditor`와 같고, 키보드 입력은 [EditorInput], 복사는 [diaryClipboard]를 거침
 * 라이브러리를 올릴 때 이 파일을 원본과 견줌(compose-rich-editor 1.2.1)
 */
@Composable
internal fun DiaryRichEditor(
    state: RichTextState,
    textStyle: TextStyle,
    cursorBrush: Brush,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val clipboard = LocalClipboard.current
    val richClipboard = remember(state, clipboard) { diaryClipboard(state, clipboard) }
    val interactionSource = remember { MutableInteractionSource() }
    val noPadding = remember { PaddingValues() }
    val input = remember(state) { EditorInput(state) }
    ClipboardEventEffect(richTextState = state)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    state.onSelectionGestureStart()
                    adjustTextIndicatorOffset(interaction.pressPosition, state, 0f, 0f)
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> state.onSelectionGestureEnd()
            }
        }
    }
    val indent = rememberChecklistIndent(textStyle)
    val marks = remember(state.annotatedString) { state.annotatedString.marks() }
    val plain = remember(state.annotatedString) { state.annotatedString.withoutBackgrounds() }
    val shown = remember(plain, indent) { plain.withChecklistIndent(indent) }
    val transformation = remember(shown) { VisualTransformation { TransformedText(shown, OffsetMapping.Identity) } }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    CompositionLocalProvider(LocalClipboard provides richClipboard) {
        BasicTextField(
            value = state.textFieldValue,
            onValueChange = input::apply,
            modifier = modifier
                .onFocusChanged { state.isFocused = it.isFocused }
                .onPreviewKeyEvent { event -> state.onPreviewKeyEvent(event) }
                .drawRichSpanStyle(richTextState = state, topPadding = 0f, startPadding = 0f)
                .drawMarks(marks, { state.selection }, { layout })
                .pointerInput(state) {
                    awaitPointerEventScope {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed } ?: continue
                            state.onSelectionGesturePointerMove(Offset(change.position.x, change.position.y))
                        }
                    }
                }
                .adjustTextIndicatorOffset(
                    state = state,
                    contentPadding = noPadding,
                    density = density,
                    layoutDirection = layoutDirection,
                    scope = rememberCoroutineScope()
                ),
            textStyle = textStyle,
            visualTransformation = transformation,
            onTextLayout = {
                layout = it
                state.onTextLayout(textLayoutResult = it, density = density)
                onTextLayout(it)
            },
            interactionSource = interactionSource,
            cursorBrush = cursorBrush
        )
    }
}

/** 형광펜 한 구간. [end]는 들어가지 않음 */
internal data class Mark(val start: Int, val end: Int, val color: Color)

/**
 * 형광펜 구간. 라이브러리가 고른 글 아래에 넣는 투명 바탕(선택 가림)은 형광펜이 아니라 뺌
 * 겹치면 안쪽 구간이 뒤에 와 위에 칠해짐
 */
internal fun AnnotatedString.marks(): List<Mark> = spanStyles.mapNotNull { span ->
    val color = span.item.background
    Mark(span.start, span.end, color).takeIf { color.isSpecified && color.alpha > 0f && span.start < span.end }
}

/**
 * 바탕색을 뺀 글. 형광펜은 [drawMarks]가 글 뒤에 따로 그림
 * 고르는 범위나 형광펜이 바뀌어도 그릴 글이 같아 다시 배치하지 않음. 끌던 핸들이 멈추지 않음(라이브러리 #730·#731)
 */
internal fun AnnotatedString.withoutBackgrounds(): AnnotatedString = if (spanStyles.none { it.item.background.isSpecified }) {
    this
} else {
    flatMapAnnotations { range ->
        val style = range.item as? SpanStyle
        when {
            style == null || !style.background.isSpecified -> listOf(range)
            style == SpanStyle(background = style.background) -> emptyList()
            else -> listOf(AnnotatedString.Range(style.copy(background = Color.Unspecified), range.start, range.end, range.tag))
        }
    }
}

/**
 * 형광펜을 글 뒤에 그림. 고른 구간은 비워 입력란의 선택 색이 다른 글과 똑같이 보이게 함
 * 안드로이드 `Layout.draw`처럼 바탕, 선택, 글자 순서. 줄 끝 공백은 칠하지 않음(글자 바탕색과 같게)
 */
private fun Modifier.drawMarks(marks: List<Mark>, selection: () -> TextRange, layout: () -> TextLayoutResult?): Modifier =
    if (marks.isEmpty()) {
        this
    } else {
        // 고른 범위나 배치가 바뀔 때만 경로를 다시 만듦. 커서가 깜빡여 다시 그릴 때는 만든 경로를 씀
        drawWithCache {
            val result = layout()
            val picked = selection()
            val paths = if (result == null) emptyList() else marks.paths(result, picked)
            onDrawBehind { paths.forEach { (path, color) -> drawPath(path, color) } }
        }
    }

private fun List<Mark>.paths(result: TextLayoutResult, picked: TextRange): List<Pair<Path, Color>> {
    val length = result.layoutInput.text.length
    val paths = ArrayList<Pair<Path, Color>>()
    for (mark in this) {
        for ((from, to) in mark.outside(picked)) {
            result.paintLines(from.coerceAtMost(length), to.coerceAtMost(length)) { paths += it to mark.color }
        }
    }
    return paths
}

/** 고른 범위 밖 조각. 고르지 않았으면 구간 그대로 */
internal fun Mark.outside(picked: TextRange): List<Pair<Int, Int>> {
    if (picked.collapsed || picked.max <= start || picked.min >= end) return listOf(start to end)
    return listOfNotNull((start to picked.min).takeIf { picked.min > start }, (picked.max to end).takeIf { picked.max < end })
}

/** 줄마다 보이는 글자까지만 칠함 */
private inline fun TextLayoutResult.paintLines(from: Int, to: Int, paint: (Path) -> Unit) {
    if (from >= to) return
    for (line in getLineForOffset(from)..getLineForOffset(to - 1)) {
        val start = maxOf(from, getLineStart(line))
        val end = minOf(to, getLineEnd(line, visibleEnd = true))
        if (start < end) paint(getPathForRange(start, end))
    }
}
