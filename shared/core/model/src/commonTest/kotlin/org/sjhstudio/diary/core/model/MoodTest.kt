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

    /** 고르기 묶음: 좋았어요, 마음이 힘들었어요, 몸이 힘들었어요, 덤덤했어요 */
    @Test
    fun the_screen_order_follows_the_picker_groups() {
        assertEquals(
            listOf(
                listOf("excited", "good", "laugh", "proud", "calm"),
                listOf("angry", "annoyed", "anxious", "sad", "bad"),
                listOf("sick", "tired"),
                listOf("cool", "meh", "bored")
            ),
            MoodGroup.entries.map { group -> group.moods.map { it.id } }
        )
        assertEquals(MoodGroup.entries.flatMap { it.moods }, Mood.ordered)
    }

    /** 마음·몸 묶음은 통계에서 둘 다 힘든 기분이라 기준 시트와 어긋나지 않음 */
    @Test
    fun picker_groups_keep_one_tone_each() {
        val tones = MoodGroup.entries.associateWith { group -> group.moods.map { it.tone }.distinct() }
        assertEquals(listOf(MoodTone.GOOD), tones[MoodGroup.GOOD])
        assertEquals(listOf(MoodTone.HARD), tones[MoodGroup.MIND])
        assertEquals(listOf(MoodTone.HARD), tones[MoodGroup.BODY])
        assertEquals(listOf(MoodTone.NEUTRAL), tones[MoodGroup.NEUTRAL])
    }

    /** 1.7.4 번호는 0~8뿐이라 새 기분은 레거시 번호로 나오지 않음 */
    @Test
    fun new_moods_have_no_legacy_code() {
        assertEquals((0..8).toList(), Mood.entries.mapNotNull { it.legacyCode }.sorted())
        assertEquals(9, Mood.entries.count { it.legacyCode != null })
    }
}
