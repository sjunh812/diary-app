package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.clay_glasses

class ClayLayersTest {
    private val look = ShownMoodLook(MoodTheme.CLAY, hair = ClayHair.DEFAULT, glasses = true)

    @Test
    fun glasses_are_drawn_on_every_mood_but_cool() {
        for (mood in Mood.ordered) {
            val glasses = look.clayLayers(mood).any { it.art == Res.drawable.clay_glasses }
            assertEquals(mood != Mood.COOL, glasses, mood.id)
        }
    }

    @Test
    fun every_mood_has_a_face_layer() {
        for (mood in Mood.ordered) assertTrue(look.clayLayers(mood).any { it.tint == null }, mood.id)
    }
}
