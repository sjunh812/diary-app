package org.sjhstudio.diary.feature.write

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 본문 스크롤이 커서·선택 끝을 가장자리에서 띄워 보이게 하는지
class EditorScrollTest {
    private val height = 600f
    private val margin = 56f

    @Test
    fun already_visible_with_room_does_not_scroll() = assertEquals(0f, scrollDistance(100f, 28f, height, margin))

    @Test
    fun line_at_the_bottom_edge_scrolls_up_to_leave_room() = // 572~600에 있는 줄은 아래에 56을 남기게 56만큼 올라감
        assertEquals(56f, scrollDistance(572f, 28f, height, margin))

    @Test
    fun line_above_the_top_scrolls_down_to_leave_room() = assertEquals(-76f, scrollDistance(-20f, 28f, height, margin))

    @Test
    fun too_tall_to_fit_with_room_falls_back_to_the_edge() = // 여백까지 넣을 수 없으면 가장자리에 맞춤
        assertEquals(100f, scrollDistance(150f, 550f, height, margin))

    @Test
    fun dragging_the_end_handle_follows_the_end() = assertEquals(12, movingEdge(TextRange(3, 8), TextRange(3, 12)))

    @Test
    fun dragging_the_start_handle_follows_the_start() = assertEquals(1, movingEdge(TextRange(3, 8), TextRange(1, 8)))

    @Test
    fun a_new_word_from_the_caret_shows_its_end() = assertEquals(9, movingEdge(TextRange(4), TextRange(4, 9)))

    @Test
    fun select_all_does_not_jump_to_the_end() = assertNull(movingEdge(TextRange(3, 8), TextRange(0, 400)))

    @Test
    fun typing_with_a_caret_does_nothing() = assertNull(movingEdge(TextRange(4), TextRange(5)))
}
