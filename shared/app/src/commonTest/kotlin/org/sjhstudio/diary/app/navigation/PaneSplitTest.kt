package org.sjhstudio.diary.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 넓은 화면 두 칸 나누기(`docs/design.md` 넓은 화면)
class PaneSplitTest {
    private val other: Map<String, Any> = emptyMap()

    @Test
    fun a_list_alone_shows_the_empty_detail() {
        val split = paneSplit(listOf(ListPane))
        assertEquals(0, split?.list)
        assertEquals(0, split?.details)
    }

    @Test
    fun an_entry_opens_beside_home_and_search() {
        assertEquals(1, paneSplit(listOf(ListPane, DetailPane))?.details)
        assertEquals(1, paneSplit(listOf(ListPane, ListPane, DetailPane))?.list)
    }

    @Test
    fun a_deeper_setting_stays_on_the_right() {
        val split = paneSplit(listOf(SettingsListPane, SettingsDetailPane, SettingsDetailPane))
        assertEquals(0, split?.list)
        assertEquals(2, split?.details)
    }

    @Test
    fun a_setting_does_not_open_beside_the_home_list() {
        assertNull(paneSplit(listOf(ListPane, SettingsDetailPane)))
    }

    @Test
    fun a_full_screen_on_top_is_not_split() {
        assertNull(paneSplit(listOf(ListPane, DetailPane, FullScreenEntry)))
        assertNull(paneSplit(listOf(SettingsListPane, SettingsDetailPane, other)))
    }

    @Test
    fun a_screen_without_a_list_is_not_split() {
        assertNull(paneSplit(listOf(other, DetailPane)))
    }
}
