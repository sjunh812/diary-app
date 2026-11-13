@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.platform.currentPlatform

private const val HANGUL_START = 'ㄱ'
private const val HANGUL_END = '힣'

/**
 * 키보드 입력을 편집기에 넘김
 * iOS 한글 키보드는 공백 뒤 글자를 조합할 때 공백과 글자를 함께 골라 지운 뒤 공백과 새 글자를 다시 넣음
 * 편집기는 줄바꿈과 목록 기호 뒤를 공백으로 들고 있어, 이 순서를 그대로 받으면 줄이 합쳐지고 목록 기호가 깨짐
 * 그래서 공백은 그대로 두고 글자만 바꿈
 * 안드로이드는 이 순서로 입력하지 않아 [fixKeyboard]가 꺼져 있고 입력을 그대로 넘김
 */
internal class EditorInput(private val state: RichTextState, private val fixKeyboard: Boolean = currentPlatform.isIOS) {
    /** 지켜야 할 공백 바로 뒤 위치. 키보드가 공백과 글자를 고른 동안만 있음 */
    private var kept: Int? = null

    /** 키보드가 다시 넣을 공백의 위치 */
    private var echoed: Int? = null

    /**
     * 선택 핸들을 끄는 중이면 늘어나는 수
     * 편집기는 이 값이 멈춘 뒤에야 꾸밈을 바꿔 그림([DiaryRichEditor])
     */
    var dragTick by mutableIntStateOf(0)
        private set

    /** 선택 핸들을 끄는 중인지. 다른 입력이 오면 바로 풀림 */
    var dragging by mutableStateOf(false)

    fun apply(new: TextFieldValue) {
        if (isHandleDrag(state.textFieldValue, new)) {
            // 선택 핸들을 끄는 중이면 일반 입력칸처럼 선택만 바꿈
            // 라이브러리는 선택마다 끌기 끝을 손가락 줄과 문단 시작에 맞춰 고치는데,
            // 그 보정이 끌던 끝을 붙잡아 핸들이 가다가 멈춤
            // 커서를 놓거나 새로 고를 때(길게 누르기)는 라이브러리 보정을 그대로 거침
            dragging = true
            dragTick++
            state.selection = new.selection
        } else {
            dragging = false
            type(new)
        }
    }

    private fun type(new: TextFieldValue) {
        if (!fixKeyboard) {
            state.onTextFieldValueChange(new)
            return
        }
        val old = state.textFieldValue
        val value = withoutEcho(old, new) ?: return
        state.onTextFieldValueChange(keepSpace(old, value))
    }

    /** 공백과 글자를 고르면 글자만 고르고, 그 범위를 지우면 글자만 지움 */
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

    /** 키보드가 다시 넣은 공백을 뺌. 빼고 나서 달라진 것이 없으면 null */
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

    /** 글은 그대로이고 이미 고른 범위의 끝만 움직임 */
    private fun isHandleDrag(old: TextFieldValue, new: TextFieldValue): Boolean = new.text == old.text &&
        new.composition == old.composition &&
        !old.selection.collapsed &&
        !new.selection.collapsed &&
        new.selection != old.selection
}

/**
 * 키보드가 조합 중인 글자를 확정
 * 한글 키보드는 낱말 전체를 조합 중으로 잡음. 그대로 서식을 바꾸면 다음 입력이 조합 낱말을 통째로 다시 넣어 서식이 되돌아감
 * 조합 범위를 비우면 입력칸이 키보드를 다시 붙여 조합이 끝남
 */
internal fun RichTextState.commitComposition() {
    if (composition != null) onTextFieldValueChange(textFieldValue.copy(composition = null))
}
