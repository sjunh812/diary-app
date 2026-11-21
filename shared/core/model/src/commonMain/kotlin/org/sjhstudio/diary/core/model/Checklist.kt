package org.sjhstudio.diary.core.model

/** 글자 [start]부터 [end] 앞까지를 [text]로 바꾸는 편집. 시작과 끝이 같으면 끼워 넣음 */
data class ChecklistEdit(val start: Int, val end: Int, val text: String) {
    override fun toString(): String = "ChecklistEdit"
}

/** 체크리스트 항목 하나. [start]는 줄의 시작, [label]은 표시를 뺀 글 */
data class ChecklistItem(val start: Int, val done: Boolean, val label: String) {
    override fun toString(): String = "ChecklistItem"
}

/**
 * 체크리스트(contracts/rich-text-html.md "체크리스트"). 줄 맨 앞의 표시 글자로 나타냄
 * 본문 글자라 HTML 허용 목록, 백업 형식, 이전이 그대로
 */
object Checklist {
    const val OPEN: Char = '\u2610'
    const val DONE: Char = '\u2611'

    /** 표시를 칸으로 그리지 않는 미리보기용 글. `☑`이 iOS에서 그림 문자로 나와 바꿈 */
    fun forPreview(text: String): String = text.replace(DONE, CHECK_IN_PREVIEW)

    private const val CHECK_IN_PREVIEW = '\u2713'

    /** 새 항목의 앞머리 */
    const val PREFIX: String = "$OPEN "

    private const val PREFIX_LENGTH = 2

    private val ITEM_IN_HTML = Regex("""(?:<p\b[^>]*>|<br\s*/?>)(?:<[a-z][^>]*>)*([$OPEN$DONE]) """, RegexOption.IGNORE_CASE)

    fun isItem(line: String): Boolean = startsItem(line, 0)

    /** [at]에서 항목 표시가 시작하는지. 줄을 잘라 만들지 않고 봄 */
    fun startsItem(text: CharSequence, at: Int): Boolean = at + 1 < text.length && isMark(text[at]) && text[at + 1] == ' '

    private fun isMark(char: Char) = char == OPEN || char == DONE

    private fun lineStart(text: String, index: Int): Int = text.lastIndexOf('\n', index - 1) + 1

    private fun lineEnd(text: String, index: Int): Int = text.indexOf('\n', index).let { if (it < 0) text.length else it }

    /** 글에 있는 항목들. 순서가 [toggleInHtml]의 번호 */
    fun items(text: String): List<ChecklistItem> {
        val items = ArrayList<ChecklistItem>()
        var start = 0
        for (line in text.split('\n')) {
            if (isItem(line)) items += ChecklistItem(start, line[0] == DONE, line.drop(PREFIX_LENGTH))
            start += line.length + 1
        }
        return items
    }

    /** 고른 줄이 모두 항목인지. 도구 버튼의 켜짐과 체크리스트 도구가 표시를 뺄지 넣을지가 같은 판단 */
    fun picksOnlyItems(text: String, selectionStart: Int, selectionEnd: Int): Boolean =
        pickedLines(text, selectionStart, selectionEnd).all { (_, line) -> isItem(line) }

    /** 표시 글자 하나를 뒤집는 편집. [start]는 항목 줄의 시작 */
    fun toggle(text: String, start: Int): ChecklistEdit =
        ChecklistEdit(start, start + 1, (if (text[start] == OPEN) DONE else OPEN).toString())

    /** [start] 줄이 본문의 몇 번째 항목인지. 항목이 아니면 null */
    fun indexOfItem(text: String, start: Int): Int? {
        if (!isItem(text.substring(start, lineEnd(text, start)))) return null
        return text.take(start).split('\n').dropLast(1).count(::isItem)
    }

    /** 저장된 본문에서 [index]번째 항목을 뒤집음. 그런 항목이 없으면 null */
    fun toggleInHtml(html: String, index: Int): String? {
        val match = ITEM_IN_HTML.findAll(html).elementAtOrNull(index) ?: return null
        // 표시는 맞은 글 끝 공백 바로 앞. 그룹 자리(MatchGroup.range)는 공통 코드에 없음
        val at = match.range.last - 1
        val flipped = if (html[at] == OPEN) DONE else OPEN
        return html.replaceRange(at, at + 1, flipped.toString())
    }

    /**
     * 도구 버튼. 고른 줄이 모두 항목이면 표시를 빼고, 아니면 항목이 아닌 줄에 표시를 넣음
     * 앞 편집이 뒤 자리를 밀지 않게 뒤에서부터 돌려줌
     */
    fun toggleLines(text: String, selectionStart: Int, selectionEnd: Int): List<ChecklistEdit> {
        val allItems = picksOnlyItems(text, selectionStart, selectionEnd)
        return pickedLines(text, selectionStart, selectionEnd).asReversed().mapNotNull { (at, line) ->
            when {
                allItems -> ChecklistEdit(at, at + PREFIX_LENGTH, "")
                isItem(line) -> null
                else -> ChecklistEdit(at, at, PREFIX)
            }
        }
    }

    /** 고른 줄의 표시만 뺌. 글머리 목록으로 바꿀 때 일반 줄에 표시가 생기지 않게 */
    fun removeLines(text: String, selectionStart: Int, selectionEnd: Int): List<ChecklistEdit> =
        pickedLines(text, selectionStart, selectionEnd).asReversed().mapNotNull { (at, line) ->
            ChecklistEdit(at, at + PREFIX_LENGTH, "").takeIf { isItem(line) }
        }

    /**
     * 고른 범위가 걸친 줄의 시작과 글
     * 끝이 다음 줄 맨 앞이면 그 줄은 고르지 않은 것으로 봄(메모 앱과 같게)
     */
    private fun pickedLines(text: String, selectionStart: Int, selectionEnd: Int): List<Pair<Int, String>> {
        val last = if (selectionEnd > selectionStart && text.getOrNull(selectionEnd - 1) == '\n') selectionEnd - 1 else selectionEnd
        val lines = ArrayList<Pair<Int, String>>()
        var start = lineStart(text, selectionStart)
        while (true) {
            val end = lineEnd(text, start)
            lines += start to text.substring(start, end)
            if (end >= last || end >= text.length) break
            start = end + 1
        }
        return lines
    }

    /**
     * [edits]를 차례로 고친 뒤의 [index] 자리. 고르던 범위가 표시를 넣고 빼도 같은 글을 가리키게 함
     * 줄 맨 앞에 표시를 넣으면 그 뒤로 밀고, 빼는 표시 안에 있던 자리는 줄 맨 앞으로 옮김
     */
    fun shifted(index: Int, edits: List<ChecklistEdit>): Int = edits.fold(index) { at, edit ->
        when {
            at >= edit.end -> at + edit.text.length - (edit.end - edit.start)
            at > edit.start -> edit.start
            else -> at
        }
    }

    /**
     * 글이 바뀐 뒤 목록을 이어 갈 편집. [cursor]는 바뀐 뒤 커서 자리
     * 줄바꿈하면 다음 항목을 잇고, 빈 항목에서 줄바꿈하거나 표시 뒤 칸을 지우면 항목을 끝냄
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
