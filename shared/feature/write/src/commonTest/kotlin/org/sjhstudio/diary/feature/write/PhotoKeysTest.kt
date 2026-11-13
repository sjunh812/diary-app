package org.sjhstudio.diary.feature.write

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.DraftPhoto

// 사진 줄의 목록 키. 같은 키가 둘이면 지연 목록이 꺼짐
class PhotoKeysTest {
    private fun photo(sha256: String) = DraftPhoto("$sha256.jpg", sha256, 1, 1, 1L)

    @Test
    fun the_same_photo_added_twice_gets_two_keys() {
        val keys = PhotoKeys().assign(listOf(photo("a"), photo("b"), photo("a")))

        assertEquals(listOf("a-0", "b-0", "a-1"), keys)
    }

    /** 끌어 옮긴 순서가 저장돼 돌아와도 사진마다 키가 그대로. 바뀌면 줄이 그 키를 따라 튐 */
    @Test
    fun a_photo_keeps_its_key_after_it_is_moved_past_its_twin() {
        val keyed = PhotoKeys()
        keyed.assign(listOf(photo("a"), photo("b"), photo("a")))

        keyed.moved(2, 0)
        val keys = keyed.assign(listOf(photo("a"), photo("a"), photo("b")))

        assertEquals(listOf("a-1", "a-0", "b-0"), keys)
    }

    @Test
    fun a_removed_photo_frees_its_key_and_a_new_one_never_collides() {
        val keyed = PhotoKeys()
        keyed.assign(listOf(photo("a"), photo("a")))
        keyed.assign(listOf(photo("a")))

        val keys = keyed.assign(listOf(photo("a"), photo("a"), photo("a")))

        assertEquals(3, keys.toSet().size)
        assertEquals("a-0", keys.first())
    }
}
