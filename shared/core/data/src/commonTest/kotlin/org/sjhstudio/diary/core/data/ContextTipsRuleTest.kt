package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 맥락 팁은 한 번에 하나, 하루에 하나(스펙 008 FR-010, FR-011)
class ContextTipsRuleTest {
    private val store = WelcomeStore(InMemoryDataStore(emptyPreferences()))
    private var today = LocalDate(2026, 11, 17)

    private suspend fun TestScope.repository() = DefaultWelcomeRepository(store, this, today = { today }) { null }.also { it.load() }

    @Test
    fun event_tips_stay_closed_until_opened() = runTest {
        val tips = repository()
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW))
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW_KEPT))
        tips.open(ContextTip.HOME_FULL_PREVIEW_KEPT)
        assertEquals(true, tips.isOpen(ContextTip.HOME_FULL_PREVIEW_KEPT))
    }

    @Test
    fun full_preview_tip_opens_after_three_clipped_opens() = runTest {
        val tips = repository()
        repeat(2) { tips.clippedOpened() }
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW))
        tips.clippedOpened()
        assertEquals(true, tips.isOpen(ContextTip.HOME_FULL_PREVIEW))
    }

    @Test
    fun only_one_tip_a_day() = runTest {
        val tips = repository()
        tips.open(ContextTip.HOME_FULL_PREVIEW_KEPT)
        assertEquals(true, tips.isOpen(ContextTip.HOME_LIST_STYLE))
        tips.shown(ContextTip.HOME_LIST_STYLE)
        tips.done(ContextTip.HOME_LIST_STYLE)
        // 같은 날 다른 팁은 없음
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW_KEPT))
        assertEquals(false, tips.isOpen(ContextTip.INSIGHT_OPEN))
        // 다음 날 보임
        today = today.plus(DatePeriod(days = 1))
        assertEquals(true, tips.isOpen(ContextTip.HOME_FULL_PREVIEW_KEPT))
    }

    @Test
    fun the_shown_tip_stays_open_that_day() = runTest {
        val tips = repository()
        tips.shown(ContextTip.HOME_LIST_STYLE)
        assertEquals(true, tips.isOpen(ContextTip.HOME_LIST_STYLE))
    }

    @Test
    fun day_rule_and_counts_survive_restart() = runTest {
        val first = repository()
        first.shown(ContextTip.INSIGHT_OPEN)
        repeat(3) { first.clippedOpened() }
        testScheduler.advanceUntilIdle()
        val next = repository()
        assertEquals(false, next.isOpen(ContextTip.HOME_LIST_STYLE))
        today = today.plus(DatePeriod(days = 1))
        assertEquals(true, next.isOpen(ContextTip.HOME_FULL_PREVIEW))
    }
}
