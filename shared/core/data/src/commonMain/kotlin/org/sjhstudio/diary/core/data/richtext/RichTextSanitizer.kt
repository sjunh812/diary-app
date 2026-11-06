package org.sjhstudio.diary.core.data.richtext

/**
 * 본문 HTML을 허용 목록만 남기고 정리한다(스펙 002 contracts/rich-text-html.md).
 *
 * - `p`, `br`, `b`(`strong`도 `b`로), `u`, `ul`, `li`만 남기고 속성은 모두 버린다.
 * - `span`은 팔레트 안의 `color`, `background-color`만 남긴다.
 * 남길 것이 없으면 태그를 버리고 글은 남긴다.
 * - 그 밖의 태그는 태그만 버린다.
 * `script`, `style`, `iframe`은 안의 글까지 버린다.
 * - 목록은 한 단계만 둔다.
 * 안쪽 `ul`은 태그만 버린다.
 * - 짝이 맞지 않는 닫는 태그는 버리고, 닫히지 않은 태그는 끝에서 닫는다.
 * - 태그가 아닌 꺾쇠는 `&lt;`로 바꾼다.
 * 글자와 공백은 그대로 둔다.
 */
object RichTextSanitizer {
    /**
     * 라이트 hex 팔레트(소문자).
     * 저장은 이 값만 쓴다.
     */
    val INK: Set<String> = setOf("#b91c1c", "#c2410c", "#15803d", "#1d4ed8", "#6b7280", "#6d28d9", "#be185d")
    val MARK: Set<String> = setOf("#fef08a", "#bbf7d0", "#bfdbfe", "#ddd6fe", "#fbcfe8")

    fun sanitize(html: String): String {
        if (html.isEmpty()) return ""
        val out = StringBuilder(html.length)
        // 열린 태그.
        // 버린 태그는 null로 넣어 닫는 태그와 짝을 맞춘다.
        val open = ArrayList<OpenTag>()
        var i = 0
        while (i < html.length) {
            val c = html[i]
            if (c != '<') {
                if (c == '>') out.append("&gt;") else out.append(c)
                i++
                continue
            }
            if (html.startsWith("<!--", i)) {
                val end = html.indexOf("-->", i + 4)
                i = if (end < 0) html.length else end + 3
                continue
            }
            val end = tagEnd(html, i)
            if (end < 0 || !html.getOrElse(i + 1) { ' ' }.let { it.isLetter() || it == '/' }) {
                out.append("&lt;")
                i++
                continue
            }
            val inside = html.substring(i + 1, end)
            i = end + 1
            val closing = inside.trimStart().startsWith('/')
            val body = inside.trimStart().removePrefix("/").trimStart()
            val name = body.takeWhile { it.isLetterOrDigit() }.lowercase()
            if (name in SKIP_CONTENT) {
                if (!closing) {
                    val close = html.indexOf("</$name", i, ignoreCase = true)
                    i = if (close < 0) html.length else html.indexOf('>', close).takeIf { it >= 0 }?.plus(1) ?: html.length
                }
                continue
            }
            if (closing) {
                closeTag(name, open, out)
                continue
            }
            openTag(name, body, open, out)
        }
        for (k in open.indices.reversed()) open[k].written?.let { out.append("</").append(it).append('>') }
        return out.toString()
    }

    /**
     * `span` 스타일에서 팔레트 값만 뽑는다.
     * 편집기가 쓰는 `rgb()`와 `background`도 받는다.
     */
    internal fun spanStyle(attributes: String): String? {
        val style = styleAttribute(attributes) ?: return null
        var color: String? = null
        var background: String? = null
        for (part in style.split(';')) {
            val colon = part.indexOf(':')
            if (colon < 0) continue
            val property = part.substring(0, colon).trim().lowercase()
            val value = hexOf(part.substring(colon + 1).trim()) ?: continue
            when (property) {
                "color" -> if (color == null && value in INK) color = value
                "background-color", "background" -> if (background == null && value in MARK) background = value
            }
        }
        val parts = listOfNotNull(color?.let { "color:$it" }, background?.let { "background-color:$it" })
        return parts.takeIf { it.isNotEmpty() }?.joinToString(";")
    }

    private fun openTag(name: String, body: String, open: MutableList<OpenTag>, out: StringBuilder) {
        when (name) {
            "br" -> out.append("<br>")

            "p", "u" -> push(name, name, open, out)

            "li" -> {
                // 안쪽 목록을 한 단계로 펴면 항목이 겹치므로 앞 항목을 닫는다.
                closeTag(name, open, out)
                push(name, name, open, out)
            }

            "b", "strong" -> push(name, "b", open, out)

            "ul" -> if (open.any { it.written == "ul" }) open += OpenTag(name, null) else push(name, "ul", open, out)

            "span" -> {
                val style = spanStyle(body.drop(name.length))
                if (style == null) {
                    open += OpenTag(name, null)
                } else {
                    out.append("<span style=\"").append(style).append("\">")
                    open += OpenTag(name, "span")
                }
            }

            else -> Unit
        }
    }

    private fun push(name: String, written: String, open: MutableList<OpenTag>, out: StringBuilder) {
        out.append('<').append(written).append('>')
        open += OpenTag(name, written)
    }

    private fun closeTag(name: String, open: MutableList<OpenTag>, out: StringBuilder) {
        val at = open.indexOfLast { it.name == name || (name == "strong" && it.name == "b") || (name == "b" && it.name == "strong") }
        if (at < 0) return
        // 안쪽에 닫히지 않은 태그가 있으면 함께 닫는다.
        for (k in open.lastIndex downTo at) {
            open[k].written?.let { out.append("</").append(it).append('>') }
            open.removeAt(k)
        }
    }

    /** 따옴표 안의 `>`는 건너뛰고 태그 끝을 찾는다. */
    private fun tagEnd(html: String, start: Int): Int {
        var quote: Char? = null
        var i = start + 1
        while (i < html.length) {
            val c = html[i]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return i
                c == '<' -> return -1
            }
            i++
        }
        return -1
    }

    private fun styleAttribute(attributes: String): String? {
        val match = STYLE.find(attributes) ?: return null
        return match.groupValues[1].ifEmpty { match.groupValues[2] }
    }

    /** `#RRGGBB`, `#RGB`, `rgb(r, g, b)`, `rgba(r, g, b, 1)`을 소문자 `#rrggbb`로 바꾼다. */
    private fun hexOf(value: String): String? {
        val lower = value.lowercase().removeSuffix("!important").trim()
        if (lower.startsWith('#')) {
            return when (lower.length) {
                7 -> lower
                4 -> "#" + lower.drop(1).map { "$it$it" }.joinToString("")
                else -> null
            }
        }
        val numbers = RGB.matchEntire(lower)?.groupValues?.get(1)?.split(',')?.map { it.trim() } ?: return null
        if (numbers.size < 3) return null
        val channels = numbers.take(3).map { it.toFloatOrNull()?.toInt() ?: return null }
        return "#" + channels.joinToString("") { it.coerceIn(0, 255).toString(16).padStart(2, '0') }
    }

    private class OpenTag(val name: String, val written: String?)

    private val SKIP_CONTENT = setOf("script", "style", "iframe")
    private val STYLE = Regex("""style\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
    private val RGB = Regex("""rgba?\(([^)]*)\)""")
}
