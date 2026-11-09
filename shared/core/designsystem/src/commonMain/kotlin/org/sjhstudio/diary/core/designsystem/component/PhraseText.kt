package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle

/**
 * 안내 글.
 * iOS는 한국어 줄을 글자 단위로 바꿔 "그대로예 / 요."처럼 낱말 가운데서 끊었다(2026-11-09 iPhone 17에서 확인).
 * 그릴 때만 한글 글자 사이에 붙임 글자를 넣어 낱말 단위로 줄을 바꾸고, 화면 읽기와 시험에는 원래 글을 준다.
 * 사용자가 쓴 일기 글에는 쓰지 않는다(안내 문구 전용).
 */
@Composable
internal fun PhraseText(text: String, style: TextStyle, modifier: Modifier = Modifier, onTextLayout: ((TextLayoutResult) -> Unit)? = null) {
    BasicText(
        keepWords(text),
        modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        style = style,
        onTextLayout = onTextLayout
    )
}
