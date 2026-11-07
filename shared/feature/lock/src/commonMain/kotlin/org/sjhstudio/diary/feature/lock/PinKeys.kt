package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * 하드웨어 키보드로 숫자와 지우기를 받는다(태블릿 키보드, 시험 스크립트의 `input keyevent`).
 * 화면이 뜨면 바로 초점을 잡는다.
 */
@Composable
internal fun Modifier.pinKeys(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit): Modifier {
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    return focusRequester(focus).focusable().onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
        if (event.key == Key.Backspace) {
            onDelete()
            return@onKeyEvent true
        }
        val char = event.utf16CodePoint.toChar()
        if (char !in '0'..'9') return@onKeyEvent false
        onDigit(char)
        true
    }
}
