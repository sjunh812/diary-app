package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.data.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.parseRichText

// 편집기를 지나도 서식이 그대로 남는지 본다(FR-008). 편집기는 라이브러리라 내보내는 모양이 저장 규칙을 지나는지가 위험하다.
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

    @Test
    fun formats_survive_in_light_theme() = roundTrip(dark = false)

    @Test
    fun formats_survive_in_dark_theme() = roundTrip(dark = true)
}

@Composable
private fun Capture(dark: Boolean, onColors: (DiaryColors) -> Unit) {
    DiaryTheme(darkTheme = dark) { onColors(DiaryTheme.colors) }
}
