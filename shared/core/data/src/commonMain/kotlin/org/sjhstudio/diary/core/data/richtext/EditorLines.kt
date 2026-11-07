package org.sjhstudio.diary.core.data.richtext

// 에디터는 줄마다 문단 하나로 다루고, 저장 형식은 빈 줄로 나뉜 덩어리를 문단으로 삼고 줄바꿈은 `br`이다(contracts/rich-text-html.md).
// 옛 글(`legacyToHtml`)과 같은 규칙으로 맞추려고 에디터를 오갈 때만 바꾼다.
// 그래야 줄바꿈 한 번이 읽기 화면에서 빈 줄로 보이지 않는다.
//
// 에디터가 낸 것은 아는 꼴이 아니어도 버리지 않는다.
// 줄 맨 앞에 `1. `을 치면 에디터가 스스로 번호 목록을 만들고, 붙여넣기도 다른 태그를 들여온다.

private val OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
private val BLOCK = Regex("""<p\b[^>]*>(.*?)</p>|<(?:ul|ol)\b[^>]*>|<br\s*/?>""", OPTIONS)
private val BREAK = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
private val LIST_EDGE = Regex("""<(/?)(?:ul|ol)\b[^>]*>""", RegexOption.IGNORE_CASE)
private val LIST_ITEM = Regex("""<li\b[^>]*>""", RegexOption.IGNORE_CASE)
private val LIST_TAGS = Regex("""</?(?:ul|ol|li)\b[^>]*>""", RegexOption.IGNORE_CASE)
private val ANY_TAG = Regex("""<[^>]*>""")
private val LIST_MARK = Regex("""^\s*(?:•|\d+\.)(?:\s|$)""")

private sealed interface Block {
    /** 문단 하나나 목록 밖 글. */
    data class Line(val html: String) : Block

    data class Bullets(val html: String) : Block

    data class Numbers(val html: String) : Block

    data object Blank : Block
}

/** 안쪽 목록까지 세어 [start]에서 열린 목록이 끝나는 자리를 찾는다. */
private fun listEnd(html: String, start: Int): Int {
    var depth = 0
    for (edge in LIST_EDGE.findAll(html, start)) {
        depth += if (edge.groupValues[1].isEmpty()) 1 else -1
        if (depth == 0) return edge.range.last + 1
    }
    return html.length
}

private fun blocksOf(html: String): List<Block> {
    val blocks = ArrayList<Block>()
    fun loose(from: Int, to: Int) {
        val text = html.substring(from, to)
        if (text.replace(ANY_TAG, "").isNotBlank()) blocks += Block.Line(text.trim())
    }
    var at = 0
    while (at < html.length) {
        val block = BLOCK.find(html, at) ?: break
        loose(at, block.range.first)
        val value = block.value
        at =
            when {
                value.startsWith("<ul", ignoreCase = true) || value.startsWith("<ol", ignoreCase = true) -> {
                    val end = listEnd(html, block.range.first)
                    val list = html.substring(block.range.first, end)
                    blocks += if (value.startsWith("<ul", ignoreCase = true)) Block.Bullets(list) else Block.Numbers(list)
                    end
                }

                value.startsWith("<br", ignoreCase = true) -> {
                    blocks += Block.Blank
                    block.range.last + 1
                }

                else -> {
                    blocks += Block.Line(block.groupValues[1])
                    block.range.last + 1
                }
            }
    }
    loose(at, html.length)
    return blocks
}

/**
 * 번호 목록은 저장 형식에 없으므로 쓴 그대로 `1. ` 글자가 붙은 줄로 남긴다.
 * 안쪽 목록의 항목도 차례대로 잇는다.
 */
private fun numberedLines(list: String): List<String> =
    list.split(LIST_ITEM).drop(1).map { it.replace(LIST_TAGS, "").trim() }.filter { it.isNotEmpty() }.mapIndexed { index, item ->
        "${index + 1}. $item"
    }

/**
 * 에디터가 내놓은 HTML을 저장 형식으로 바꾼다.
 * 빈 줄 없이 이어진 줄은 한 문단 안의 `br`이 된다.
 */
fun editorToBody(html: String): String {
    val out = StringBuilder()
    val lines = ArrayList<String>()
    fun flush() {
        if (lines.isEmpty()) return
        for (chunk in lines.joinToString("\n").split("\n\n")) out.append("<p>").append(chunk.replace("\n", "<br>")).append("</p>")
        lines.clear()
    }
    for (block in blocksOf(html)) {
        when (block) {
            is Block.Bullets -> {
                flush()
                out.append(block.html)
            }

            is Block.Numbers -> lines += numberedLines(block.html)

            Block.Blank -> lines += ""

            is Block.Line -> lines += block.html.takeUnless { BREAK.matches(it.trim()) }.orEmpty()
        }
    }
    flush()
    return out.toString()
}

/**
 * 저장 형식을 에디터에 넣을 HTML로 바꾼다.
 * 문단 안의 `br`은 줄로, 문단 사이는 빈 줄로 편다.
 */
fun bodyToEditor(html: String): String {
    val out = StringBuilder()
    val chunks = ArrayList<String>()
    fun flush() {
        if (chunks.isEmpty()) return
        for (line in chunks.joinToString("\n\n").split("\n")) out.append("<p>").append(line).append("</p>")
        chunks.clear()
    }
    for (block in blocksOf(html)) {
        when (block) {
            is Block.Bullets -> {
                flush()
                out.append(block.html)
            }

            is Block.Numbers -> chunks += numberedLines(block.html).joinToString("\n")

            Block.Blank -> chunks += ""

            is Block.Line -> chunks += block.html.replace(BREAK, "\n")
        }
    }
    flush()
    return out.toString()
}

/**
 * 에디터 글에 실제로 쓴 글자가 있는가.
 * 목록 버튼만 눌러 표시만 있는 줄은 쓴 글이 아니다.
 */
fun hasWords(editorText: String): Boolean = editorText.lineSequence().any { it.replace(LIST_MARK, "").isNotBlank() }

/**
 * 변환 결과가 비었는데 에디터에는 글이 있으면 보이는 글을 그대로 저장 형식으로 만든다.
 * 앞으로 라이브러리가 낯선 꼴을 내놓아도 사용자의 글은 남아야 한다(헌법 I).
 */
fun keepingEditorText(body: String, editorText: String): String {
    if (toPlainText(RichTextSanitizer.sanitize(body)).isNotBlank() || !hasWords(editorText)) return body
    val escaped = editorText.trim().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return escaped.split("\n\n").joinToString("") { "<p>" + it.replace("\n", "<br>") + "</p>" }
}
