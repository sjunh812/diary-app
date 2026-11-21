package org.sjhstudio.diary.core.model.richtext

import kotlin.test.Test
import kotlin.test.assertEquals

/** 다른 앱은 붙여넣을 때 문단·목록 사이를 빈 줄로 바꿔, 복사용 HTML은 줄을 `br`로만 이음 */
class ClipboardHtmlTest {
    @Test
    fun lines_join_with_one_break() = assertEquals("a<br>b", editorToClipboard("<p>a</p><p>b</p>"))

    @Test
    fun an_empty_line_stays_one_empty_line() = assertEquals("a<br><br>b", editorToClipboard("<p>a</p><br><p>b</p>"))

    @Test
    fun breaks_inside_a_paragraph_stay() = assertEquals("a<br>b<br><br>c", editorToClipboard("<p>a<br>b</p><br><p>c</p>"))

    @Test
    fun styles_stay_on_their_line() = assertEquals(
        "<b>굵게</b> 글<br><span style=\"color: rgba(185, 28, 28, 1.0);\">빨강</span>",
        editorToClipboard("<p><b>굵게</b> 글</p><p><span style=\"color: rgba(185, 28, 28, 1.0);\">빨강</span></p>")
    )

    @Test
    fun list_items_become_marked_lines() {
        assertEquals("a<br>• 가<br>• <b>나</b><br>b", editorToClipboard("<p>a</p><ul><li>가</li><li><b>나</b></li></ul><p>b</p>"))
        assertEquals("1. 가<br>2. 나", editorToClipboard("<ol><li>가</li><li>나</li></ol>"))
    }

    @Test
    fun leading_and_trailing_empty_lines_stay() = assertEquals("<br>가운데<br>", editorToClipboard("<br><p>가운데</p><br>"))

    @Test
    fun no_block_tags_are_left() {
        val html = editorToClipboard("<p>a</p><br><ul><li>x</li></ul><ol><li>y</li></ol><p>b<br>c</p>")
        assertEquals(null, Regex("</?(p|ul|ol|li)\\b").find(html))
        assertEquals("a\n\n• x\n1. y\nb\nc", toPlainText(html))
    }
}
