@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.AndroidClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.nativeClipboardManager
import com.mohamedrejeb.richeditor.clipboard.createRichTextClipboardManager
import com.mohamedrejeb.richeditor.model.RichTextState
import org.sjhstudio.diary.core.model.richtext.editorToClipboard

internal actual fun diaryClipboard(state: RichTextState, platform: Clipboard): Clipboard =
    DiaryAndroidClipboard(state, platform, createRichTextClipboardManager(richTextState = state, clipboard = platform))

/** Compose가 `AndroidClipboard`로 플랫폼 클립보드를 찾아 그 꼴을 지킴(라이브러리 #744) */
@SuppressLint("VisibleForTests")
private class DiaryAndroidClipboard(private val state: RichTextState, private val platform: Clipboard, private val rich: Clipboard) :
    AndroidClipboard,
    Clipboard by rich {
    override val clipboardManager: ClipboardManager
        get() = platform.nativeClipboardManager

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val copied = state.copySelection
        if (clipEntry == null || copied == null || copied.collapsed) return rich.setClipEntry(clipEntry)
        val text = state.toText(copied)
        platform.setClipEntry(ClipEntry(ClipData.newHtmlText("rich text", text, editorToClipboard(state.toHtml(copied)))))
    }
}
