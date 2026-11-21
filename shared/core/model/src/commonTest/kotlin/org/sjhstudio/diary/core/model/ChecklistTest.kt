package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChecklistTest {
    private val open = Checklist.OPEN
    private val done = Checklist.DONE

    private fun String.apply(edits: List<ChecklistEdit>) = edits.fold(this) { text, edit ->
        text.replaceRange(edit.start, edit.end, edit.text)
    }

    @Test
    fun item_needs_a_mark_and_a_space() {
        assertEquals(true, Checklist.isItem("$open 우유"))
        assertEquals(true, Checklist.isItem("$done 우유"))
        assertEquals(false, Checklist.isItem("$open"))
        assertEquals(false, Checklist.isItem("우유 $open "))
    }

    @Test
    fun html_toggle_flips_only_the_chosen_item() {
        val html = "<p>장보기<br>$open 우유<br>$open 달걀</p><p>$done 편지</p>"

        assertEquals("<p>장보기<br>$open 우유<br>$done 달걀</p><p>$done 편지</p>", Checklist.toggleInHtml(html, 1))
        assertEquals("<p>장보기<br>$open 우유<br>$open 달걀</p><p>$open 편지</p>", Checklist.toggleInHtml(html, 2))
        assertNull(Checklist.toggleInHtml(html, 3))
    }

    @Test
    fun html_toggle_finds_a_mark_inside_a_format_tag() {
        val html = "<p><span style=\"color: #b91c1c\"><b>$open 우유</b></span></p>"

        assertEquals(html.replace(open, done), Checklist.toggleInHtml(html, 0))
    }

    @Test
    fun mark_in_the_middle_of_a_line_is_not_an_item() {
        assertNull(Checklist.toggleInHtml("<p>오늘 $open 우유</p>", 0))
        assertNull(Checklist.toggleInHtml("<ul><li>$open 우유</li></ul>", 0))
    }

    @Test
    fun items_list_marks_and_labels_in_order() {
        val items = Checklist.items("장보기\n$open 우유\n$done 달걀")

        assertEquals(listOf(ChecklistItem(4, false, "우유"), ChecklistItem(9, true, "달걀")), items)
    }

    @Test
    fun index_counts_only_items_before_the_line() {
        val text = "장보기\n$open 우유\n메모\n$done 달걀"

        assertEquals(0, Checklist.indexOfItem(text, 4))
        assertEquals(1, Checklist.indexOfItem(text, text.lastIndexOf(done)))
        assertNull(Checklist.indexOfItem(text, 0))
    }

    @Test
    fun button_adds_marks_then_takes_them_away() {
        val text = "우유\n달걀"

        val added = text.apply(Checklist.toggleLines(text, 0, text.length))
        assertEquals("$open 우유\n$open 달걀", added)
        assertEquals(text, added.apply(Checklist.toggleLines(added, 0, added.length)))
    }

    @Test
    fun button_fills_only_the_plain_lines_of_a_mixed_selection() {
        val text = "$done 우유\n달걀"

        assertEquals("$done 우유\n$open 달걀", text.apply(Checklist.toggleLines(text, 0, text.length)))
    }

    /** 여러 줄을 고른 채 바꿔도 고른 글이 그대로 골라져 있음 */
    @Test
    fun the_selection_follows_the_marks() {
        val text = "오늘\n우유\n달걀"
        val start = text.indexOf("우유") + 1
        val end = text.length

        val add = Checklist.toggleLines(text, start, end)
        val added = text.apply(add)
        assertEquals("유\n$open 달걀", added.substring(Checklist.shifted(start, add), Checklist.shifted(end, add)))

        val remove = Checklist.toggleLines(added, added.indexOf("우유"), added.length)
        val removed = added.apply(remove)
        assertEquals("우유\n달걀", removed.substring(Checklist.shifted(added.indexOf("우유"), remove), Checklist.shifted(added.length, remove)))
        // 표시 안에 있던 자리는 줄 맨 앞으로
        assertEquals(removed.indexOf("우유"), Checklist.shifted(added.indexOf("우유") - 1, remove))
    }

    /** 첫 줄만 항목이면 켜짐이 아님. 누르면 표시가 생겨 Plus 확인을 거쳐야 함 */
    @Test
    fun only_items_means_every_picked_line() {
        val text = "$open 우유\n달걀"

        assertTrue(Checklist.picksOnlyItems(text, 0, 1))
        assertFalse(Checklist.picksOnlyItems(text, 0, text.length))
        assertTrue(Checklist.toggleLines(text, 0, text.length).any { it.start == it.end })
    }

    /** 글머리 목록으로 바꿀 때는 표시만 빼고 일반 줄은 그대로 */
    @Test
    fun removing_takes_marks_off_only_the_items() {
        val text = "$open 우유\n달걀\n$done 빵"

        assertEquals("우유\n달걀\n빵", text.apply(Checklist.removeLines(text, 0, text.length)))
    }

    /** 고른 끝이 다음 줄 맨 앞이면 그 줄은 바꾸지 않음 */
    @Test
    fun a_selection_ending_at_the_next_line_start_leaves_that_line() {
        val text = "우유\n달걀"

        assertEquals("$open 우유\n달걀", text.apply(Checklist.toggleLines(text, 0, text.indexOf("달걀"))))
        // 커서만 있으면 그 줄
        assertEquals("우유\n$open 달걀", text.apply(Checklist.toggleLines(text, text.indexOf("달걀"), text.indexOf("달걀"))))
    }

    @Test
    fun button_works_on_an_empty_note() {
        assertEquals("$open ", "".apply(Checklist.toggleLines("", 0, 0)))
    }

    @Test
    fun enter_continues_the_list() {
        val before = "$done 우유"
        val after = "$before\n"

        assertEquals(ChecklistEdit(after.length, after.length, "$open "), Checklist.afterEdit(before, after, after.length))
    }

    @Test
    fun enter_on_an_empty_item_ends_the_list() {
        val before = "$open 우유\n$open "
        val after = "$before\n"

        assertEquals("$open 우유\n", after.apply(listOfNotNull(Checklist.afterEdit(before, after, after.length))))
    }

    @Test
    fun deleting_the_space_takes_the_mark_away() {
        val before = "우유\n$open "
        val after = "우유\n$open"

        assertEquals("우유\n", after.apply(listOfNotNull(Checklist.afterEdit(before, after, after.length))))
    }

    @Test
    fun plain_typing_leaves_the_text_alone() {
        assertNull(Checklist.afterEdit("우유", "우유\n", 3))
        assertNull(Checklist.afterEdit("$open 우", "$open 우유", 4))
    }
}
