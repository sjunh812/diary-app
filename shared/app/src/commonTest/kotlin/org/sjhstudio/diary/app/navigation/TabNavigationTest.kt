package org.sjhstudio.diary.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 탭마다 따로 쌓는 화면(contracts/navigation.md 뒤로 가기, 공식 nav3-recipes multiplestacks)
class TabNavigationTest {
    private fun navigation(start: TabScreen = HomeKey) = TabNavigation(
        selected = mutableStateOf(start),
        stacks = listOf(HomeKey, CalendarKey, InsightKey, SettingsKey).associateWith { mutableListOf<NavKey>(it) }
    )

    @Test
    fun moving_to_another_tab_keeps_the_screens_of_the_tab_you_left() {
        val nav = navigation()
        nav.push(EntryDetailKey("a"))

        nav.selectTab(CalendarKey)
        nav.selectTab(HomeKey)

        assertEquals(listOf<NavKey>(HomeKey, EntryDetailKey("a")), nav.current)
    }

    @Test
    fun opening_a_screen_from_a_notice_keeps_the_entry_being_written() {
        val nav = navigation()
        val writing = WriteKey()
        nav.push(writing)

        nav.openInTab(SettingsKey, FileBackupKey)

        assertEquals(listOf<NavKey>(SettingsKey, FileBackupKey), nav.current)
        nav.selectTab(HomeKey)
        assertEquals(listOf<NavKey>(HomeKey, writing), nav.current)
    }

    @Test
    fun tapping_the_tab_you_are_on_goes_back_to_its_first_screen() {
        val nav = navigation()
        nav.push(EntryDetailKey("a"))

        nav.selectTab(HomeKey)

        assertEquals(listOf<NavKey>(HomeKey), nav.current)
    }

    @Test
    fun back_from_a_tabs_first_screen_goes_home() {
        val nav = navigation()
        nav.selectTab(CalendarKey)

        nav.back()

        assertEquals(HomeKey, nav.tab)
    }

    @Test
    fun back_inside_a_tab_stays_in_that_tab() {
        val nav = navigation()
        nav.selectTab(InsightKey)
        nav.push(SearchKey())

        nav.back()

        assertEquals(InsightKey, nav.tab)
        assertEquals(listOf<NavKey>(InsightKey), nav.current)
    }

    @Test
    fun back_on_home_does_nothing_so_the_system_can_close_the_app() {
        val nav = navigation()

        nav.back()

        assertEquals(HomeKey, nav.tab)
        assertEquals(listOf<NavKey>(HomeKey), nav.current)
    }

    @Test
    fun home_stays_under_every_other_tab() {
        val nav = navigation()
        assertEquals(listOf<TabScreen>(HomeKey), nav.visibleTabs)

        nav.selectTab(SettingsKey)

        assertEquals(listOf(HomeKey, SettingsKey), nav.visibleTabs)
    }

    @Test
    fun an_app_opened_on_another_tab_still_leaves_through_home() {
        assertEquals(listOf(HomeKey, SettingsKey), navigation(start = SettingsKey).visibleTabs)
    }

    @Test
    fun opening_a_tab_from_elsewhere_starts_from_its_first_screen() {
        val nav = navigation()
        nav.selectTab(SettingsKey)
        nav.push(TrashKey)
        nav.selectTab(HomeKey)

        nav.openTab(SettingsKey)
        nav.push(BackupSettingsKey)

        assertEquals(listOf<NavKey>(SettingsKey, BackupSettingsKey), nav.current)
    }

    /** 복원이 끝나면 다른 탭에 쌓인 복원 화면과 상세가 남지 않음 */
    @Test
    fun starting_over_clears_every_tab() {
        val nav = navigation()
        nav.openEntry("a")
        nav.selectTab(SettingsKey)
        nav.push(RestoreProgressKey)

        nav.startOver(HomeKey)

        assertEquals(HomeKey, nav.tab)
        assertEquals(listOf<NavKey>(HomeKey), nav.current)
        assertEquals(listOf<NavKey>(SettingsKey), nav.stacks.getValue(SettingsKey))
    }

    @Test
    fun push_twice_keeps_one_screen() {
        val nav = navigation()

        nav.push(SearchKey())
        nav.push(SearchKey())

        assertEquals(2, nav.current.size)
    }

    /** 쓰기 화면 키는 열 때마다 새 값. 두 번 눌러도 쓰기 화면은 하나 */
    @Test
    fun two_writes_in_a_row_keep_one() {
        val nav = navigation()

        nav.push(WriteKey())
        nav.push(WriteKey())

        assertEquals(2, nav.current.size)
    }

    @Test
    fun a_new_detail_replaces_the_one_on_top() {
        val nav = navigation()
        nav.openEntry("a")

        nav.openEntry("b")

        assertEquals(listOf<NavKey>(HomeKey, EntryDetailKey("b")), nav.current)
    }

    /** 다른 탭에 같은 일기가 열려 있어도 지운 일기를 보이지 않음 */
    @Test
    fun a_trashed_entry_closes_in_every_tab() {
        val nav = navigation()
        nav.openEntry("a")
        nav.push(PhotoViewerKey("a", 0))
        nav.selectTab(CalendarKey)
        nav.openEntry("a")

        nav.closeEntry("a")

        assertEquals(listOf<NavKey>(CalendarKey), nav.current)
        assertEquals(listOf<NavKey>(HomeKey), nav.stacks.getValue(HomeKey))
    }

    @Test
    fun only_a_change_of_tab_is_marked_as_one() {
        val nav = navigation()

        nav.selectTab(CalendarKey)
        assertTrue(nav.movedBetweenTabs)

        nav.push(SearchKey())
        assertFalse(nav.movedBetweenTabs)

        nav.back()
        assertFalse(nav.movedBetweenTabs)

        nav.back()
        assertTrue(nav.movedBetweenTabs)
    }
}
