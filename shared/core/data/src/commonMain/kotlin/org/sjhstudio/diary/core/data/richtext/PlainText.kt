package org.sjhstudio.diary.core.data.richtext

import org.sjhstudio.diary.core.model.decodeHtmlEntity

/**
 * 본문 HTML을 서식 없는 글로 바꾼다(스펙 002 contracts/rich-text-html.md "서식 없는 글 만들기").
 *
 * - 문단(`p`)과 목록(`ul`) 같은 덩어리 사이는 빈 줄 하나(`"\n\n"`)다.
 * - `br`과 `li` 사이는 줄바꿈 하나, `li` 앞에는 `• `를 붙인다.
 * - 엔티티는 글자로 풀고, 공백·탭·`\r`은 줄이거나 바꾸지 않는다.
 * - 허용 목록 밖의 태그는 태그만 버리고 안의 글은 남긴다.
 * `script`, `style`, `iframe`은 안의 글까지 버린다.
 *
 * 레거시 글은 `toPlainText(legacyToHtml(c)) == c`여야 한다(스펙 001 contracts/legacy-mapping.md).
 */
fun toPlainText(html: String): String {
    if (html.isEmpty()) return ""
    val out = StringBuilder(html.length)
    var blocks = 0
    var itemsInList = 0
    var i = 0
    while (i < html.length) {
        val c = html[i]
        if (c == '<') {
            val end = html.indexOf('>', i)
            if (end < 0) {
                // 닫히지 않은 꺾쇠는 글자로 둔다.
                out.append(html, i, html.length)
                break
            }
            val tag = parseTag(html.substring(i + 1, end))
            i = end + 1
            when {
                tag.name in SKIP_CONTENT && !tag.closing -> {
                    val close = html.indexOf("</${tag.name}", i, ignoreCase = true)
                    i = if (close < 0) html.length else (html.indexOf('>', close).takeIf { it >= 0 }?.plus(1) ?: html.length)
                }

                tag.closing -> Unit

                tag.name == "p" || tag.name == "ul" -> {
                    if (blocks > 0) out.append("\n\n")
                    blocks++
                    if (tag.name == "ul") itemsInList = 0
                }

                tag.name == "li" -> {
                    if (itemsInList > 0) out.append('\n')
                    out.append("• ")
                    itemsInList++
                }

                tag.name == "br" -> out.append('\n')
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
            // 덩어리 밖에 놓인 글도 덩어리 하나로 센다.
            if (blocks == 0) blocks = 1
            out.append(c)
            i++
        }
    }
    return out.toString()
}

private val SKIP_CONTENT = setOf("script", "style", "iframe")

private class Tag(val name: String, val closing: Boolean)

private fun parseTag(inside: String): Tag {
    val trimmed = inside.trim()
    val closing = trimmed.startsWith('/')
    val body = trimmed.removePrefix("/").trimStart()
    val name = body.takeWhile { it.isLetterOrDigit() }.lowercase()
    return Tag(name, closing)
}
