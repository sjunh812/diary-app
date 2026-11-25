package org.sjhstudio.diary.feature.write

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 재생성된 화면에 커서와 키보드를 돌려줄지 정하는 규칙
class WritingMemoryTest {
    @Test
    fun typing_with_the_keyboard_up_is_writing() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        assertTrue(memory.writing)
    }

    @Test
    fun lowering_the_keyboard_is_not_writing() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.update(focused = true, imeVisible = false)
        assertFalse(memory.writing)
    }

    @Test
    fun raising_the_keyboard_again_is_writing() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.update(focused = true, imeVisible = false)
        memory.update(focused = true, imeVisible = true)
        assertTrue(memory.writing)
    }

    /** 하드웨어 키보드는 화면 키보드가 처음부터 없어 내린 것이 아님 */
    @Test
    fun a_hardware_keyboard_is_writing() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = false)
        assertTrue(memory.writing)
    }

    @Test
    fun losing_the_cursor_is_not_writing() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.update(focused = false, imeVisible = false)
        assertFalse(memory.writing)
    }

    /** 내린 뒤 다른 곳에 갔다 본문을 다시 누르면 처음부터 다시 봄 */
    @Test
    fun focusing_again_after_lowering_starts_over() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.update(focused = true, imeVisible = false)
        memory.update(focused = false, imeVisible = false)
        memory.update(focused = true, imeVisible = false)
        assertTrue(memory.writing)
    }

    /** 다시 만든 화면이 커서를 주기 전 기록은 대기 중이라 덮지 않음 */
    @Test
    fun a_recreated_screen_keeps_writing_until_the_cursor_is_given() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        assertTrue(memory.restore())
        memory.update(focused = false, imeVisible = false)
        assertTrue(memory.writing)
        assertTrue(memory.restore())
    }

    /** 앞 화면에서 보이던 키보드가 새 화면에서 내린 것으로 이어지지 않음 */
    @Test
    fun the_keyboard_coming_up_on_a_recreated_screen_is_not_lowering() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.restore()
        memory.settle()
        memory.update(focused = true, imeVisible = false)
        assertTrue(memory.writing)
        memory.update(focused = true, imeVisible = true)
        assertTrue(memory.writing)
    }

    @Test
    fun not_writing_waits_for_nothing() {
        val memory = WritingMemory()
        assertFalse(memory.restore())
        memory.update(focused = true, imeVisible = true)
        assertTrue(memory.writing)
    }

    /** 다시 만든 화면을 덮개가 덮어 버린 커서는 닫은 뒤 다시 돌려도 안 돌려줌 */
    @Test
    fun a_dropped_cursor_is_not_given_back_on_the_next_rotation() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.restore()
        memory.drop()
        assertFalse(memory.writing)
        assertFalse(memory.restore())
    }

    /** 대기 중이 아니면 덮개가 떠도 쓰던 기록을 지우지 않음. 초점이 풀리며 기록이 바뀜 */
    @Test
    fun dropping_without_waiting_keeps_the_record() {
        val memory = WritingMemory()
        memory.update(focused = true, imeVisible = true)
        memory.drop()
        assertTrue(memory.writing)
    }
}
