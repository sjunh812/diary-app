package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

// 기분 테마 설정(스펙 013 data-model "MoodLook")
class MoodThemeTest {
    @Test
    fun empty_look_is_the_basic_theme_and_default_clay() {
        val look = MoodLook()
        assertEquals(MoodTheme.BASIC, look.theme)
        assertEquals(ClayHair.BOB, look.hair)
        assertEquals(ClaySkin.SKIN2, look.skin)
        assertEquals(false, look.glasses)
        assertEquals(false, look.sticker)
    }

    /** 다음 판의 새 테마를 모르면 기본으로 보이지만 저장값은 그대로 */
    @Test
    fun unknown_ids_show_defaults_but_stay_stored() {
        val look = MoodLook(themeId = "galaxy", clayHair = "mohawk", claySkin = "skin9")
        assertEquals(MoodTheme.BASIC, look.theme)
        assertEquals(ClayHair.BOB, look.hair)
        assertEquals(ClaySkin.SKIN2, look.skin)
        assertEquals("galaxy", look.withSticker(true).themeId)
    }

    @Test
    fun plus_themes_show_only_with_plus() {
        val clay = MoodLook().withTheme(MoodTheme.CLAY)
        assertEquals(MoodTheme.BASIC, clay.shownTheme(plusAllowed = false))
        assertEquals(MoodTheme.CLAY, clay.shownTheme(plusAllowed = true))
        assertEquals(MoodTheme.DOT, MoodLook().withTheme(MoodTheme.DOT).shownTheme(plusAllowed = false))
    }

    /** Plus가 끝나면 테마만 기본으로. 다시 Plus가 되면 고르던 클레이 꼴이 돌아옴 */
    @Test
    fun without_plus_keeps_the_clay_look() {
        val look = MoodLook().withTheme(MoodTheme.CLAY).withClay(ClayHair.CURLY, ClaySkin.SKIN5, glasses = true)
        val reset = look.withoutPlusTheme()
        assertEquals(MoodTheme.BASIC, reset.theme)
        assertEquals(ClayHair.CURLY, reset.hair)
        assertEquals(ClaySkin.SKIN5, reset.skin)
        assertEquals(true, reset.glasses)
        val free = MoodLook().withTheme(MoodTheme.GOUACHE)
        assertEquals(free, free.withoutPlusTheme())
    }

    /** 한 칸만 바꾸면 나머지 칸은 모르는 id여도 원문 그대로 */
    @Test
    fun changing_one_clay_part_keeps_the_others() {
        val look = MoodLook(clayHair = "mohawk", claySkin = "skin9")
        val glasses = look.withClay(glasses = true)
        assertEquals("mohawk", glasses.clayHair)
        assertEquals("skin9", glasses.claySkin)
        assertEquals(true, glasses.glasses)
        assertEquals("skin9", look.withClay(hair = ClayHair.BUN).claySkin)
    }

    @Test
    fun free_and_plus_themes_are_as_planned() {
        assertEquals(
            listOf(MoodTheme.BUBBLE, MoodTheme.DOODLE, MoodTheme.VEGGIE, MoodTheme.CLAY),
            MoodTheme.entries.filter { it.plus }
        )
        assertEquals(true, Plan.PLUS.moodThemes)
        assertEquals(false, Plan.AD_FREE.moodThemes)
        assertEquals(false, Plan.FREE.moodThemes)
    }
}
