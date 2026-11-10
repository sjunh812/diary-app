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
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey)))
    }

    @Test
    fun it_waits_on_the_detail_and_shows_back_on_the_list() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey, EntryDetailKey("e1"))))
        assertTrue(turn.takeTurn(HomeKey, listOf(HomeKey)))
    }

    @Test
    fun it_shows_only_once_per_save() {
        val turn = AfterSaveAdTurn()
        turn.saved(CalendarKey)
        assertTrue(turn.takeTurn(CalendarKey, listOf(CalendarKey)))
        assertNull(turn.savedOn)
        assertFalse(turn.takeTurn(CalendarKey, listOf(CalendarKey)))
    }

    /** 다른 탭으로 가면 기회를 버림. 나중에 홈으로 돌아와도 뜨지 않음 */
    @Test
    fun moving_to_another_tab_drops_the_turn() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(SettingsKey, listOf(SettingsKey)))
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey)))
    }

    @Test
    fun a_save_from_calendar_does_not_show_on_home() {
        val turn = AfterSaveAdTurn()
        turn.saved(CalendarKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey)))
    }

    /** 넓은 화면에서 상세가 열린 채 목록과 함께 보이면 상세 화면이라 띄우지 않음 */
    @Test
    fun the_detail_pane_still_open_keeps_it_waiting() {
        val turn = AfterSaveAdTurn()
        turn.saved(HomeKey)
        assertFalse(turn.takeTurn(HomeKey, listOf(HomeKey, EntryDetailKey("e1"))))
        assertTrue(turn.savedOn == HomeKey)
    }
}
