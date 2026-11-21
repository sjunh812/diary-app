package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// iOS는 쓴 줄바꿈에서 끝나는 마지막 줄에 "…"를 그리지 않음
@OptIn(ExperimentalTestApi::class)
class DiaryPreviewTextTest {
    private val longFirstLine = "아침에 일어나 창문을 열었더니 바람이 생각보다 차가워서 다시 이불 속으로 들어갔다"

    private fun shown(text: String, maxLines: Int = 3): Pair<TextLayoutResult, Boolean> {
        val out = mutableStateOf<Pair<TextLayoutResult, Boolean>?>(null)
        runComposeUiTest {
            setContent {
                DiaryTheme(darkTheme = false) {
                    val measurer = rememberTextMeasurer()
                    val density = LocalDensity.current
                    val direction = LocalLayoutDirection.current
                    val width = with(density) { 200.dp.roundToPx() }
                    out.value = measurer.preview(AnnotatedString(text), DiaryTheme.typography.body, maxLines, width, density, direction)
                }
            }
            waitForIdle()
        }
        return out.value!!
    }

    @Test
    fun wrapped_first_line_pushes_a_written_line_out_and_ends_with_ellipsis() {
        val (layout, clipped) = shown("$longFirstLine\n점심\n저녁")

        assertTrue(clipped)
        val text = layout.layoutInput.text.text
        assertTrue(text.endsWith("…"), text)
        assertFalse(text.contains("저녁"), text)
    }

    @Test
    fun short_written_lines_that_fit_are_left_alone() {
        val (layout, clipped) = shown("아침\n점심\n저녁")

        assertFalse(clipped)
        assertEquals("아침\n점심\n저녁", layout.layoutInput.text.text)
    }

    @Test
    fun one_long_paragraph_keeps_the_platform_ellipsis() {
        val (layout, clipped) = shown(longFirstLine.repeat(4))

        assertTrue(clipped)
        // 접힌 줄은 플랫폼이 줄이므로 글을 바꾸지 않음
        assertEquals(longFirstLine.repeat(4), layout.layoutInput.text.text)
    }

    @Test
    fun two_wrapped_lines_then_more_written_lines_end_with_ellipsis() {
        val (layout, clipped) = shown("$longFirstLine\n$longFirstLine\n끝")

        assertTrue(clipped)
        assertTrue(layout.layoutInput.text.text.endsWith("…"))
        assertFalse(layout.layoutInput.text.text.contains("끝"))
    }

    @Test
    fun clipped_is_reported_to_the_row() {
        var clipped: Boolean? = null
        runComposeUiTest {
            setContent {
                DiaryTheme(darkTheme = false) {
                    Box(Modifier.width(200.dp)) {
                        DiaryPreviewText(AnnotatedString("$longFirstLine\n점심\n저녁"), DiaryTheme.typography.body, 3, onClip = {
                            clipped =
                                it
                        })
                    }
                }
            }
            waitForIdle()
        }
        assertEquals(true, clipped)
    }

    @Test
    fun asking_intrinsic_size_does_not_change_what_is_drawn() = runComposeUiTest {
        val reports = mutableListOf<Boolean>()
        setContent {
            DiaryTheme(darkTheme = false) {
                Row(Modifier.width(200.dp).height(IntrinsicSize.Min)) {
                    DiaryPreviewText(
                        AnnotatedString("$longFirstLine\n점심\n저녁"),
                        DiaryTheme.typography.body,
                        3,
                        Modifier.weight(1f).testTag("preview"),
                        onClip = { reports += it }
                    )
                }
            }
        }
        waitForIdle()
        // 고유 크기를 물어도 줄바꿈 없는 한 줄로 그려지거나 잘림이 거짓으로 바뀌지 않음
        assertTrue(reports.isNotEmpty() && reports.all { it }, reports.toString())
        val bounds = onNodeWithTag("preview").getBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top > 40.dp, bounds.toString())
    }

    @Test
    fun new_text_replaces_the_old_one() = runComposeUiTest {
        var text by mutableStateOf("아침")
        var clipped: Boolean? = null
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(200.dp)) {
                    DiaryPreviewText(AnnotatedString(text), DiaryTheme.typography.body, 3, Modifier.testTag("preview"), onClip = {
                        clipped =
                            it
                    })
                }
            }
        }
        waitForIdle()
        assertEquals(false, clipped)
        text = "$longFirstLine\n점심\n저녁"
        waitForIdle()
        assertEquals(true, clipped)
        onNodeWithText(text).assertExists()
    }
}
