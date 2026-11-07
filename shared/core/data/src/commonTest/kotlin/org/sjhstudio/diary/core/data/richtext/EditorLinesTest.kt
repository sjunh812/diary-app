package org.sjhstudio.diary.core.data.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorLinesTest {
    @Test
    fun one_enter_is_a_line_inside_a_paragraph() = assertEquals("<p>a<br>b</p>", editorToBody("<p>a</p><p>b</p>"))

    @Test
    fun blank_line_splits_paragraphs() {
        assertEquals("<p>a</p><p>b</p>", editorToBody("<p>a</p><p></p><p>b</p>"))
        assertEquals("<p>a</p><p>b</p>", editorToBody("<p>a</p><br><p>b</p>"))
        assertEquals("<p>a</p><p>b</p>", editorToBody("<p>a</p><p><br></p><p>b</p>"))
    }

    @Test
    fun styles_stay_inside_their_line() =
        assertEquals("<p><b>a</b><br><span>b</span></p>", editorToBody("<p><b>a</b></p><p><span>b</span></p>"))

    @Test
    fun lists_stay_as_blocks() = assertEquals("<p>a</p><ul><li>x</li></ul><p>b</p>", editorToBody("<p>a</p><ul><li>x</li></ul><p>b</p>"))

    @Test
    fun saved_body_opens_as_lines() {
        assertEquals("<p>a</p><p>b</p>", bodyToEditor("<p>a<br>b</p>"))
        assertEquals("<p>a</p><p></p><p>b</p>", bodyToEditor("<p>a</p><p>b</p>"))
    }

    @Test
    fun round_trip_keeps_saved_body() {
        for (body in listOf("<p>a</p>", "<p>a<br>b</p>", "<p>a</p><p>b</p>", "<p>a</p><p></p><p>b</p>", "<p>a</p><p><br>b</p>")) {
            assertEquals(body, editorToBody(bodyToEditor(body)))
        }
    }

    @Test
    fun plain_text_matches_what_was_typed() {
        assertEquals("a\nb", toPlainText(editorToBody("<p>a</p><p>b</p>")))
        assertEquals("a\n\nb", toPlainText(editorToBody("<p>a</p><p></p><p>b</p>")))
    }

    @Test
    fun a_numbered_list_becomes_numbered_lines() {
        assertEquals("<p>1. 할 일<br>2. 운동</p>", editorToBody("<ol><li>할 일</li><li>운동</li></ol>"))
    }

    @Test
    fun a_numbered_list_between_paragraphs_keeps_every_line() {
        assertEquals(
            "<p>앞줄<br>1. 하나<br>2. 둘</p><p>뒷줄</p>",
            editorToBody("<p>앞줄</p><ol><li>하나</li><li>둘</li></ol><p></p><p>뒷줄</p>")
        )
    }

    @Test
    fun a_numbered_list_keeps_its_styles() {
        assertEquals("<p>1. <b>가</b></p>", editorToBody("<ol><li><b>가</b></li></ol>"))
    }

    @Test
    fun a_nested_list_keeps_the_item_after_it() {
        val body = editorToBody("<ul><li>가<ul><li>나</li></ul></li><li>다</li></ul>")
        assertEquals("• 가\n• 나\n• 다", toPlainText(RichTextSanitizer.sanitize(body)))
    }

    @Test
    fun a_nested_numbered_list_keeps_every_item() {
        val body = editorToBody("<ol><li>가<ol><li>나</li></ol></li><li>다</li></ol>")
        assertEquals("1. 가\n2. 나\n3. 다", toPlainText(RichTextSanitizer.sanitize(body)))
    }

    @Test
    fun text_outside_known_blocks_stays() {
        assertEquals("<p>가<br>나<br>다</p>", RichTextSanitizer.sanitize(editorToBody("<p>가</p><h1>나</h1><p>다</p>")))
        assertEquals("<p>맨 글자</p>", editorToBody("맨 글자"))
    }

    @Test
    fun an_unclosed_list_keeps_its_text() {
        assertEquals("가\n1. 나", toPlainText(RichTextSanitizer.sanitize(editorToBody("<p>가</p><ol><li>나"))))
    }

    @Test
    fun nothing_the_editor_shows_is_lost() {
        val blocks = listOf("<p>문단</p>", "<p></p>", "<ul><li>글머리</li></ul>", "<ol><li>번호</li></ol>", "<p><b>굵게</b> 😀</p>")
        for (a in blocks) {
            for (b in blocks) {
                for (c in blocks) {
                    val html = a + b + c
                    val saved = toPlainText(RichTextSanitizer.sanitize(editorToBody(html)))
                    for (word in Regex("[가-힣]+|😀").findAll(html.replace(Regex("<[^>]+>"), " "))) {
                        assertTrue(word.value in saved, "$html 에서 ${word.value} 사라짐: $saved")
                    }
                }
            }
        }
    }

    @Test
    fun a_list_mark_alone_is_not_writing() {
        assertFalse(hasWords("• "))
        assertFalse(hasWords("1. \n2. "))
        assertFalse(hasWords("  \n"))
        assertTrue(hasWords("• 가"))
        assertTrue(hasWords("1.5배 늘었다"))
        assertTrue(hasWords("☐ "))
    }

    @Test
    fun an_empty_conversion_falls_back_to_what_the_editor_shows() {
        assertEquals("<p>가<br>나</p>", keepingEditorText("", "가\n나"))
        assertEquals("<p>&lt;가&gt; &amp;</p>", keepingEditorText("<p></p>", "<가> &"))
        assertEquals("<p>본문</p>", keepingEditorText("<p>본문</p>", "다른 글"))
        assertEquals("", keepingEditorText("", "• "))
    }
}
