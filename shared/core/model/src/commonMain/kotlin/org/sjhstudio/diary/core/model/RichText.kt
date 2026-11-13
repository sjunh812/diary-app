package org.sjhstudio.diary.core.model

import org.sjhstudio.diary.core.model.richtext.blockGap

/** 글자색 팔레트(contracts/rich-text-html.md). [hex]는 저장 값, 그릴 때는 테마 토큰을 씀 */
enum class InkColor(val hex: String) {
    GRAY("#6b7280"),
    RED("#b91c1c"),
    ORANGE("#c2410c"),
    GREEN("#15803d"),
    BLUE("#1d4ed8"),
    PURPLE("#6d28d9"),
    PINK("#be185d");

    companion object {
        fun fromHex(hex: String): InkColor? = entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) }
    }
}

/** 형광펜 팔레트 */
enum class MarkColor(val hex: String) {
    YELLOW("#fef08a"),
    GREEN("#bbf7d0"),
    BLUE("#bfdbfe"),
    PURPLE("#ddd6fe"),
    PINK("#fbcfe8");

    companion object {
        fun fromHex(hex: String): MarkColor? = entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) }
    }
}

sealed interface RichStyle {
    data object Bold : RichStyle

    data object Underline : RichStyle

    data class Ink(val color: InkColor) : RichStyle

    data class Mark(val color: MarkColor) : RichStyle
}

/** [start]부터 [end] 앞까지의 서식 */
data class RichSpan(val start: Int, val end: Int, val style: RichStyle)

/** 본문을 그리려고 푼 값. [text]는 서식 없는 글과 같음. 일기 내용이라 [toString]에 담지 않음 */
data class RichText(val text: String, val spans: List<RichSpan>) {
    override fun toString(): String = "RichText"
}

/**
 * 정리된 본문 HTML을 글과 서식으로 분리
 * 줄바꿈 규칙은 서식 없는 글(contracts/rich-text-html.md)과 같고, 모르는 태그는 글만 남김
 * 그리는 쪽은 이 값만 쓰고 HTML을 직접 다루지 않음
 */
fun parseRichText(html: String): RichText {
    val out = StringBuilder(html.length)
    val spans = ArrayList<RichSpan>()
    val open = ArrayList<Open>()
    var blocks = 0
    var items = 0
    var lastBlock = ""
    var i = 0
    while (i < html.length) {
        val c = html[i]
        if (c == '<') {
            val end = html.indexOf('>', i)
            if (end < 0) {
                out.append(html, i, html.length)
                break
            }
            val inside = html.substring(i + 1, end).trim()
            i = end + 1
            val closing = inside.startsWith('/')
            val body = inside.removePrefix("/").trimStart()
            val name = body.takeWhile { it.isLetterOrDigit() }.lowercase()
            when {
                closing -> {
                    val at = open.indexOfLast { it.name == name }
                    if (at >= 0) {
                        val tag = open.removeAt(at)
                        for (style in tag.styles) if (out.length > tag.start) spans += RichSpan(tag.start, out.length, style)
                    }
                }

                name == "p" || name == "ul" -> {
                    if (blocks > 0) out.append(blockGap(lastBlock, name))
                    blocks++
                    lastBlock = name
                    if (name == "ul") items = 0
                }

                name == "li" -> {
                    if (items > 0) out.append('\n')
                    out.append("• ")
                    items++
                }

                name == "br" -> out.append('\n')

                name == "b" || name == "strong" -> open += Open(name, out.length, listOf(RichStyle.Bold))

                name == "u" -> open += Open(name, out.length, listOf(RichStyle.Underline))

                name == "span" -> open += Open(name, out.length, spanStyles(body))
            }
        } else if (c == '&') {
            val decoded = decodeHtmlEntity(html, i)
            if (decoded == null) {
                out.append(c)
                i++
            } else {
                if (blocks == 0) blocks = 1
                out.append(decoded.first)
                i = decoded.second
            }
        } else {
            if (blocks == 0) blocks = 1
            out.append(c)
            i++
        }
    }
    return RichText(out.toString(), spans.sortedBy { it.start })
}

private class Open(val name: String, val start: Int, val styles: List<RichStyle>)

private val HEX = Regex("#[0-9a-fA-F]{6}")

private fun spanStyles(tag: String): List<RichStyle> {
    val styles = ArrayList<RichStyle>(2)
    for (part in tag.substringAfter("style=", "").trim('"', '\'').split(';')) {
        val hex = HEX.find(part)?.value ?: continue
        if (part.trimStart('"', '\'', ' ').startsWith("background")) {
            MarkColor.fromHex(hex)?.let { styles += RichStyle.Mark(it) }
        } else {
            InkColor.fromHex(hex)?.let { styles += RichStyle.Ink(it) }
        }
    }
    return styles
}

/** `&...;`를 풀어 (글자, 다음 위치)를 돌려줌. 모르는 엔티티면 null */
fun decodeHtmlEntity(html: String, start: Int): Pair<String, Int>? {
    val semi = html.indexOf(';', start)
    if (semi < 0 || semi - start > 12) return null
    val name = html.substring(start + 1, semi)
    val text =
        when {
            name == "amp" -> "&"
            name == "lt" -> "<"
            name == "gt" -> ">"
            name == "quot" -> "\""
            name == "#39" || name == "apos" -> "'"
            name == "nbsp" -> "\u00A0"
            name.startsWith("#x") || name.startsWith("#X") -> name.drop(2).toIntOrNull(16)?.let(::codePointToString)
            name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePointToString)
            else -> null
        } ?: return null
    return text to semi + 1
}

private fun codePointToString(codePoint: Int): String? {
    if (codePoint < 0 || codePoint > 0x10FFFF || codePoint in 0xD800..0xDFFF) return null
    return if (codePoint < 0x10000) {
        codePoint.toChar().toString()
    } else {
        val v = codePoint - 0x10000
        charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
    }
}
