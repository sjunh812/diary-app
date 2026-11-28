package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.allDrawableResources

// 테마 그림이 빠지면 그 기분만 기본 얼굴로 돌아가 모르고 지나치기 쉬움
class MoodArtTest {
    private val drawn = MoodTheme.entries - MoodTheme.BASIC - MoodTheme.CLAY

    @Test
    fun every_picture_theme_has_all_moods_in_light_and_dark() {
        for (theme in drawn) {
            for (sticker in listOf(false, true)) {
                for (dark in listOf(false, true)) {
                    for (mood in Mood.ordered) {
                        val look = ShownMoodLook(theme, sticker = sticker)
                        val large = look.art(mood, dark, small = false)
                        val small = look.art(mood, dark, small = true)
                        assertNotNull(large, "${theme.id} sticker=$sticker dark=$dark ${mood.id}")
                        // 작은 칸용 그림이 빠지면 그 기분만 큰 그림을 줄여 테두리가 깨짐
                        assertNotEquals(large, small, "${theme.id} sticker=$sticker dark=$dark ${mood.id} small")
                    }
                }
            }
        }
    }

    @Test
    fun basic_and_clay_are_not_single_pictures() {
        for (mood in Mood.ordered) {
            assertNull(ShownMoodLook(MoodTheme.BASIC).art(mood, false, small = false))
            assertNull(ShownMoodLook(MoodTheme.CLAY).art(mood, false, small = false))
        }
    }

    @Test
    fun every_clay_hair_draws_skin_hair_nose_and_face_for_every_mood() {
        val all = Res.allDrawableResources
        for (hair in ClayHair.entries) {
            for (mood in Mood.ordered) {
                val look = ShownMoodLook(MoodTheme.CLAY, hair = hair, glasses = true)
                val layers = look.clayLayers(mood, small = false)
                val smallLayers = look.clayLayers(mood, small = true)
                assertEquals(layers.size, smallLayers.size, "${hair.id} ${mood.id} small count")
                assertTrue(layers.zip(smallLayers).none { (large, small) -> large.art == small.art }, "${hair.id} ${mood.id} small")
                assertTrue(layers.any { it.tint == Tint.SKIN }, "${hair.id} skin")
                assertTrue(layers.any { it.tint == Tint.HAIR }, "${hair.id} hair")
                assertTrue(layers.any { it.tint == Tint.NOSE }, "${hair.id} nose")
                assertTrue(layers.any { it.art == all["clay_face_${mood.id}"] }, "${hair.id} ${mood.id} face")
            }
        }
    }
}
