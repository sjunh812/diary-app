package org.sjhstudio.diary.app.scaffold

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.InsightKey
import org.sjhstudio.diary.app.navigation.SearchKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.WriteKey

// 저장 뒤 전면 광고가 뜰 수 있는 자리(docs/business.md 광고 자리, 헌법 III)
class InterstitialPlaceTest {
    @Test
    fun it_shows_on_the_lists() {
        assertTrue(interstitialMayShow(listOf(HomeKey), justSavedInTwoPane = false))
        assertTrue(interstitialMayShow(listOf(CalendarKey), justSavedInTwoPane = false))
    }

    /** 돌아보기는 일기 목록이 아님. 저장 뒤 레일로 옮겨 간 태블릿에서도 뜨지 않음 */
    @Test
    fun it_never_shows_on_insight() {
        assertFalse(interstitialMayShow(listOf(InsightKey), justSavedInTwoPane = false))
    }

    @Test
    fun it_never_shows_over_a_screen_that_is_being_read_or_written() {
        assertFalse(interstitialMayShow(listOf(HomeKey, EntryDetailKey("e1")), justSavedInTwoPane = false))
        assertFalse(interstitialMayShow(listOf(HomeKey, WriteKey()), justSavedInTwoPane = false))
        assertFalse(interstitialMayShow(listOf(HomeKey, SearchKey()), justSavedInTwoPane = false))
    }

    /** 넓은 화면은 상세를 보다 레일로 설정에 갈 수 있음. 설정에는 광고가 없음 */
    @Test
    fun it_never_shows_on_settings() {
        assertFalse(interstitialMayShow(listOf(SettingsKey), justSavedInTwoPane = false))
    }

    @Test
    fun an_empty_stack_shows_nothing() {
        assertFalse(interstitialMayShow(emptyList(), justSavedInTwoPane = false))
    }

    /** 두 칸에서 목록 옆에 상세 하나만 열렸으면 목록이 보임 */
    @Test
    fun two_panes_show_beside_a_detail_on_the_lists() {
        assertTrue(interstitialMayShow(listOf(HomeKey, EntryDetailKey("e1")), justSavedInTwoPane = true))
        assertTrue(interstitialMayShow(listOf(CalendarKey, EntryDetailKey("e1")), justSavedInTwoPane = true))
        assertTrue(interstitialMayShow(listOf(HomeKey), justSavedInTwoPane = true))
    }

    /** 두 칸이어도 찾기·쓰기·사진 보기 같은 다른 화면 위에는 뜨지 않음 */
    @Test
    fun two_panes_never_show_over_other_screens() {
        assertFalse(interstitialMayShow(listOf(HomeKey, WriteKey()), justSavedInTwoPane = true))
        assertFalse(interstitialMayShow(listOf(HomeKey, SearchKey(), EntryDetailKey("e1")), justSavedInTwoPane = true))
        assertFalse(interstitialMayShow(listOf(HomeKey, EntryDetailKey("e1"), EntryDetailKey("e2")), justSavedInTwoPane = true))
        assertFalse(interstitialMayShow(listOf(InsightKey, EntryDetailKey("e1")), justSavedInTwoPane = true))
        assertFalse(interstitialMayShow(listOf(SettingsKey), justSavedInTwoPane = true))
    }
}
