package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.designsystem.theme.DiaryBrand
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor

// 어떤 테마 색이 보이는지(스펙 010 FR-018, US6, Edge Cases)
class ThemeBrandTest {
    private val custom = ThemeChoice(colorId = "custom", customHex = "#C2185B")

    @Test
    fun default_is_lavender() {
        assertEquals(DiaryBrand.Lavender, ThemeChoice().brand(customAllowed = false))
    }

    @Test
    fun preset_is_shown_for_everyone() {
        assertEquals(DiaryBrand.Sea, ThemeChoice(colorId = "sea").brand(customAllowed = false))
    }

    @Test
    fun custom_needs_plus() {
        assertEquals(DiaryBrand.custom(0xC2185B), custom.brand(customAllowed = true))
        assertEquals(ThemeColor.CUSTOM, custom.shownColor(customAllowed = true))
        // Plus가 끝나면 라벤더로 보임
        assertEquals(DiaryBrand.Lavender, custom.brand(customAllowed = false))
        assertEquals(ThemeColor.LAVENDER, custom.shownColor(customAllowed = false))
    }

    @Test
    fun unknown_name_and_broken_custom_show_lavender() {
        assertEquals(DiaryBrand.Lavender, ThemeChoice(colorId = "mauve").brand(customAllowed = true))
        assertEquals(DiaryBrand.Lavender, ThemeChoice(colorId = "custom", customHex = "#12").brand(customAllowed = true))
        assertEquals(ThemeColor.LAVENDER, ThemeChoice(colorId = "custom").shownColor(customAllowed = true))
    }
}
