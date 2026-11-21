package org.sjhstudio.diary.core.model.richtext

// 목록 미리보기 줄 나누기와 띄어쓰기 다듬기(PreviewText)

/**
 * 줄마다 남길 글자의 원문 위치
 * 띄어쓰기 아닌 글자는 모두 남으니, [limit]을 넘게 모으면 뒤는 보지 않고 멈춘 것을 알림
 */
internal fun splitLines(text: String, limit: Int): Pair<List<List<Int>>, Boolean> {
    val lines = ArrayList<List<Int>>()
    var line = ArrayList<Int>()
    var solid = 0
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
            i++
            continue
        }
        if (isBreak(c)) {
            lines += line
            line = ArrayList()
        } else {
            line += i
            if (!isSpace(c) && ++solid > limit) {
                lines += line
                return lines to (i + 1 < text.length)
            }
        }
        i++
    }
    lines += line
    return lines to false
}

/**
 * 줄 끝 띄어쓰기를 지움. 띄어쓰기만 있는 줄은 빈 줄이 됨
 * [keepSpaces]가 아니면 줄 앞 띄어쓰기도 지우고 연이은 띄어쓰기는 하나로
 */
internal fun tidy(text: String, indices: List<Int>, keepSpaces: Boolean): List<Int> {
    val out = ArrayList<Int>(indices.size)
    for (at in indices) {
        val squeeze = !keepSpaces && isSpace(text[at])
        if (squeeze && (out.isEmpty() || isSpace(text[out.last()]))) continue
        out += at
    }
    while (out.isNotEmpty() && isSpace(text[out.last()])) out.removeAt(out.lastIndex)
    return out
}

internal fun isBreak(c: Char): Boolean = c == '\n' || c == '\r' || c in BREAKS

// 탭, 줄 바꾸지 않는 띄어쓰기, 전각 띄어쓰기(U+3000)까지 띄어쓰기로
internal fun isSpace(c: Char): Boolean = !isBreak(c) && c.isWhitespace()

// 줄 구분, 문단 구분, NEL, 세로 탭, 쪽 넘김
private const val BREAKS = "\u2028\u2029\u0085\u000B\u000C"
