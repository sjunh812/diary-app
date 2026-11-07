package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
