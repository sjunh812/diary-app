package org.sjhstudio.diary.app.scaffold

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
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
        var prepared = 0
        var shown = 0

        override suspend fun takeNotice() = notice.also { notice = false }

        override suspend fun prepare() {
            prepared++
        }

        override suspend fun backToList(): Boolean {
            if (due) shown++
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

    private fun shell(entries: EntryRepository = FakeEntryRepository(), ads: Ads = Ads(), screens: MutableList<String> = mutableListOf()) =
        AppShellViewModel(entries, FakePlanRepository(), ads, askReview = { reviews++ }) { screens += it }

    @Test
    fun review_is_asked_only_when_no_interstitial_was_due() = runTest(dispatcher) {
        backOnTheListAfterSaving(Ads(due = true))
        assertEquals(0, reviews)

        backOnTheListAfterSaving(Ads(due = false))
        assertEquals(1, reviews)
    }

    @Test
    fun no_review_while_the_first_ad_notice_shows() = runTest(dispatcher) {
        val shell = backOnTheListAfterSaving(Ads(notice = true, due = true))
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(0, reviews)
    }

    @Test
    fun going_to_write_withdraws_a_pending_review() = runTest(dispatcher) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val shell = AppShellViewModel(FakeEntryRepository(), FakePlanRepository(), Ads(due = false), askReview = {
            gate.await()
            reviews++
        }) {}
        shell.saved(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), twoPane = false)
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        advanceUntilIdle()
        shell.screenShown(WriteKey(), HomeKey, listOf(HomeKey, WriteKey()))
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(0, reviews)
    }

    @Test
    fun no_review_without_saving() = runTest(dispatcher) {
        val shell = shell(ads = Ads(due = false))
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        advanceUntilIdle()
        assertEquals(0, reviews)
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
    fun saving_prepares_and_shows_once_back_on_the_list() = runTest(dispatcher) {
        val ads = Ads()
        val shell = shell(ads = ads)
        shell.saved(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), twoPane = false)
        advanceUntilIdle()
        assertEquals(1, ads.prepared)
        assertEquals(0, ads.shown)

        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        shell.screenShown(EntryDetailKey("e2"), HomeKey, listOf(HomeKey, EntryDetailKey("e2")))
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        advanceUntilIdle()
        assertEquals(1, ads.shown)
    }

    @Test
    fun the_first_ad_waits_for_the_notice_and_shows_on_confirm() = runTest(dispatcher) {
        val ads = Ads(notice = true)
        val shell = backOnTheListAfterSaving(ads)
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
        val shell = backOnTheListAfterSaving(ads)

        shell.removeAdsFirst()
        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(AdNotice.AwaitingRemoval, shell.adNotice.value)
        assertEquals(0, ads.shown)

        shell.adRemovalClosed()
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(1, ads.shown)
    }

    private fun TestScope.backOnTheListAfterSaving(ads: Ads): AppShellViewModel {
        val shell = shell(ads = ads)
        shell.saved(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), twoPane = false)
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        advanceUntilIdle()
        return shell
    }

    @Test
    fun moving_to_another_tab_drops_the_turn() = runTest(dispatcher) {
        val ads = Ads()
        val shell = shell(ads = ads)
        shell.saved(HomeKey, listOf(HomeKey, EntryDetailKey("e1")), twoPane = false)
        shell.screenShown(CalendarKey, CalendarKey, listOf(CalendarKey))
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
        advanceUntilIdle()
        assertEquals(0, ads.shown)
    }

    @Test
    fun screens_report_only_the_key_name() = runTest(dispatcher) {
        val screens = mutableListOf<String>()
        val shell = shell(screens = screens)
        shell.screenShown(HomeKey, HomeKey, listOf(HomeKey))
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
