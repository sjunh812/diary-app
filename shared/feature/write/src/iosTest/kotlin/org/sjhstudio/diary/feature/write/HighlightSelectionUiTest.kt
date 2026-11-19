package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 형광펜 위를 골라도 고른 곳은 선택 색, 나머지는 형광펜. 고르는 동안 글을 다시 배치하지 않음
@OptIn(ExperimentalTestApi::class)
class HighlightSelectionUiTest {
    private val yellow = Color(0xFFFFE066)
    private val pick = Color(0xFF3366FF)

    @Test
    fun the_selection_shows_over_a_mark_without_a_new_layout() = runComposeUiTest {
        val state = RichTextState()
        state.setText("aaaa bbbb cccc dddd")
        state.selection = TextRange(0, 19)
        state.addSpanStyle(SpanStyle(background = yellow))
        state.selection = TextRange(19)
        var layout: TextLayoutResult? = null
        var layouts = 0
        setContent {
            CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(pick, pick)) {
                DiaryRichEditor(
                    state = state,
                    textStyle = TextStyle(fontSize = 24.sp, color = Color.Black),
                    cursorBrush = SolidColor(Color.Black),
                    onTextLayout = {
                        layout = it
                        layouts++
                    },
                    modifier = Modifier.testTag(EDITOR).background(Color.White)
                )
            }
        }
        onNodeWithTag(EDITOR).performClick()
        waitForIdle()
        val settled = layouts

        for (range in listOf(TextRange(5, 9), TextRange(5, 12), TextRange(5, 16), TextRange(5, 7))) {
            runOnIdle { state.selection = range }
            waitForIdle()
            val pixels = onNodeWithTag(EDITOR).captureToImage().toPixelMap()
            fun at(offset: Int): Color {
                val box = layout!!.getBoundingBox(offset)
                return pixels[(box.left + 1).toInt(), (box.top + 1).toInt()]
            }
            assertNear(yellow, at(0), "고르지 않은 형광펜 $range")
            assertNear(yellow, at(17), "고르지 않은 형광펜 $range")
            val picked = at(range.min + 1)
            assertTrue(picked.blue > picked.red && picked.blue > picked.green, "고른 곳은 선택 색 $range: $picked")
        }
        assertEquals(settled, layouts, "고르는 동안 다시 배치")
    }

    private fun assertNear(expected: Color, actual: Color, message: String) {
        val near =
            abs(expected.red - actual.red) < 0.03f && abs(expected.green - actual.green) < 0.03f && abs(expected.blue - actual.blue) < 0.03f
        assertTrue(near, "$message: $actual")
    }

    private companion object {
        const val EDITOR = "editor"
    }
}
