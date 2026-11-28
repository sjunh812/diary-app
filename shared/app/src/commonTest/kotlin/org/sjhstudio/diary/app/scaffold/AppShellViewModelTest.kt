package org.sjhstudio.diary.app.scaffold

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.app.ads.AdRemovalState
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.WriteKey
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository

// 앱 틀 규칙: 저장 뒤 광고 차례, 화면 통계, 휴지통 되돌리기, 홈 쓰기 버튼
@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class Ads(var notice: Boolean = false, var due: Boolean = true) : AfterSaveAds {
        val calls = mutableListOf<String>()
        val shown get() = calls.count { it == "shown" }

        override suspend fun noticeDue() = notice

        override suspend fun noticeAnswered() {
            notice = false
        }

        override suspend fun prepare() {
            calls += "prepared"
        }

        override suspend fun show(): Boolean {
            if (due) calls += "shown"
            return due
        }
    }

    private class Entries(base: FakeEntryRepository) : EntryRepository by base {
        val restored = mutableListOf<String>()

        override suspend fun restore(id: String) {
            restored += id
        }
    }

    private val oneEntry = mapOf(LocalDate(2026, 11, 10) to DayMood(LocalDate(2026, 11, 10), Mood.GOOD, 1))

    private var reviews = 0
    private val locked = MutableStateFlow(false)
    private val removal = FakeAdRemoval()

    private fun shell(
        entries: EntryRepository = FakeEntryRepository(),
        ads: AfterSaveAds = Ads(),
        screens: MutableList<String> = mutableListOf()
    ) = AppShellViewModel(entries, FakePlanRepository(), ads, removal, locked, askReview = { reviews++ }) { screens += it }

    private fun TestScope.savedWith(ads: AfterSaveAds): AppShellViewModel {
        val shell = shell(ads = ads)
        shell.saved("e1")
        advanceUntilIdle()
        return shell
    }

    @Test
    fun ad_visibility_is_known_before_an_ad_slot_reads_it() = runTest(dispatcher) {
        // 광고 자리는 화면에 돌아올 때마다 새로 그려짐. 처음 값이 거짓이면 자리가 비었다 생겨 스크롤이 맨 위로 감
        val showAds = MutableStateFlow(true)
        val shell = AppShellViewModel(FakeEntryRepository(), FakePlanRepository(), Ads(), removal, locked, showAds, askReview = {}) {}
        advanceUntilIdle()
        assertTrue(shell.showAds.value)
        showAds.value = false
        advanceUntilIdle()
        assertFalse(shell.showAds.value)
    }

    @Test
    fun opening_write_prepares_and_saving_shows_right_away() = runTest(dispatcher) {
        val ads = Ads()
        val shell = shell(ads = ads)
        shell.screenShown(HomeKey)
        advanceUntilIdle()
        assertEquals(emptyList(), ads.calls)

        // 쓰는 동안 받아 두고 저장한 순간 띄움
        shell.screenShown(WriteKey())
        advanceUntilIdle()
        shell.saved("e1")
        advanceUntilIdle()
        assertEquals(listOf("prepared", "shown"), ads.calls)
    }

    @Test
    fun every_save_asks_and_the_repository_keeps_once_a_day() = runTest(dispatcher) {
        val ads = Ads()
        val shell = savedWith(ads)
        ads.due = false
        shell.saved("e1")
        advanceUntilIdle()
        assertEquals(1, ads.shown)
        assertEquals(1, reviews)
    }

    @Test
    fun review_is_asked_only_when_no_interstitial_was_due() = runTest(dispatcher) {
        savedWith(Ads(due = true))
        assertEquals(0, reviews)

        savedWith(Ads(due = false))
        assertEquals(1, reviews)
    }

    @Test
    fun moving_on_right_after_saving_still_shows_the_ad() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var shown = 0
        val slowAds =
            object : AfterSaveAds {
                override suspend fun prepare() = Unit

                override suspend fun show(): Boolean {
                    gate.await()
                    shown++
                    return true
                }
            }
        val shell = shell(ads = slowAds)
        shell.saved("e1")
        advanceUntilIdle()
        shell.screenShown(SettingsKey)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, shown)
        assertEquals(0, reviews)
    }

    @Test
    fun leaving_the_saved_entry_withdraws_a_pending_review() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val shell = AppShellViewModel(FakeEntryRepository(), FakePlanRepository(), Ads(due = false), removal, locked, askReview = {
            gate.await()
            reviews++
        }) {}
        shell.saved("e1")
        shell.screenShown(EntryDetailKey("e1"))
        advanceUntilIdle()
        // 리뷰 정보를 받는 사이 쓰기를 엶
        shell.screenShown(WriteKey())
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(0, reviews)
    }

    @Test
    fun staying_on_the_saved_entry_keeps_the_review() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val shell = AppShellViewModel(FakeEntryRepository(), FakePlanRepository(), Ads(due = false), removal, locked, askReview = {
            gate.await()
            reviews++
        }) {}
        shell.saved("e1")
        shell.screenShown(EntryDetailKey("e1"))
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, reviews)
    }

    @Test
    fun no_review_while_the_first_ad_notice_shows() = runTest(dispatcher) {
        val shell = savedWith(Ads(notice = true, due = true))
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(0, reviews)
    }

    @Test
    fun no_review_without_saving() = runTest(dispatcher) {
        val shell = shell(ads = Ads(due = false))
        shell.screenShown(HomeKey)
        advanceUntilIdle()
        assertEquals(0, reviews)
    }

    @Test
    fun the_first_ad_waits_for_the_notice_and_shows_on_confirm() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)
        assertEquals(AdNotice.Showing, shell.adNotice.value)
        assertEquals(0, ads.shown)

        shell.confirmAdNotice()
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(1, ads.shown)
    }

    @Test
    fun remove_ads_holds_the_ad_until_the_sheet_closes() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)

        shell.removeAdsFirst()
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(AdNotice.AwaitingRemoval, shell.adNotice.value)
        assertEquals(0, ads.shown)

        removal.close()
        removal.close()
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(1, ads.shown)
    }

    /** 시트를 연 채 잠기면 잠금을 풀어도 광고가 뜨지 않음 */
    @Test
    fun locking_with_the_removal_sheet_open_drops_the_ad_turn() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)
        shell.removeAdsFirst()
        locked.value = true
        advanceUntilIdle()
        // 잠금이 시트를 닫음
        removal.close()
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(0, ads.shown)
    }

    /** 안내에 답하기 전에 잠기면 광고 없이 끝나고, 다음 저장에 안내부터 다시 뜸 */
    @Test
    fun locking_while_the_notice_shows_brings_it_back_on_the_next_save() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)
        locked.value = true
        advanceUntilIdle()
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(0, ads.shown)
        assertTrue(ads.notice)

        locked.value = false
        shell.saved("e2")
        advanceUntilIdle()
        assertEquals(AdNotice.Showing, shell.adNotice.value)
    }

    /** [광고 없애기]로 연 시트를 기다리는 동안 잠기면 시트가 닫혀도 광고가 뜨지 않음 */
    @Test
    fun locking_while_the_removal_sheet_is_open_ends_the_ad_turn() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)
        shell.removeAdsFirst()
        advanceUntilIdle()
        assertEquals(1, removal.opened)

        locked.value = true
        advanceUntilIdle()
        removal.close()
        advanceUntilIdle()
        assertEquals(0, ads.shown)
        assertEquals(AdNotice.None, shell.adNotice.value)
    }

    /** 보상 영상을 보는 동안은 시트를 닫아도 전면 광고를 미룸 */
    @Test
    fun the_ad_waits_until_the_reward_video_ends() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = savedWith(ads)
        shell.removeAdsFirst()
        removal.state.value = AdRemovalState(watching = true)
        advanceUntilIdle()
        assertEquals(0, ads.shown)

        removal.close()
        advanceUntilIdle()
        assertEquals(1, ads.shown)
    }

    /** 안내를 띄울지 읽는 사이 잠기면 잠금을 풀어도 안내가 뜨지 않음 */
    @Test
    fun locking_while_the_notice_is_checked_shows_nothing() = runTest(dispatcher) {
        val checking = CompletableDeferred<Boolean>()
        val ads =
            object : AfterSaveAds {
                override suspend fun prepare() = Unit

                override suspend fun show() = true

                override suspend fun noticeDue() = checking.await()
            }
        val shell = shell(ads = ads)
        shell.saved("e1")
        advanceUntilIdle()
        locked.value = true
        advanceUntilIdle()
        checking.complete(true)
        advanceUntilIdle()
        locked.value = false
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
    }

    @Test
    fun either_button_on_the_notice_counts_as_answered() = runTest(dispatcher) {
        val confirmed = Ads(notice = true)
        savedWith(confirmed).confirmAdNotice()
        advanceUntilIdle()
        assertFalse(confirmed.notice)

        val removing = Ads(notice = true)
        savedWith(removing).removeAdsFirst()
        advanceUntilIdle()
        assertFalse(removing.notice)
        assertEquals(0, removing.shown)
    }

    @Test
    fun the_home_button_waits_until_entries_are_read() = runTest(dispatcher) {
        val empty = shell()
        assertNull(empty.hasEntries.value)
        backgroundScope.launch { empty.hasEntries.collect {} }
        advanceUntilIdle()
        assertEquals(false, empty.hasEntries.value)

        val some = shell(FakeEntryRepository(oneEntry))
        backgroundScope.launch { some.hasEntries.collect {} }
        advanceUntilIdle()
        assertEquals(true, some.hasEntries.value)
    }

    @Test
    fun screens_report_only_the_key_name() = runTest(dispatcher) {
        val screens = mutableListOf<String>()
        val shell = shell(screens = screens)
        shell.screenShown(HomeKey)
        assertEquals(listOf(HomeKey.toString()), screens)
    }

    @Test
    fun undo_restores_the_trashed_entry() = runTest(dispatcher) {
        val entries = Entries(FakeEntryRepository())
        val shell = shell(entries)
        shell.restoreTrashed("e1")
        advanceUntilIdle()
        assertEquals(listOf("e1"), entries.restored)
    }
}
