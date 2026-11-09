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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.clipboard.ClipboardEventEffect
import com.mohamedrejeb.richeditor.clipboard.createRichTextClipboardManager
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.adjustTextIndicatorOffset
import com.mohamedrejeb.richeditor.ui.drawRichSpanStyle
import kotlinx.coroutines.delay

/**
 * 일기 본문 편집기.
 * 라이브러리의 `BasicRichTextEditor`와 같고, 키보드 입력만 [EditorInput]을 지나게 했다.
 * 라이브러리를 올릴 때 이 파일을 원본과 견준다(compose-rich-editor 1.2.1).
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
    val richClipboard = remember(state, clipboard) { createRichTextClipboardManager(richTextState = state, clipboard = clipboard) }
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
    // 핸들을 끄는 동안에는 화면 변환을 바꾸지 않는다.
    // 형광펜(바탕색)이 있으면 라이브러리가 선택이 바뀔 때마다 꾸밈을 다시 만들고 화면 변환을 새로 바꾸는데,
    // 안드로이드는 그때 끌던 핸들을 놓쳐 핸들이 가다가 멈췄다(2026-11-10 사용자 실기기, 라이브러리 #730·#731).
    // 끌기가 멈추면(잠깐 새 끌기가 없으면) 그때의 꾸밈으로 다시 그린다.
    val live = state.visualTransformation
    var held by remember { mutableStateOf(live) }
    if (!input.dragging) held = live
    LaunchedEffect(input.dragTick) {
        if (input.dragging) {
            delay(DRAG_SETTLE_MS)
            input.dragging = false
        }
    }
    CompositionLocalProvider(LocalClipboard provides richClipboard) {
        BasicTextField(
            value = state.textFieldValue,
            onValueChange = input::apply,
            modifier = modifier
                .onFocusChanged { state.isFocused = it.isFocused }
                .onPreviewKeyEvent { event -> state.onPreviewKeyEvent(event) }
                .drawRichSpanStyle(richTextState = state, topPadding = 0f, startPadding = 0f)
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
            visualTransformation = if (input.dragging) held else live,
            onTextLayout = {
                state.onTextLayout(textLayoutResult = it, density = density)
                onTextLayout(it)
            },
            interactionSource = interactionSource,
            cursorBrush = cursorBrush
        )
    }
}

/** 핸들 끌기가 멈췄다고 보는 시간. 손가락을 잠깐 멈췄다 다시 끌어도 이어지게 넉넉히 둔다. */
private const val DRAG_SETTLE_MS = 1_000L
