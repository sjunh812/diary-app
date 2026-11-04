package org.sjhstudio.diary.app.scaffold

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** 하드웨어 키보드 단축키(contracts/navigation.md 단축키). 안드로이드는 Ctrl, iPad는 Command다. */
internal fun Modifier.diaryShortcuts(onNewEntry: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown || !(event.isCtrlPressed || event.isMetaPressed)) return@onPreviewKeyEvent false
    when (event.key) {
        Key.N -> {
            onNewEntry()
            true
        }

        else -> false
    }
}
