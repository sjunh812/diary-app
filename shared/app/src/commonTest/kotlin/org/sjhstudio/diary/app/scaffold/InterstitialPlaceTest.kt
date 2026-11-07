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

// 저장 뒤 전면 광고가 뜰 수 있는 자리(docs/business.md 광고 자리, 헌법 III).
class InterstitialPlaceTest {
    @Test
    fun it_shows_on_the_lists() {
        assertTrue(interstitialMayShow(listOf(HomeKey)))
        assertTrue(interstitialMayShow(listOf(CalendarKey)))
    }

    /** 돌아보기는 일기 목록이 아니다. 저장 뒤 레일로 옮겨 간 태블릿에서도 뜨지 않는다. */
    @Test
    fun it_never_shows_on_insight() {
        assertFalse(interstitialMayShow(listOf(InsightKey)))
    }

    @Test
    fun it_never_shows_over_a_screen_that_is_being_read_or_written() {
        assertFalse(interstitialMayShow(listOf(HomeKey, EntryDetailKey("e1"))))
        assertFalse(interstitialMayShow(listOf(HomeKey, WriteKey())))
        assertFalse(interstitialMayShow(listOf(HomeKey, SearchKey())))
    }

    /** 넓은 화면은 상세를 보다 레일로 설정에 갈 수 있다. 설정에는 광고가 없다. */
    @Test
    fun it_never_shows_on_settings() {
        assertFalse(interstitialMayShow(listOf(SettingsKey)))
    }

    @Test
    fun an_empty_stack_shows_nothing() {
        assertFalse(interstitialMayShow(emptyList()))
    }
}
