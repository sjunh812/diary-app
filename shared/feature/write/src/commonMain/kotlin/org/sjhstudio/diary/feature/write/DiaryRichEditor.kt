@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
            visualTransformation = state.visualTransformation,
            onTextLayout = {
                state.onTextLayout(textLayoutResult = it, density = density)
                onTextLayout(it)
            },
            interactionSource = interactionSource,
            cursorBrush = cursorBrush
        )
    }
}
