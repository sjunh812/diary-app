package org.sjhstudio.diary.app.scaffold

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.SettingsKey

// 전면 광고는 일기를 저장하고 목록으로 돌아갈 때만 뜸(헌법 III, docs/business.md)
// 하루 한 번은 AdRepository 시험이 봄
class AfterSaveAdTurnTest {
    @Test
    fun nothing_shows_without_a_save() {
        val turn = AfterSaveAdTurn()
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey), justSavedInTwoPane = false))
    }

    @Test
    fun it_waits_on_the_detail_and_shows_back_on_the_list() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), justSavedInTwoPane = false))
        assertTrue(turn.takeTurn(HomeKey, listOf(HomeKey), justSavedInTwoPane = false))
    }

    @Test
    fun it_shows_only_once_per_save() {
        val turn = AfterSaveAdTurn()
        turn.saved(CalendarKey)
        assertTrue(turn.takeTurn(CalendarKey, listOf(CalendarKey), justSavedInTwoPane = false))
        assertNull(turn.savedOn)
        assertFalse(turn.takeTurn(CalendarKey, listOf(CalendarKey), justSavedInTwoPane = false))
    }

    /** 다른 탭으로 가면 기회를 버림. 나중에 홈으로 돌아와도 뜨지 않음 */
    @Test
    fun moving_to_another_tab_drops_the_turn() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(SettingsKey, listOf(SettingsKey), justSavedInTwoPane = false))
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey), justSavedInTwoPane = false))
    }

    @Test
    fun a_save_from_calendar_does_not_show_on_home() {
        val turn = AfterSaveAdTurn()
        turn.saved(CalendarKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey), justSavedInTwoPane = false))
    }

    /** 넓은 화면에서 상세가 열린 채 목록과 함께 보이면 상세 화면이라 띄우지 않음 */
    @Test
    fun the_detail_pane_still_open_keeps_it_waiting() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), justSavedInTwoPane = false))
        assertTrue(turn.savedOn == HomeKey)
    }

    /** 두 칸은 저장하면 목록 옆에 상세가 열림. 그때가 목록으로 돌아온 때 */
    @Test
    fun two_panes_take_the_turn_right_after_saving() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertTrue(turn.takeTurn(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), justSavedInTwoPane = true))
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey), justSavedInTwoPane = true))
    }
}
