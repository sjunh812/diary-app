package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

// 글자는 4.5:1 이상이라는 규칙(`docs/design.md` 색, WCAG 1.4.3)을 토큰 값으로 지킨다.
// textTertiary와 placeholder는 규칙이 허용한 자리에만 쓰므로 뺀다.
class ColorContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertReadable(mode: String, name: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue(ratio >= 4.5, "$mode $name: ${(ratio * 100).toInt() / 100.0}:1")
    }

    @Test
    fun text_tokens_are_readable_on_every_background() {
        for ((mode, c) in listOf("라이트" to LightDiaryColors, "다크" to DarkDiaryColors)) {
            for ((bgName, bg) in listOf(
                "canvas" to c.canvas,
                "surface" to c.surface,
                "paper" to c.paper,
                "sunken" to c.sunken,
                "selected" to c.selected,
                "surfaceCard" to c.surfaceCard
            )) {
                assertReadable(mode, "textPrimary/$bgName", c.textPrimary, bg)
                assertReadable(mode, "textSecondary/$bgName", c.textSecondary, bg)
                assertReadable(mode, "accentText/$bgName", c.accentText, bg)
            }
            assertReadable(mode, "onAccent/accent", c.onAccent, c.accent)
            assertReadable(mode, "onDanger/danger", c.onDanger, c.danger)
            assertReadable(mode, "onToast/toast", c.onToast, c.toast)
            assertReadable(mode, "toastAction/toast", c.toastAction, c.toast)
        }
    }
}
