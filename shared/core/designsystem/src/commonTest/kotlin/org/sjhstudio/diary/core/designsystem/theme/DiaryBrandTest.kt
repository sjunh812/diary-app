package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 테마 색 토큰(스펙 010 FR-002·FR-005~FR-010, SC-001·SC-002)
class DiaryBrandTest {
    private val presets =
        mapOf(
            "라벤더" to DiaryBrand.Lavender,
            "자두" to DiaryBrand.Plum,
            "로즈" to DiaryBrand.Rose,
            "단풍" to DiaryBrand.Maple,
            "숲" to DiaryBrand.Forest,
            "바다" to DiaryBrand.Sea,
            "흑백" to DiaryBrand.Black
        )

    @Test
    fun lavender_is_exactly_the_current_tokens() {
        assertEquals(LightDiaryColors, diaryColors(dark = false, brand = DiaryBrand.Lavender))
        assertEquals(DarkDiaryColors, diaryColors(dark = true, brand = DiaryBrand.Lavender))
    }

    @Test
    fun presets_meet_contrast_in_both_modes() {
        for ((name, brand) in presets) assertReadable(name, brand, preset = true)
    }

    @Test
    fun custom_colors_meet_contrast_around_the_hue_wheel() {
        for (hue in 0 until 360 step 15) {
            for (l in listOf(0.25, 0.45, 0.6, 0.75, 0.9, 0.97)) {
                for (c in listOf(0.04, 0.12, 0.25)) {
                    val seed = okLchToRgb(l, c, hue.toDouble())
                    assertReadable(hex(seed), DiaryBrand.custom(seed), preset = false)
                }
            }
        }
    }

    @Test
    fun custom_edge_colors_meet_contrast() {
        val edges =
            listOf(0x000000, 0xFFFFFF, 0x808080, 0xC0C0C0, 0xFFFF00, 0x8DB600, 0xFFC0CB, 0x1A237E, 0xFF7F50, 0xFF0000, 0x00FFFF, 0x2A9D8F)
        for (seed in edges) assertReadable(hex(seed), DiaryBrand.custom(seed), preset = false)
    }

    @Test
    fun custom_keeps_the_picked_color() {
        assertEquals(0x6E56CF, DiaryBrand.custom(0x6E56CF).light.accent.rgb())
        assertEquals(0xFFFFFF, DiaryBrand.custom(0x6E56CF).light.onAccent.rgb())
        assertEquals(0xD2691E, DiaryBrand.custom(0xD2691E).light.accent.rgb())
    }

    @Test
    fun vivid_custom_colors_stay_as_picked() {
        // 채도 상한은 계산한 면에만. 버튼 면까지 줄이면 빨강이 탁한 주황빨강이 됨
        for (seed in listOf(0xFF0000, 0x0000FF, 0x007AFF, 0xE91E63, 0x9C27B0, 0x00C853)) {
            assertEquals(hex(seed), hex(DiaryBrand.custom(seed).light.accent.rgb()))
        }
    }

    @Test
    fun light_custom_color_gets_a_darker_icon_only() {
        // 버튼 면은 고른 그대로, 흰 바탕 위 별·고리만 3:1까지 진하게
        val sky = DiaryBrand.custom(0xC8E9FF).light
        assertTrue(contrast(sky.accent.rgb(), 0xF3F4F5) < 3.0)
        assertTrue(contrast(sky.accentIcon.rgb(), 0xF3F4F5) >= 3.0)
        // 충분히 진한 색은 아이콘도 같은 색
        val violet = DiaryBrand.custom(0x6E56CF).light
        assertEquals(violet.accent, violet.accentIcon)
    }

    @Test
    fun button_text_is_whichever_reads_better_by_apca() {
        // WCAG 2로는 흰 글자 3:1 미만이지만 APCA로는 흰 글자가 더 잘 보임
        for (seed in listOf(0x46A6FF, 0xE07A5F)) {
            val light = DiaryBrand.custom(seed).light
            assertEquals(seed, light.accent.rgb())
            assertEquals(0xFFFFFF, light.onAccent.rgb(), hex(seed))
        }
        // 주황·초록은 검은 글자가 더 잘 보임
        for (seed in listOf(0xFF9500, 0x34C759)) {
            assertEquals(0x1A1C20, DiaryBrand.custom(seed).light.onAccent.rgb(), hex(seed))
        }
    }

