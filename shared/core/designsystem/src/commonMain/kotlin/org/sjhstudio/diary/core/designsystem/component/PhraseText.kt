package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * 안내 글
 * iOS는 한국어 줄을 글자 단위로 바꿔 "그대로예 / 요."처럼 낱말 가운데서 끊음
 * 그릴 때만 한글 글자 사이에 붙임 글자를 넣어 낱말 단위로 줄을 바꾸고, 화면 읽기와 시험에는 원래 글을 줌
 * 사용자가 쓴 일기 글에는 쓰지 않음(안내 문구 전용)
 * 화면 읽기 정보를 이 글로 바꿔 써서 제목·알림 같은 의미 정보는 [modifier] 대신 [semantics]로 줌
 */
@Composable
fun PhraseText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    semantics: SemanticsPropertyReceiver.() -> Unit = {},
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) = PhraseText(AnnotatedString(text), style, modifier, maxLines, minLines, overflow, semantics, onTextLayout)

/** 강조한 낱말이 있는 [PhraseText]. 글자 꾸밈과 문단 꾸밈만 옮김 */
@Composable
fun PhraseText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    semantics: SemanticsPropertyReceiver.() -> Unit = {},
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    BasicText(
        remember(text) { keepWords(text) },
        modifier.clearAndSetSemantics {
            this.text = text
            semantics()
        },
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        maxLines = maxLines,
        minLines = minLines
    )
}
