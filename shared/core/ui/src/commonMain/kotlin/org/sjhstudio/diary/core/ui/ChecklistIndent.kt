package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.sjhstudio.diary.core.model.Checklist

/**
 * 체크리스트 표시("☐ ")의 폭과 끝낸 표시를 맞출 자간
 * 둘째 줄부터 [width]만큼 들여 써 글이 칸 옆에서 이어짐(메모 앱과 같게)
 * iOS의 `☑`은 그림 문자라 `☐`보다 넓음. 표시는 칸으로 덮어 그리니 자간을 줄여 글이 같은 자리에서 시작하게 함
 */
@Immutable
data class ChecklistIndent(val width: TextUnit, val doneSpacing: TextUnit)

@Composable
fun rememberChecklistIndent(style: TextStyle): ChecklistIndent {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(style, density) {
        fun width(mark: Char) = measurer.measure("$mark ", style).size.width
        val open = width(Checklist.OPEN)
        with(density) { ChecklistIndent(open.toSp(), (open - width(Checklist.DONE)).coerceAtMost(0).toSp()) }
    }
}

/**
 * 체크리스트 줄에 둘째 줄 들여쓰기를 더함. 글자 수와 자리는 그대로라 저장하는 본문은 바뀌지 않음
 * 편집기 글은 문단마다 문단 스타일이 있어 그 문단에 더하고, 읽기 글은 줄마다 문단을 둠
 */
fun AnnotatedString.withChecklistIndent(indent: ChecklistIndent): AnnotatedString {
    val indented = if (paragraphStyles.isNotEmpty()) indentParagraphs(indent) else indentLines(indent)
    return indented.alignDoneMarks(indent)
}

private fun AnnotatedString.indentParagraphs(indent: ChecklistIndent): AnnotatedString = mapAnnotations { range ->
    val style = range.item as? ParagraphStyle
    if (style == null || !Checklist.startsItem(text, range.start)) {
        range
    } else {
        // 첫 줄 들여쓰기가 있는 문단도 둘째 줄이 첫 줄 글과 맞게 그만큼 더함
        val first = style.textIndent?.firstLine?.takeIf { it.isSp } ?: 0.sp
        val rest = (first.value + indent.width.value).sp
        AnnotatedString.Range(style.merge(ParagraphStyle(textIndent = TextIndent(first, rest))), range.start, range.end, range.tag)
    }
}

/**
 * 체크리스트가 있을 때만 줄마다 문단을 둠
 * 문단 끝 줄바꿈은 빈 줄로 그려져 편집기 라이브러리처럼 공백으로 바꿈. 낭독기도 공백을 낱말 경계로 읽음
 * 형광펜과 밑줄은 그 공백에서 끊어 줄 끝까지 그려지지 않게 함. 줄을 나눌 때는 원래 글을 씀
 */
private fun AnnotatedString.indentLines(indent: ChecklistIndent): AnnotatedString {
    val lines = text.split('\n')
    if (lines.none(Checklist::isItem)) return this
    val paragraphs = ArrayList<AnnotatedString.Range<ParagraphStyle>>(lines.size)
    val ends = ArrayList<Int>(lines.size)
    var start = 0
    for (line in lines) {
        val end = minOf(start + line.length + 1, text.length)
        val style = if (Checklist.isItem(line)) ParagraphStyle(textIndent = TextIndent(restLine = indent.width)) else ParagraphStyle()
        paragraphs += AnnotatedString.Range(style, start, end)
        if (start + line.length < text.length) ends += start + line.length
        start += line.length + 1
    }
    return AnnotatedString(text.replace('\n', ' '), spanStyles.flatMap { it.withoutPaintAt(ends) }, paragraphs)
}

/** 글자 밖까지 그리는 꾸밈(바탕색, 밑줄) 구간에서 문단 끝 자리를 뺌. 굵게·글자색은 그대로 */
private fun AnnotatedString.Range<SpanStyle>.withoutPaintAt(ends: List<Int>): List<AnnotatedString.Range<SpanStyle>> {
    if (!item.background.isSpecified && item.textDecoration == null) return listOf(this)
    val pieces = ArrayList<AnnotatedString.Range<SpanStyle>>()
    var from = start
    for (at in ends) {
        if (at < from || at >= end) continue
        if (at > from) pieces += AnnotatedString.Range(item, from, at, tag)
        from = at + 1
    }
    if (from < end) pieces += AnnotatedString.Range(item, from, end, tag)
    return pieces
}

/** 끝낸 표시의 자간을 줄여 빈 표시와 폭을 같게 함. 문단 시작은 위에서 정한 문단 스타일로 찾음 */
private fun AnnotatedString.alignDoneMarks(indent: ChecklistIndent): AnnotatedString {
    val marks =
        if (indent.doneSpacing.value == 0f) {
            emptyList()
        } else {
            paragraphStyles.map { it.start }.filter { Checklist.startsItem(text, it) && text[it] == Checklist.DONE }
        }
    if (marks.isEmpty()) return this
    val builder = AnnotatedString.Builder(this)
    for (at in marks) builder.addStyle(SpanStyle(letterSpacing = indent.doneSpacing), at, at + 1)
    return builder.toAnnotatedString()
}
