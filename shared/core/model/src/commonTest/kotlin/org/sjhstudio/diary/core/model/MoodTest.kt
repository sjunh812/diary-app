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

    /** 통계 기준 시트가 묶음별로 거른 순서와 쓰기 시트 순서가 어긋나지 않음 */
    @Test
    fun tones_come_in_one_block_each() {
        val tones = Mood.ordered.map { it.tone }
        assertEquals(listOf(MoodTone.GOOD, MoodTone.HARD, MoodTone.NEUTRAL), tones.distinct())
        assertEquals(tones, tones.sortedBy { listOf(MoodTone.GOOD, MoodTone.HARD, MoodTone.NEUTRAL).indexOf(it) })
    }

    /** 좋은 기분, 힘든 기분, 세지 않는 기분 차례이고 묶음 안은 색 차례(무지개) */
    @Test
    fun the_screen_order_follows_tone_then_color() {
        assertEquals(
            listOf(
                "excited", "good", "laugh", "proud", "calm",
                "angry", "annoyed", "sick", "tired", "sad",
                "anxious", "bad", "cool", "meh", "bored"
            ),
            Mood.ordered.map { it.id }
        )
    }

    /** 1.7.4 번호는 0~8뿐이라 새 기분은 레거시 번호로 나오지 않음 */
    @Test
    fun new_moods_have_no_legacy_code() {
        assertEquals((0..8).toList(), Mood.entries.mapNotNull { it.legacyCode }.sorted())
        assertEquals(9, Mood.entries.count { it.legacyCode != null })
    }
}
