@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.richtext.editorToClipboard
import org.sjhstudio.diary.core.model.richtext.toPlainText

/**
 * 복사한 일기를 다른 앱에 붙여넣으면 줄마다 빈 줄이 하나씩 더 생기던 문제(2.0.1 리뷰)
 * 다른 앱은 `br`만 줄바꿈 하나로 읽으니, 복사용 HTML을 서식 없이 읽으면 에디터 글과 같아야 함
 */
class CopyLinesTest {
    private val bodies =
        listOf(
            "<p>첫 줄</p><p>둘째 줄</p>",
            "<p>첫 줄</p><br><p>빈 줄 뒤</p>",
            "<p><b>굵게</b> 글</p><p><span style=\"color: rgba(185, 28, 28, 1.0);\">빨강</span></p>",
            "<p>앞</p><ul><li>가</li><li>나</li></ul><p>뒤</p>",
            "<br><p>가운데</p><br><br><p>끝</p>",
            "<p>☐ 우유</p><p>☑ 빵</p>"
        )

    private fun state(html: String) = RichTextState().apply { setHtml(html) }

    private fun copied(state: RichTextState, range: TextRange) = editorToClipboard(state.toHtml(range))

    @Test
    fun anotherAppReadsTheSameLines() {
        for (body in bodies) {
            val state = state(body)
            val all = TextRange(0, state.annotatedString.text.length)
            assertEquals(state.toText(all), toPlainText(copied(state, all)), body)
        }
    }

    @Test
    fun pastingBackKeepsTheLines() {
        for (body in bodies) {
            val state = state(body)
            val all = TextRange(0, state.annotatedString.text.length)
            assertEquals(state.toText(all), state(copied(state, all)).toText(), body)
        }
    }

    @Test
    fun pastingBackKeepsStyles() {
        val state = state("<p><b>굵게</b> 글</p><p><u>밑줄</u></p>")
        val html = state(copied(state, TextRange(0, state.annotatedString.text.length))).toHtml()
        assertTrue("<b>굵게</b>" in html && "<u>밑줄</u>" in html, html)
    }

    @Test
    fun aPartCopiesOnlyThoseLines() {
        val state = state("<p>하나</p><p>둘</p><br><p>셋</p><p>넷</p>")
        val text = state.annotatedString.text
        val range = TextRange(text.indexOf("둘"), text.indexOf("셋") + 1)
        assertEquals("둘\n\n셋", toPlainText(copied(state, range)))
    }
}
