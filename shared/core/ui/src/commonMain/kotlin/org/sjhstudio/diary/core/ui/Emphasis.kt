package org.sjhstudio.diary.core.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/*
 * 문장 안 핵심 낱말 강조(`docs/design.md` 글자 "문장 안 강조")
 * 문자열 리소스의 자리에 [emphasis]로 감싼 값을 넣고, 만든 문장을 [withEmphasis]로 바꿈
 * 그래서 언어마다 어순이 달라도 강조가 그 값에 붙음
 * 색은 보라를 쓰지 않음. 보라는 누를 수 있는 것과 고른 것에만 씀
 */

private const val START = ''
private const val END = ''

/** 리소스 자리에 넣을 값을 강조 표시로 감쌈 */
fun emphasis(value: Any): String = "$START$value$END"

/**
 * [emphasis]로 감싼 부분에 [style]을 입히고 표시는 지움
 * 표시가 짝이 맞지 않으면 남은 부분은 강조 없이 그대로 둠
 */
fun String.withEmphasis(style: SpanStyle): AnnotatedString = buildAnnotatedString {
    var rest = this@withEmphasis
    while (true) {
        val start = rest.indexOf(START)
        val end = if (start < 0) -1 else rest.indexOf(END, start + 1)
        if (start < 0 || end < 0) {
            append(rest.replace(START.toString(), "").replace(END.toString(), ""))
            break
        }
        append(rest.substring(0, start))
        withStyle(style) { append(rest.substring(start + 1, end)) }
        rest = rest.substring(end + 1)
    }
}
