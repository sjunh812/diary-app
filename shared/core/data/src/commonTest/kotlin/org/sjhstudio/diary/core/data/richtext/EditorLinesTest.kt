package org.sjhstudio.diary.core.data.richtext

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