    @Test
    fun apca_matches_the_reference_values() {
        // APCA 0.0.98G 공개 예시 값
        assertEquals(106.04, apca(0x000000, 0xFFFFFF), 0.01)
        assertEquals(107.88, apca(0xFFFFFF, 0x000000), 0.01)
        assertEquals(63.06, apca(0x888888, 0xFFFFFF), 0.01)
        assertEquals(68.54, apca(0xFFFFFF, 0x888888), 0.01)
    }

    @Test
    fun bright_colors_keep_their_color_with_dark_text() {
        val honey = DiaryBrand.custom(0xEBCB8B).light
        assertEquals(0xEBCB8B, honey.accent.rgb())
        assertEquals(0x1A1C20, honey.onAccent.rgb())
    }

    @Test
    fun dark_neutral_becomes_black_and_light_neutral_stays_gray() {
        assertEquals(DiaryBrand.Black, DiaryBrand.custom(0x333333))
        val gray = DiaryBrand.custom(0xBBBBBB).light
        assertEquals(0x1A1C20, gray.onAccent.rgb())
    }

    @Test
    fun near_white_gets_a_visible_button() {
        val white = DiaryBrand.custom(0xFFFFFF).light
        assertTrue(contrast(white.accent.rgb(), 0xFFFFFF) >= 1.29)
    }

    @Test
    fun very_dark_color_stays_as_picked() {
        val navy = DiaryBrand.custom(0x0A0A40)
        assertEquals(0x0A0A40, navy.light.accent.rgb())
        assertEquals(0xFFFFFF, navy.light.onAccent.rgb())
        // 다크 바탕에 묻히면 다크에서만 밝힘
        assertTrue(contrast(navy.dark.accent.rgb(), DarkDiaryColors.canvas.rgb()) >= 3.0)
    }

    /**
     * 브랜드 글자는 모든 바탕 위 4.5:1, 버튼 위 글자는 APCA Lc 45 이상(정한 색은 WCAG 4.5:1도)
     * 다크 버튼은 다크 바탕과 3:1, 토스트 버튼 글자는 토스트 위 4.5:1
     */
    private fun assertReadable(name: String, brand: DiaryBrand, preset: Boolean) {
        for (dark in listOf(false, true)) {
            val c = diaryColors(dark, brand)
            val mode = if (dark) "다크" else "라이트"
            for ((bg, color) in listOf(
                "canvas" to c.canvas,
                "surface" to c.surface,
                "paper" to c.paper,
                "sunken" to c.sunken,
                "selected" to c.selected,
                "surfaceCard" to c.surfaceCard
            )) {
                check(name, mode, "accentText/$bg", c.accentText, color, BODY)
            }
            check(name, mode, "textPrimary/selected", c.textPrimary, c.selected, BODY)
            if (preset) check(name, mode, "onAccent/accent", c.onAccent, c.accent, BODY)
            val lc = apca(c.onAccent.rgb(), c.accent.rgb())
            assertTrue(lc >= BUTTON_LC, "$name $mode onAccent/accent: Lc ${lc.toInt()} < $BUTTON_LC")
            check(name, mode, "toastAction/toast", c.toastAction, c.toast, BODY)
            if (dark) check(name, mode, "accent/canvas", c.accent, c.canvas, LARGE)
            // 별·고리·커서는 놓이는 바탕과 3:1. 정한 색 다크는 지금 값(버튼과 같은 색)이라 다크 바탕만 봄
            val iconBackgrounds =
                when {
                    !dark -> listOf("canvas" to c.canvas, "sunken" to c.sunken, "selected" to c.selected)
                    !preset -> listOf("canvas" to c.canvas, "surfaceCard" to c.surfaceCard, "sunken" to c.sunken, "selected" to c.selected)
                    else -> listOf("canvas" to c.canvas)
                }
            for ((bg, color) in iconBackgrounds) check(name, mode, "accentIcon/$bg", c.accentIcon, color, LARGE)
            // 사진 위 글자는 테마와 상관없이 흰색. 흑백·밝은 색에서 onAccent는 검정
            check(name, mode, "onPhotoContent/photoBackdrop", c.onPhotoContent, c.photoBackdrop, BODY)
        }
    }

    private fun check(name: String, mode: String, pair: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg.rgb(), bg.rgb())
        assertTrue(ratio >= min, "$name $mode $pair: ${(ratio * 100).toInt() / 100.0}:1 < $min")
    }

    private fun Color.rgb() = toArgb() and 0xFFFFFF

    private fun hex(rgb: Int) = "#" + rgb.toString(16).padStart(6, '0').uppercase()

    private companion object {
        const val BODY = 4.5
        const val LARGE = 3.0

        // APCA 굵은 큰 글자 최소
        const val BUTTON_LC = 45
    }
}
