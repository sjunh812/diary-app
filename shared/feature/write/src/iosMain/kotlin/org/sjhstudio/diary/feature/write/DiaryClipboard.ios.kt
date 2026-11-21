@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.clipboard.createRichTextClipboardManager
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.cinterop.BetaInteropApi
import org.sjhstudio.diary.core.model.richtext.editorToClipboard
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

internal actual fun diaryClipboard(state: RichTextState, platform: Clipboard): Clipboard =
    DiaryIosClipboard(state, platform, createRichTextClipboardManager(richTextState = state, clipboard = platform))

private class DiaryIosClipboard(private val state: RichTextState, private val platform: Clipboard, private val rich: Clipboard) :
    Clipboard by rich {
    @OptIn(ExperimentalComposeUiApi::class, BetaInteropApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val copied = state.copySelection
        if (clipEntry == null || copied == null || copied.collapsed) return rich.setClipEntry(clipEntry)
        val text = state.toText(copied)
        val html = NSString.create(string = editorToClipboard(state.toHtml(copied))).dataUsingEncoding(NSUTF8StringEncoding)
        val plain = NSString.create(string = text).dataUsingEncoding(NSUTF8StringEncoding)
        if (html == null || plain == null) return rich.setClipEntry(clipEntry)
        platform.nativeClipboard.items = listOf(mapOf(HTML_TYPE to html, PLAIN_TYPE to plain))
    }

    private companion object {
        const val HTML_TYPE = "public.html"
        const val PLAIN_TYPE = "public.utf8-plain-text"
    }
}
