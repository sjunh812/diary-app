package org.sjhstudio.diary.core.model

/**
 * 글자 [start]부터 [end] 앞까지를 [text]로 바꾸는 편집.
 * 시작과 끝이 같으면 끼워 넣는다.
 */
data class ChecklistEdit(val start: Int, val end: Int, val text: String) {
    override fun toString(): String = "ChecklistEdit"
}

/**
 * 체크리스트 항목 하나.
 * [start]는 줄의 시작이고 [label]은 표시를 뺀 글이다.
 */
data class ChecklistItem(val start: Int, val done: Boolean, val label: String) {
    override fun toString(): String = "ChecklistItem"
}

/**
 * 체크리스트(contracts/rich-text-html.md "체크리스트").
 * 줄 맨 앞의 표시 글자로 나타낸다.
 * 본문 글자라서 HTML 허용 목록, 백업 형식, 이전이 그대로다.
 */
object Checklist {
    const val OPEN: Char = '\u2610'
    const val DONE: Char = '\u2611'

    /**
     * 표시를 칸으로 그리지 않는 미리보기용 글.
     * `☑`이 iOS에서 그림 문자로 나와서 바꾼다.
     */
    fun forPreview(text: String): String = text.replace(DONE, CHECK_IN_PREVIEW)

    private const val CHECK_IN_PREVIEW = '\u2713'

    /** 새 항목의 앞머리. */
    const val PREFIX: String = "$OPEN "

    private const val PREFIX_LENGTH = 2

    private val ITEM_IN_HTML = Regex("""(?:<p\b[^>]*>|<br\s*/?>)(?:<[a-z][^>]*>)*([$OPEN$DONE]) """, RegexOption.IGNORE_CASE)

    fun isItem(line: String): Boolean = line.length >= PREFIX_LENGTH && isMark(line[0]) && line[1] == ' '

    private fun isMark(char: Char) = char == OPEN || char == DONE

    private fun lineStart(text: String, index: Int): Int = text.lastIndexOf('\n', index - 1) + 1

    private fun lineEnd(text: String, index: Int): Int = text.indexOf('\n', index).let { if (it < 0) text.length else it }

    /**
     * 글에 있는 항목들.
     * 순서가 [toggleInHtml]의 번호다.
     */
    fun items(text: String): List<ChecklistItem> {
        val items = ArrayList<ChecklistItem>()
        var start = 0
        for (line in text.split('\n')) {
            if (isItem(line)) items += ChecklistItem(start, line[0] == DONE, line.drop(PREFIX_LENGTH))
            start += line.length + 1
        }
        return items
    }

    /** 커서가 있는 줄이 항목인가. */
    fun isItemAt(text: String, cursor: Int): Boolean = isItem(text.substring(lineStart(text, cursor), lineEnd(text, cursor)))

    /**
     * 표시 글자 하나를 뒤집는 편집.
     * [start]는 항목 줄의 시작이다.
     */
    fun toggle(text: String, start: Int): ChecklistEdit =
        ChecklistEdit(start, start + 1, (if (text[start] == OPEN) DONE else OPEN).toString())

    /**
     * [start] 줄이 본문의 몇 번째 항목인지.
     * 항목이 아니면 null이다.
     */
    fun indexOfItem(text: String, start: Int): Int? {
        if (!isItem(text.substring(start, lineEnd(text, start)))) return null
        return text.take(start).split('\n').dropLast(1).count(::isItem)
    }

    /**
     * 저장된 본문에서 [index]번째 항목을 뒤집는다.
     * 그런 항목이 없으면 null이다.
     */
    fun toggleInHtml(html: String, index: Int): String? {
        val mark = ITEM_IN_HTML.findAll(html).elementAtOrNull(index)?.groups?.get(1) ?: return null
        val flipped = if (mark.value.single() == OPEN) DONE else OPEN
        return html.replaceRange(mark.range, flipped.toString())
    }

    /**
     * 도구 버튼.
     * 고른 줄이 모두 항목이면 표시를 빼고, 아니면 항목이 아닌 줄에 표시를 넣는다.
     * 앞 편집이 뒤 자리를 밀지 않게 뒤에서부터 돌려준다.
     */
    fun toggleLines(text: String, selectionStart: Int, selectionEnd: Int): List<ChecklistEdit> {
        val starts = ArrayList<Int>()
        var start = lineStart(text, selectionStart)
        while (true) {
            starts += start
            val end = lineEnd(text, start)
            if (end >= selectionEnd || end >= text.length) break
            start = end + 1
        }
        val lines = starts.map { it to text.substring(it, lineEnd(text, it)) }
        val allItems = lines.all { (_, line) -> isItem(line) }
        return lines.asReversed().mapNotNull { (at, line) ->
            when {
                allItems -> ChecklistEdit(at, at + PREFIX_LENGTH, "")
                isItem(line) -> null
                else -> ChecklistEdit(at, at, PREFIX)
            }
        }
    }

    /**
     * 글이 바뀐 뒤 목록을 이어 갈 편집.
     * [cursor]는 바뀐 뒤 커서 자리다.
     * 줄바꿈하면 다음 항목을 잇고, 빈 항목에서 줄바꿈하거나 표시 뒤 칸을 지우면 항목을 끝낸다.
     */
    fun afterEdit(before: String, after: String, cursor: Int): ChecklistEdit? = when {
        after.length == before.length + 1 && cursor > 0 && after[cursor - 1] == '\n' -> {
            val start = lineStart(after, cursor - 1)
            val line = after.substring(start, cursor - 1)
            when {
                !isItem(line) -> null
                line.length == PREFIX_LENGTH -> ChecklistEdit(start, cursor, "")
                else -> ChecklistEdit(cursor, cursor, PREFIX)
            }
        }

        after.length == before.length - 1 && cursor > 0 -> {
            val start = lineStart(after, cursor)
            val line = after.substring(start, lineEnd(after, cursor))
            if (line.length == 1 && isMark(line[0])) ChecklistEdit(start, start + 1, "") else null
        }

        else -> null
    }
}
