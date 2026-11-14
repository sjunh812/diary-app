package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MoodTest {
    /** 화면 순서에서 기분이 빠지면 고를 수 없게 됨 */
    @Test
    fun the_screen_order_has_every_mood_once() {
        assertEquals(Mood.entries.toSet(), Mood.ordered.toSet())
        assertEquals(Mood.entries.size, Mood.ordered.size)
    }
}
