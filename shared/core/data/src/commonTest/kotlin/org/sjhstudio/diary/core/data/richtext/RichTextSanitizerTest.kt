package org.sjhstudio.diary.core.data.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor

// 저장 전 정리(스펙 002 T017, contracts/rich-text-html.md).
class RichTextSanitizerTest {
    private fun clean(html: String) = RichTextSanitizer.sanitize(html)

    @Test
    fun allowed_tags_stay_and_lose_their_attributes() {
        assertEquals("<p>글<br>줄</p><ul><li>하나</li></ul>", clean("""<p class="x" onclick="a()">글<br/>줄</p><ul id="y"><li>하나</li></ul>"""))
    }

    @Test
    fun every_palette_color_survives() {
        InkColor.entries.forEach { ink ->
            val html = """<p><span style="color:${ink.hex}">글</span></p>"""
            assertEquals(html, clean(html))
        }
        MarkColor.entries.forEach { mark ->
            val html = """<p><span style="background-color:${mark.hex}">글</span></p>"""
            assertEquals(html, clean(html))
        }
    }

    @Test
    fun strong_is_stored_as_b() {
        assertEquals("<p><b>굵게</b></p>", clean("<p><strong>굵게</strong></p>"))
    }

    @Test
    fun unknown_tags_lose_the_tag_but_keep_the_text() {
        assertEquals("<p>링크 글</p>", clean("""<p><a href="https://x.test">링크</a> <i>글</i></p>"""))
    }

    @Test
    fun script_style_and_iframe_lose_their_text_too() {
        assertEquals("<p>남음</p>", clean("<script>alert(1)</script><style>p{}</style><iframe src=x>안</iframe><p>남음</p>"))
        assertEquals("<p>남음</p>", clean("<p>남음</p><SCRIPT>끝나지 않음"))
    }

    @Test
    fun palette_colors_stay_and_others_keep_only_the_text() {
        assertEquals("""<span style="color:#b91c1c">빨강</span>""", clean("""<span style="color:#B91C1C">빨강</span>"""))
        assertEquals(
            """<span style="background-color:#fef08a">노랑</span>""",
            clean("""<span style="background-color: #FEF08A;">노랑</span>""")
        )
        assertEquals("다른 색", clean("""<span style="color:#123456">다른 색</span>"""))
        assertEquals("글", clean("""<span class="a">글</span>"""))
    }

    @Test
    fun a_span_keeps_one_color_and_one_highlight() {
        assertEquals(
            """<span style="color:#1d4ed8;background-color:#bbf7d0">둘</span>""",
            clean("""<span style="font-size:20px;color:#1D4ED8;background-color:#BBF7D0;color:#B91C1C">둘</span>""")
        )
    }

    @Test
    fun editor_rgb_values_become_palette_hex() {
        assertEquals("""<span style="color:#b91c1c">글</span>""", clean("""<span style="color: rgba(185, 28, 28, 1);">글</span>"""))
        assertEquals(
            """<span style="background-color:#fbcfe8">글</span>""",
            clean("""<span style="background: rgb(251, 207, 232);">글</span>""")
        )
    }

    @Test
    fun nested_lists_are_flattened_to_one_level() {
        assertEquals("<ul><li>가</li><li>나</li></ul>", clean("<ul><li>가<ul><li>나</li></ul></li></ul>"))
    }

    @Test
    fun broken_markup_is_balanced() {
        assertEquals("<p><b>글</b></p>", clean("<p><b>글"))
        assertEquals("<p>글</p>", clean("</b><p>글</p></u>"))
        assertEquals("<p><b>글</b></p>", clean("<p><b>글</p></b>"))
    }

    @Test
    fun plain_angle_brackets_and_entities_stay_as_text() {
        assertEquals("<p>1 &lt; 2 &amp; 3 &gt; 2</p>", clean("<p>1 < 2 &amp; 3 > 2</p>"))
    }

    @Test
    fun quoted_angle_bracket_does_not_end_the_tag() {
        assertEquals("글", clean("""<x title="a>b">글</x>"""))
    }

    @Test
    fun comments_are_dropped() {
        assertEquals("<p>글</p>", clean("<!-- 숨김 --><p>글</p>"))
    }

    @Test
    fun spaces_are_kept() {
        assertEquals("<p>  두 칸\t탭</p>", clean("<p>  두 칸\t탭</p>"))
    }

    @Test
    fun cleaned_html_is_stable() {
        val once = clean("""<p><strong>a</strong><span style="color: rgb(21, 128, 61)">b</span><x>c</x></p>""")
        assertEquals(once, clean(once))
    }

    @Test
    fun plain_text_of_cleaned_html_matches_the_original_text() {
        val html = """<p><b>굵게</b> 글</p><ul><li>하나</li><li>둘</li></ul>"""
        assertEquals("굵게 글\n\n• 하나\n• 둘", toPlainText(clean(html)))
    }
}
