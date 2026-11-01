package org.sjhstudio.diary.core.data.richtext

import kotlin.test.Test
import kotlin.test.assertEquals

class PlainTextTest {
    @Test
    fun blocks_lines_and_list_items() {
        assertEquals("", toPlainText(""))
        assertEquals("a\n\nb", toPlainText("<p>a</p><p>b</p>"))
        assertEquals("a\nb", toPlainText("<p>a<br>b</p>"))
        assertEquals("앞\n\n• 하나\n• 둘\n\n뒤", toPlainText("<p>앞</p><ul><li>하나</li><li>둘</li></ul><p>뒤</p>"))
    }

    @Test
    fun formatting_tags_keep_text_and_scripts_drop_it() {
        assertEquals("굵게 밑줄 빨강", toPlainText("<p><b>굵게</b> <u>밑줄</u> <span style=\"color:#B91C1C\">빨강</span></p>"))
        assertEquals("남김", toPlainText("<p><script>alert(1)</script>남김<style>p{}</style></p>"))
        assertEquals("모르는 태그 글", toPlainText("<p><em>모르는</em> 태그 글</p>"))
    }

    @Test
    fun entities_are_decoded_and_whitespace_is_kept() {
        assertEquals("&<>\"' \u00A0😀가", toPlainText("<p>&amp;&lt;&gt;&quot;&#39; &nbsp;&#x1F600;&#44032;</p>"))
        assertEquals("  두 칸\t탭\r\n", toPlainText("<p>  두 칸\t탭\r<br></p>"))
        assertEquals("a & b", toPlainText("<p>a & b</p>"))
    }
}
