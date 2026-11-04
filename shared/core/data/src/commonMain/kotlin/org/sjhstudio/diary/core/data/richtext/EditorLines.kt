package org.sjhstudio.diary.core.data.richtext

// 에디터는 줄마다 문단 하나로 다루고, 저장 형식은 빈 줄로 나뉜 덩어리를 문단으로 삼고 줄바꿈은 `br`이다(contracts/rich-text-html.md).
// 옛 글(`legacyToHtml`)과 같은 규칙으로 맞추려고 에디터를 오갈 때만 바꾼다. 그래야 줄바꿈 한 번이 읽기 화면에서 빈 줄로 보이지 않는다.

private val BLOCK =
    Regex("""<p\b[^>]*>(.*?)</p>|<ul\b[^>]*>.*?</ul>|<br\s*/?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val BREAK = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

/** 에디터가 내놓은 HTML을 저장 형식으로 바꾼다. 빈 줄 없이 이어진 줄은 한 문단 안의 `br`이 된다. */
fun editorToBody(html: String): String {
    val out = StringBuilder()
    val lines = ArrayList<String>()
    fun flush() {
        if (lines.isEmpty()) return
        for (chunk in lines.joinToString("\n").split("\n\n")) out.append("<p>").append(chunk.replace("\n", "<br>")).append("</p>")
        lines.clear()
    }
    for (block in BLOCK.findAll(html)) {
        val value = block.value
        when {
            value.startsWith("<ul", ignoreCase = true) -> {
                flush()
                out.append(value)
            }

            value.startsWith("<br", ignoreCase = true) -> lines += ""

            else -> lines += block.groupValues[1].takeUnless { BREAK.matches(it.trim()) }.orEmpty()
        }
    }
    flush()
    return out.toString()
}

/** 저장 형식을 에디터에 넣을 HTML로 바꾼다. 문단 안의 `br`은 줄로, 문단 사이는 빈 줄로 편다. */
fun bodyToEditor(html: String): String {
    val out = StringBuilder()
    val chunks = ArrayList<String>()
    fun flush() {
        if (chunks.isEmpty()) return
        for (line in chunks.joinToString("\n\n").split("\n")) out.append("<p>").append(line).append("</p>")
        chunks.clear()
    }
    for (block in BLOCK.findAll(html)) {
        val value = block.value
        when {
            value.startsWith("<ul", ignoreCase = true) -> {
                flush()
                out.append(value)
            }

            value.startsWith("<br", ignoreCase = true) -> chunks += ""

            else -> chunks += block.groupValues[1].replace(BREAK, "\n")
        }
    }
    flush()
    return out.toString()
}
