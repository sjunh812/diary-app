package org.sjhstudio.diary.app.scaffold

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.SearchKey
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 첫 전면 광고 안내부터 광고까지 실제 광고 규칙(AdRepository)으로 따라감
// 결제·보상·폴더블·태블릿에서 광고가 잘못 뜨거나 빠지면 안 됨(docs/business.md 광고 자리)
@OptIn(ExperimentalCoroutinesApi::class)
class FirstAdFlowTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private var now = Instant.parse("2026-11-15T01:00:00Z")
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val plans = FakePlanRepository()
    private val store = AdStore(InMemoryDataStore(emptyPreferences()))

    private class Player : AdPlayer {
        var shown = 0
        var reward = true
        override val available = true

        override fun prepareInterstitial() = Unit

        override suspend fun showInterstitial(onImpression: () -> Unit): Boolean {
            onImpression()
            shown++
            return true
        }

        override suspend fun showRewarded() = reward
    }

    private val player = Player()
    private val ads = AdRepository(plans, store, player, clock = clock, timeZone = { TimeZone.UTC })

    // 시험 디스패처를 건 뒤에 만들어야 화면 모델의 작업이 시험 시계를 따름
    private val shell by lazy {
        AppShellViewModel(
            FakeEntryRepository(),
            plans,
            object : AfterSaveAds {
                override suspend fun prepare() = ads.afterSave()

                override suspend fun backToList() = ads.backToList()

                override suspend fun takeNotice() = ads.takeInterstitialNotice()
            }
        ) {}
    }

    private val detail = listOf(HomeKey, EntryDetailKey("e1"))
    private val list = listOf(HomeKey)

    /** 폰: 저장하면 상세, 뒤로 가면 목록 */
    private fun TestScope.saveOnPhoneAndGoBack() {
        shell.saved(HomeKey, detail, twoPane = false)
        shell.screenShown(EntryDetailKey("e1"), HomeKey, detail)
        shell.screenShown(HomeKey, HomeKey, list)
        advanceUntilIdle()
    }

    @Test
    fun the_notice_comes_once_then_the_ad_on_confirm() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        assertEquals(AdNotice.Showing, shell.adNotice.value)
        assertEquals(0, player.shown)

        shell.confirmAdNotice()
        advanceUntilIdle()
        assertEquals(1, player.shown)

        // 다음 날은 안내 없이 광고만
        now += 1.days
        saveOnPhoneAndGoBack()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(2, player.shown)
    }

    @Test
    fun buying_ad_removal_from_the_notice_shows_no_ad() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        // 결제가 끝나면 시트가 닫히기 전에 요금제가 바뀜(StoreBilling.buy)
        plans.plan.value = Plan.AD_FREE
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(0, player.shown)

        // 산 뒤로는 저장해도 안내도 광고도 없음
        now += 1.days
        saveOnPhoneAndGoBack()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(0, player.shown)
    }

    @Test
    fun buying_plus_from_the_notice_shows_no_ad() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        plans.plan.value = Plan.PLUS
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(0, player.shown)
    }

    @Test
    fun watching_the_reward_video_shows_no_interstitial() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        ads.watchReward()
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(0, player.shown)
    }

    /** 취소·실패·결제 대기도 요금제가 그대로라 같음 */
    @Test
    fun closing_the_sheet_without_buying_shows_the_ad() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(1, player.shown)
    }

    @Test
    fun a_failed_reward_video_still_shows_the_ad() = runTest(dispatcher) {
        player.reward = false
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        ads.watchReward()
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(1, player.shown)
    }

    /** 재설치 직후처럼 요금제를 모르면 산 사람일 수 있어 안내도 광고도 없음 */
    @Test
    fun an_unknown_plan_shows_nothing() = runTest(dispatcher) {
        plans.known.value = false
        saveOnPhoneAndGoBack()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(0, player.shown)
        assertEquals(false, store.current().interstitialNoticed)
    }

    @Test
    fun tablet_two_panes_take_the_turn_right_after_saving() = runTest(dispatcher) {
        shell.saved(HomeKey, detail, twoPane = true)
        advanceUntilIdle()
        assertEquals(AdNotice.Showing, shell.adNotice.value)

        shell.confirmAdNotice()
        // 그 뒤 상세를 닫거나 다른 일기를 열어도 다시 뜨지 않음
        shell.screenShown(HomeKey, HomeKey, list)
        shell.screenShown(EntryDetailKey("e2"), HomeKey, listOf(HomeKey, EntryDetailKey("e2")))
        advanceUntilIdle()
        assertEquals(1, player.shown)
    }

    @Test
    fun tablet_calendar_takes_the_turn_too() = runTest(dispatcher) {
        shell.saved(CalendarKey, listOf(CalendarKey, EntryDetailKey("e1")), twoPane = true)
        advanceUntilIdle()
        assertEquals(AdNotice.Showing, shell.adNotice.value)
    }

    /** 찾기에서 연 일기를 고쳐 저장하면 목록이 아니라 찾기 위라 뜨지 않음 */
    @Test
    fun saving_from_search_waits_for_the_list() = runTest(dispatcher) {
        shell.saved(HomeKey, listOf(HomeKey, SearchKey(), EntryDetailKey("e1")), twoPane = true)
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)

        shell.screenShown(HomeKey, HomeKey, list)
        advanceUntilIdle()
        assertEquals(AdNotice.Showing, shell.adNotice.value)
    }

    /** 접은 채 저장하고 편 뒤 다른 일기를 열어도 뜨지 않음. 목록으로 돌아갈 때 뜸 */
    @Test
    fun unfolding_after_saving_folded_waits_for_the_list() = runTest(dispatcher) {
        shell.saved(HomeKey, detail, twoPane = false)
        shell.screenShown(EntryDetailKey("e1"), HomeKey, detail)
        // 펴고 목록에서 다른 일기를 누름
        shell.screenShown(EntryDetailKey("e2"), HomeKey, listOf(HomeKey, EntryDetailKey("e2")))
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)

        shell.screenShown(HomeKey, HomeKey, list)
        advanceUntilIdle()
        assertEquals(AdNotice.Showing, shell.adNotice.value)
    }

    /** 편 채 저장하면 그 자리에서 차례를 씀. 접어도 다시 생기지 않음 */
    @Test
    fun folding_after_saving_unfolded_does_not_bring_a_second_turn() = runTest(dispatcher) {
        shell.saved(HomeKey, detail, twoPane = true)
        advanceUntilIdle()
        shell.confirmAdNotice()
        // 접으면 상세가 한 칸으로, 뒤로 가면 목록
        shell.screenShown(HomeKey, HomeKey, list)
        advanceUntilIdle()
        assertEquals(AdNotice.None, shell.adNotice.value)
        assertEquals(1, player.shown)
    }

    @Test
    fun tapping_twice_shows_one_ad() = runTest(dispatcher) {
        saveOnPhoneAndGoBack()
        shell.removeAdsFirst()
        shell.removeAdsFirst()
        shell.confirmAdNotice()
        shell.adRemovalClosed()
        shell.adRemovalClosed()
        advanceUntilIdle()
        assertEquals(1, player.shown)
    }
}
