package org.sjhstudio.diary.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
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

    /** 같은 화면이 후보를 다시 고르도록 팁이 열리고 닫히면 알림 */
    @Test
    fun revision_changes_when_tips_open_or_close() = runTest {
        val tips = repository()
        val start = tips.revision.value
        repeat(3) { tips.clippedOpened() }
        val opened = tips.revision.value
        tips.done(ContextTip.HOME_FULL_PREVIEW)
        kotlin.test.assertTrue(opened > start)
        kotlin.test.assertTrue(tips.revision.value > opened)
    }

    /** 옛 설정을 이었다고 알린 사람에게는 전체 보기를 다시 권하지 않음 */
    @Test
    fun kept_tip_retires_the_full_preview_tip() = runTest {
        val tips = repository()
        tips.open(ContextTip.HOME_FULL_PREVIEW_KEPT)
        tips.done(ContextTip.HOME_FULL_PREVIEW_KEPT)
        repeat(3) { tips.clippedOpened() }
        today = today.plus(DatePeriod(days = 1))
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW))
        testScheduler.advanceUntilIdle()
        // 다시 켜도 그대로
        repeat(3) { repository().clippedOpened() }
        assertEquals(false, repository().isOpen(ContextTip.HOME_FULL_PREVIEW))
    }

    /** 시작 단계가 기록을 읽기 전에 옛 설정 팁을 열어도 같음 */
    @Test
    fun kept_tip_opened_before_load_retires_the_full_preview_tip() = runTest {
        val tips = DefaultWelcomeRepository(store, this, today = { today }) { null }
        tips.open(ContextTip.HOME_FULL_PREVIEW_KEPT)
        tips.load()
        repeat(3) { tips.clippedOpened() }
        assertEquals(false, tips.isOpen(ContextTip.HOME_FULL_PREVIEW))
    }

    /** 기록을 읽는 동안 화면이 먼저 보인 오늘 팁을, 늦게 끝난 읽기가 되돌리지 않음 */
    @Test
    fun loads_finishing_late_keep_the_tip_shown_meanwhile() = runTest {
        val gate = CompletableDeferred<Unit>()
        val inner = InMemoryDataStore(emptyPreferences())
        // 적기가 실패해 기록에는 남지 않고 메모리에만 있는 경우
        inner.failWrites = true
        val gated =
            object : DataStore<Preferences> by inner {
                override val data = flow {
                    gate.await()
                    emitAll(inner.data)
                }
            }
        val tips = DefaultWelcomeRepository(WelcomeStore(gated), this, today = { today }) { null }
        val first = async { tips.load() }
        val second = async { tips.load() }
        runCurrent()
        tips.shown(ContextTip.HOME_LIST_STYLE)
        gate.complete(Unit)
        first.await()
        second.await()
        assertEquals(false, tips.isOpen(ContextTip.INSIGHT_OPEN))
        assertEquals(true, tips.isOpen(ContextTip.HOME_LIST_STYLE))
    }
}
