package org.sjhstudio.diary.core.model.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.RichSpan
import org.sjhstudio.diary.core.model.RichStyle
import org.sjhstudio.diary.core.model.parseRichText

// 그리는 쪽이 쓰는 글과 저장되는 서식 없는 글이 어긋나지 않는지 본다.
class RichTextParseTest {
    @Test
    fun text_matches_plain_text() {
        for (html in listOf(
            "<p>첫 문단</p><p>둘째<br>줄</p>",
            "<ul><li>하나</li><li>둘</li></ul><p>끝</p>",
            "<p>1 &lt; 2 &amp; 3</p>",
            "글만",
            "<p></p><p>빈 문단 뒤</p>",
            ""
        )) {
            assertEquals(toPlainText(html), parseRichText(html).text)
        }
    }

    @Test
    fun spans_cover_the_styled_text() {
        val html = """<p>가<b>나</b><u>다</u><span style="color:#b91c1c;background-color:#fef08a">라</span></p>"""

        val parsed = parseRichText(html)

        assertEquals("가나다라", parsed.text)
        assertEquals(
            listOf(
                RichSpan(1, 2, RichStyle.Bold),
                RichSpan(2, 3, RichStyle.Underline),
                RichSpan(3, 4, RichStyle.Ink(InkColor.RED)),
                RichSpan(3, 4, RichStyle.Mark(MarkColor.YELLOW))
            ),
            parsed.spans
        )
    }

    @Test
    fun colors_outside_the_palette_are_ignored() {
        assertEquals(emptyList(), parseRichText("""<span style="color:#123456">글</span>""").spans)
    }
}
