@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.platform.currentPlatform

private const val HANGUL_START = 'ㄱ'
private const val HANGUL_END = '힣'

/**
 * 키보드 입력을 편집기에 넘긴다.
 * iOS 한글 키보드는 공백 뒤의 글자를 조합할 때 공백과 글자를 함께 골라 지운 뒤 공백과 새 글자를 다시 넣는다.
 * 편집기는 줄바꿈과 목록 기호 뒤를 공백으로 들고 있어서, 이 순서를 그대로 받으면 줄이 합쳐지고 목록 기호가 깨진다.
 * 그래서 공백은 그대로 두고 글자만 바꾼다.
 * 안드로이드는 이 순서로 입력하지 않아서 [fixKeyboard]가 꺼져 있고 입력을 그대로 넘긴다.
 */
internal class EditorInput(private val state: RichTextState, private val fixKeyboard: Boolean = currentPlatform.isIOS) {
    /** 지켜야 할 공백 바로 뒤 위치. 키보드가 공백과 글자를 고른 동안만 있다. */
    private var kept: Int? = null

    /** 키보드가 다시 넣을 공백의 위치. */
    private var echoed: Int? = null

    fun apply(new: TextFieldValue) {
        if (!fixKeyboard) {
            state.onTextFieldValueChange(new)
            return
        }
        val old = state.textFieldValue
        val value = withoutEcho(old, new) ?: return
        state.onTextFieldValueChange(keepSpace(old, value))
    }

    /** 공백과 글자를 고르면 글자만 고르고, 그 범위를 지우면 글자만 지운다. */
    private fun keepSpace(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val start = kept
        kept = null
        return when {
            start != null && removesPick(old, new, start) -> {
                echoed = start
                TextFieldValue(old.text.removeRange(start, old.selection.max), TextRange(start))
            }

            isKeyboardPick(old, new) -> {
                kept = new.selection.min + 1
                new.copy(selection = TextRange(new.selection.min + 1, new.selection.max))
            }

            else -> new
        }
    }

    private fun removesPick(old: TextFieldValue, new: TextFieldValue, start: Int): Boolean {
        val caret = new.selection
        val shorter = new.text.length < old.text.length && caret.collapsed && caret.min <= start
        return shorter && new.text == old.text.removeRange(caret.min, old.selection.max)
    }

    private fun isKeyboardPick(old: TextFieldValue, new: TextFieldValue): Boolean {
        val picked = new.selection
        val sameText = new.text == old.text && old.selection.collapsed
        return sameText && picked.length == 2 && picked.max == old.selection.max && pickedText(old.text, picked)
    }

    private fun pickedText(text: String, picked: TextRange) = text[picked.min] == ' ' && text[picked.max - 1] in HANGUL_START..HANGUL_END

    /** 키보드가 다시 넣은 공백을 뺀다. 빼고 나서 달라진 것이 없으면 null이다. */
    private fun withoutEcho(old: TextFieldValue, new: TextFieldValue): TextFieldValue? {
        val at = echoed
        val hasEcho = at != null && new.text.length > old.text.length && new.text.getOrNull(at) == ' '
        if (at == null || !hasEcho) {
            echoed = null
            return new
        }
        val text = new.text.removeRange(at, at + 1)
        if (text != old.text) echoed = null
        return if (text == old.text) null else new.copy(text = text, selection = TextRange(new.selection.min - 1))
    }
}
