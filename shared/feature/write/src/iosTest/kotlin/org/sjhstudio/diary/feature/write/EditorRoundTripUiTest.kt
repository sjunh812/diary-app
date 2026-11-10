@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.parseRichText
import org.sjhstudio.diary.core.model.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.model.richtext.toPlainText

// 편집기를 지나도 서식이 그대로 남는지(FR-008)
// 편집기는 라이브러리라 내보내는 모양이 저장 규칙을 지나는지가 위험함
@OptIn(ExperimentalTestApi::class)
class EditorRoundTripUiTest {
    private val body =
        "<p><b>굵게</b> 글 <u>밑줄</u></p>" +
            """<p><span style="color:#b91c1c">빨강</span> <span style="background-color:#fef08a">노랑</span></p>""" +
            "<ul><li>하나</li><li>둘</li></ul>"

    private fun roundTrip(dark: Boolean) = runComposeUiTest {
        lateinit var colors: DiaryColors
        setContent { Capture(dark) { colors = it } }
        waitForIdle()
        val editor = EditorColors(colors)
        val state = RichTextState()

        state.setHtml(editor.toEditor(body))
        val saved = RichTextSanitizer.sanitize(editor.fromEditor(state.toHtml()))

        val expected = parseRichText(body)
        val actual = parseRichText(saved)
        assertEquals(expected.text, actual.text, saved)
        assertEquals(expected.spans.map { it.style }.toSet(), actual.spans.map { it.style }.toSet(), saved)
    }

    /** 줄바꿈 한 번은 한 줄로, 빈 줄은 문단 나눔으로 남음 */
    @Test
    fun lines_and_paragraphs_survive() = runComposeUiTest {
        lateinit var colors: DiaryColors
        setContent { Capture(false) { colors = it } }
        waitForIdle()
        val editor = EditorColors(colors)
        for (saved in listOf("<p>첫 줄<br>둘째 줄</p>", "<p>문단</p><p>다음 문단</p>")) {
            val state = RichTextState()
            state.setHtml(editor.toEditor(saved))
            assertEquals(saved, RichTextSanitizer.sanitize(editor.fromEditor(state.toHtml())))
        }
    }

    /** 사람이 치듯 한 글자씩 넣어야 `1. `을 목록으로 바꾸는 편집기 동작까지 지남 */
    private fun RichTextState.typeOut(text: String) {
        for (letter in text) {
            val value = textFieldValue
            val at = value.selection.min
            onTextFieldValueChange(TextFieldValue(value.text.substring(0, at) + letter + value.text.substring(at), TextRange(at + 1)))
        }
    }

    private fun savedBody(editor: EditorColors, state: RichTextState) = RichTextSanitizer.sanitize(editor.fromEditor(state.toHtml()))

    private fun withEditor(dark: Boolean = false, block: (EditorColors) -> Unit) = runComposeUiTest {
        lateinit var colors: DiaryColors
        setContent { Capture(dark) { colors = it } }
        waitForIdle()
        block(EditorColors(colors))
    }

    @Test
    fun a_typed_numbered_list_keeps_its_text() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("앞줄\n1. 할 일\n운동\n\n뒷줄")
        val plain = toPlainText(savedBody(editor, state))
        for (word in listOf("앞줄", "할 일", "운동", "뒷줄")) assertTrue(word in plain, plain)
    }

    @Test
    fun a_body_of_only_a_numbered_list_is_not_empty() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("1. 오늘 할 일\n운동")
        val plain = toPlainText(savedBody(editor, state))
        assertTrue("오늘 할 일" in plain && "운동" in plain, plain)
    }

    @Test
    fun a_saved_numbered_list_reopens_and_saves_the_same() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("1. 가\n나")
        val first = savedBody(editor, state)
        val reopened = RichTextState()
        reopened.setHtml(editor.toEditor(first))
        assertEquals(first, savedBody(editor, reopened))
    }

    @Test
    fun every_palette_color_survives_in_light_theme() = paletteRoundTrip(dark = false)

    @Test
    fun every_palette_color_survives_in_dark_theme() = paletteRoundTrip(dark = true)

    private fun paletteRoundTrip(dark: Boolean) = withEditor(dark) { editor ->
        for (hex in RichTextSanitizer.INK) {
            val saved = """<p><span style="color:$hex">글</span></p>"""
            val state = RichTextState()
            state.setHtml(editor.toEditor(saved))
            assertEquals(saved, savedBody(editor, state))
        }
        for (hex in RichTextSanitizer.MARK) {
            val saved = """<p><span style="background-color:$hex">글</span></p>"""
            val state = RichTextState()
            state.setHtml(editor.toEditor(saved))
            assertEquals(saved, savedBody(editor, state))
        }
    }

    @Test
    fun text_that_looks_like_a_color_stays() = withEditor(dark = true) { editor ->
        val saved = "<p>#b91c1c 그리고 rgb(1, 2, 3)</p>"
        val state = RichTextState()
        state.setHtml(editor.toEditor(saved))
        assertEquals(saved, savedBody(editor, state))
    }

    @Test
    fun bold_underline_ink_and_mark_on_the_same_text_survive() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("가나다")
        state.selection = TextRange(0, 3)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.addSpanStyle(SpanStyle(color = Color(0xFFB91C1C)))
        state.addSpanStyle(SpanStyle(background = Color(0xFFFEF08A)))
        val saved = savedBody(editor, state)
        for (piece in listOf("<b>", "<u>", "color:#b91c1c", "background-color:#fef08a", "가나다")) assertTrue(piece in saved, saved)
    }

    @Test
    fun a_style_across_two_lines_stays_on_both() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("가\n나")
        state.selection = TextRange(0, state.textFieldValue.text.length)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        assertEquals("<p><b>가</b><br><b>나</b></p>", savedBody(editor, state))
    }

    @Test
    fun emoji_tags_and_ampersands_stay_as_written() = withEditor { editor ->
        val state = RichTextState()
        state.typeOut("😀 <b>가</b> & 나  다")
        assertEquals("😀 <b>가</b> & 나  다", toPlainText(savedBody(editor, state)))
    }

    /** 옛 일기를 열어 저장해도 들여쓰기와 겹친 공백이 그대로 */
    @Test
    fun spaces_in_an_old_entry_stay() = withEditor { editor ->
        val saved = "<p>  첫 줄<br>\t둘째  줄 끝</p>"
        val state = RichTextState()
        state.setHtml(editor.toEditor(saved))
        assertEquals(toPlainText(saved), toPlainText(savedBody(editor, state)))
    }

    @Test
    fun a_very_long_entry_round_trips() = withEditor { editor ->
        val saved = "<p>" + "가나다라마바사 ".repeat(14_000) + "</p>"
        val state = RichTextState()
        state.setHtml(editor.toEditor(saved))
        assertEquals(toPlainText(saved).trim(), toPlainText(savedBody(editor, state)).trim())
    }

    @Test
    fun formats_survive_in_light_theme() = roundTrip(dark = false)

    @Test
    fun formats_survive_in_dark_theme() = roundTrip(dark = true)
}

@Composable
private fun Capture(dark: Boolean, onColors: (DiaryColors) -> Unit) {
    DiaryTheme(darkTheme = dark) { onColors(DiaryTheme.colors) }
}
