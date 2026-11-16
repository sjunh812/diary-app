package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class AdRepositoryTest {
    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val plans = object : PlanRepository {
        override val plan = MutableStateFlow(Plan.FREE)
    }
    private val store = AdStore(InMemoryDataStore(emptyPreferences()))

    private inner class Player : AdPlayer {
        var shown = 0
        var prepared = 0
        var warmed = 0
        var reward = true
        var ready = true
        var dayWhileShowing: String? = null
        var dayBeforeImpression: String? = null

        /** 거짓이면 광고가 화면에 나오기 전에 닫혔거나 앱이 꺼진 경우 */
        var impression = true
        override val available = true

        override fun warmUp() {
            warmed++
        }

        override fun prepareInterstitial() {
            prepared++
        }

        override suspend fun showInterstitial(onImpression: () -> Unit): Boolean {
            if (ready) dayBeforeImpression = store.current().lastInterstitialDay
            if (!ready || !impression) return false
            onImpression()
            yield()
            dayWhileShowing = store.current().lastInterstitialDay
            shown++
            return true
        }

        var rewardedShown = 0

        /** 있으면 영상을 받는 동안 이것이 열릴 때까지 기다림 */
        var loading: CompletableDeferred<Unit>? = null

        /** 있으면 끝까지 본 뒤 이것이 열릴 때까지 닫히지 않음(닫기 전에 앱이 꺼짐) */
        var closing: CompletableDeferred<Unit>? = null

        override suspend fun loadRewarded(): ReadyReward {
            // 영상을 받는 몇 초 동안
            loading?.await() ?: yield()
            return ReadyReward { onEarned ->
                rewardedShown++
                if (reward) onEarned()
                closing?.await()
            }
        }
    }

    private val player = Player()
    private var enabled = true

    private fun repo() = AdRepository(plans, store, player, { enabled }, clock, { TimeZone.UTC })

    @Test
    fun interstitial_shows_once_a_day() = runTest {
        val repo = repo()

        repo.showAfterSave()
        repo.showAfterSave()
        assertEquals(1, player.shown)

        now += 1.days
        repo.showAfterSave()
        assertEquals(2, player.shown)
    }

    @Test
    fun the_first_interstitial_gets_a_notice_only_once() = runTest {
        val repo = repo()

        // 답하기 전까지는 차례마다 다시 띄움
        assertTrue(repo.interstitialNoticeDue())
        assertTrue(repo.interstitialNoticeDue())
        repo.interstitialNoticeAnswered()
        assertFalse(repo.interstitialNoticeDue())
        repo.showAfterSave()
        assertEquals(1, player.shown)

        now += 1.days
        assertFalse(repo.interstitialNoticeDue())
    }

    @Test
    fun no_notice_when_no_ad_is_due() = runTest {
        plans.plan.value = Plan.AD_FREE
        assertFalse(repo().interstitialNoticeDue())

        plans.plan.value = Plan.FREE
        val repo = repo()
        repo.showAfterSave()
        assertFalse(repo.interstitialNoticeDue())
        assertFalse(store.current().interstitialNoticed)
    }

    @Test
    fun clock_moved_back_never_shows_a_second_time() = runTest {
        val repo = repo()
        repo.showAfterSave()

        now -= 2.days
        repo.showAfterSave()

        assertEquals(1, player.shown)
    }

    @Test
    fun paid_plans_see_no_ads() = runTest {
        plans.plan.value = Plan.AD_FREE
        val repo = repo()

        assertFalse(repo.showAds.first())
        repo.showAfterSave()
        assertEquals(0, player.shown)
    }

    @Test
    fun remote_switch_stops_the_interstitial_only() = runTest {
        enabled = false
        val repo = repo()

        assertTrue(repo.showAds.first())
        repo.prepareAfterSave()
        repo.showAfterSave()
        assertEquals(0, player.prepared)
        assertEquals(0, player.shown)
    }

    /** 재설치 직후 산 사람에게 광고가 스치면 안 됨 */
    @Test
    fun an_unknown_plan_shows_no_ads() = runTest {
        val unknown =
            object : PlanRepository {
                override val plan = MutableStateFlow(Plan.FREE)
                override val known = MutableStateFlow(false)
            }
        val repository = AdRepository(unknown, store, player, clock = clock, timeZone = { TimeZone.UTC })

        assertFalse(repository.showAds.first())
        repository.showAfterSave()
        assertEquals(0, player.shown)

        unknown.known.value = true
        assertTrue(repository.showAds.first())
    }

    @Test
    fun finished_reward_gives_a_day_without_ads() = runTest {
        val repo = repo()
        assertTrue(repo.showAds.first())

        assertTrue(repo.watchReward())
        assertFalse(repo.showAds.first())

        now += 25.hours
        assertTrue(repo().showAds.first())
    }

    /** 영상을 받는 동안 시트를 닫고 다시 [보기]를 눌러도 영상은 하나. 둘째는 첫째가 받은 보상을 그대로 알림 */
    @Test
    fun watching_twice_at_once_shows_one_video() = runTest {
        val repo = repo()

        val first = async { repo.watchReward() }
        val second = async { repo.watchReward() }

        assertTrue(first.await())
        assertTrue(second.await())
        assertEquals(1, player.rewardedShown)
    }

    /** 끝까지 본 순간 보상이 남음. 닫기 전에 앱이 꺼져도 하루 광고 없음을 잃지 않음 */
    @Test
    fun a_reward_is_kept_as_soon_as_it_is_earned_before_the_ad_closes() = runTest {
        player.closing = CompletableDeferred()
        val repo = repo()
        backgroundScope.launch { repo.watchReward() }
        runCurrent()
        assertFalse(repo.showAds.first())
    }

    /** 영상을 받는 동안 시트를 닫으면 띄우지 않음. 다른 화면 위에 광고가 뜨지 않게 */
    @Test
    fun closing_the_sheet_while_loading_never_shows_the_reward() = runTest {
        player.loading = CompletableDeferred()
        val repo = repo()
        val watch = launch { repo.watchReward() }
        runCurrent()
        watch.cancel()
        player.loading?.complete(Unit)
        runCurrent()
        assertEquals(0, player.rewardedShown)
        assertTrue(repo.showAds.first())
    }

    @Test
    fun closed_reward_gives_nothing() = runTest {
        player.reward = false
        val repo = repo()

        assertFalse(repo.watchReward())
        assertTrue(repo.showAds.first())
    }

    @Test
    fun declined_interstitial_stays_due_for_the_next_return() = runTest {
        val repository = repo()
        player.ready = false
        repository.showAfterSave()
        assertEquals(0, player.shown)

        player.ready = true
        repository.showAfterSave()
        assertEquals(1, player.shown)
    }

    /**
     * 광고가 화면에 나온 순간 오늘 본 것으로 기록
     * 보는 중에 앱이 꺼져도 다시 열었을 때 또 뜨지 않음
     */
    @Test
    fun the_day_is_written_when_the_ad_appears() = runTest {
        repo().showAfterSave()

        assertEquals(null, player.dayBeforeImpression)
        assertEquals("2026-11-05", player.dayWhileShowing)
    }

    /** 광고가 나오기 전에 꺼졌으면 본 것이 아님. 다음 저장에 다시 띄움 */
    @Test
    fun an_ad_closed_before_it_appeared_shows_again_next_time() = runTest {
        val repository = repo()
        player.impression = false
        repository.showAfterSave()
        assertEquals(null, store.current().lastInterstitialDay)

        player.impression = true
        repository.showAfterSave()
        assertEquals(1, player.shown)
        assertEquals("2026-11-05", store.current().lastInterstitialDay)
    }

    @Test
    fun an_ad_that_could_not_show_leaves_no_record() = runTest {
        player.ready = false
        repo().showAfterSave()

        assertEquals(null, store.current().lastInterstitialDay)
    }

    @Test
    fun a_new_day_shows_one_again_even_across_a_year() = runTest {
        now = Instant.parse("2026-12-31T12:00:00Z")
        val repository = repo()
        repository.showAfterSave()
        now = Instant.parse("2027-01-01T01:00:00Z")
        repository.showAfterSave()
        repository.showAfterSave()
        assertEquals(2, player.shown)
    }

    @Test
    fun reward_ends_after_a_day() = runTest {
        val repository = repo()
        player.reward = true
        assertTrue(repository.watchReward())
        assertFalse(repository.showAds.first())

        now += 25.hours
        assertTrue(repository.showAds.first())
    }

    @Test
    fun clock_moved_forward_before_a_reward_does_not_last_longer() = runTest {
        val repository = repo()
        now += 1000.hours
        player.reward = true
        assertTrue(repository.watchReward())
        now -= 1000.hours
        assertTrue(repository.showAds.first())
    }

    @Test
    fun buying_while_a_reward_runs_keeps_ads_off_after_it_ends() = runTest {
        val repository = repo()
        player.reward = true
        repository.watchReward()
        plans.plan.value = Plan.AD_FREE
        now += 25.hours
        assertFalse(repository.showAds.first())
    }

    /** 자정은 기기 시간대를 따름(`docs/business.md` 저장 뒤 하루 1번) */
    @Test
    fun the_day_changes_at_local_midnight() = runTest {
        val repository = AdRepository(plans, store, player, { true }, clock, { TimeZone.of("Asia/Seoul") })
        now = Instant.parse("2026-11-05T14:59:00Z")
        repository.showAfterSave()

        now = Instant.parse("2026-11-05T15:00:00Z")
        repository.showAfterSave()

        assertEquals(2, player.shown, "서울은 자정이 지났다")
    }

    /** 시간대를 옮겨 날짜가 뒤로 가도 그 날짜는 이미 본 것으로 보아 덜 보이는 쪽으로 감 */
    @Test
    fun flying_west_does_not_show_a_second_time() = runTest {
        var zone = TimeZone.of("Asia/Seoul")
        val repository = AdRepository(plans, store, player, { true }, clock, { zone })
        now = Instant.parse("2026-11-05T15:30:00Z")
        repository.showAfterSave()

        zone = TimeZone.of("America/Los_Angeles")
        repository.showAfterSave()

        assertEquals(1, player.shown)
    }

    @Test
    fun a_day_already_shown_does_not_load_another_ad() = runTest {
        val repository = repo()
        repository.showAfterSave()

        repository.prepareAfterSave()

        assertEquals(0, player.prepared)
    }

    @Test
    fun saving_loads_an_ad_when_one_is_due() = runTest {
        repo().prepareAfterSave()

        assertEquals(1, player.prepared)
    }

    @Test
    fun a_reward_ends_on_the_exact_moment() = runTest {
        val repository = repo()
        repository.watchReward()

        now += 24.hours

        assertTrue(repository.showAds.first())
    }

    /** iOS처럼 광고 구현이 없으면 어느 요금제든 광고 없음 */
    @Test
    fun without_an_ad_player_nothing_shows() = runTest {
        val repository = AdRepository(plans, store, NoAdPlayer, { true }, clock, { TimeZone.UTC })

        assertFalse(repository.showAds.first())
        assertFalse(repository.watchReward())
        repository.showAfterSave()
    }

    /** 광고를 받지 못한 날은 기록하지 않음. 다음 저장 때 다시 뜰 수 있음 */
    @Test
    fun a_day_without_an_ad_records_nothing() = runTest {
        player.ready = false
        repo().showAfterSave()

        assertEquals(null, store.current().lastInterstitialDay)
    }

    /** 보상 시각을 시계가 앞서 있을 때 적었어도 하루를 넘겨 쉬지 못함 */
    @Test
    fun a_reward_written_ahead_of_the_clock_is_ignored() = runTest {
        store.rewardUntil(now + 25.hours)

        assertTrue(repo().showAds.first())
    }

    /** 광고를 볼 사용자만 시작 뒤 SDK를 미리 켬. Plus는 켜지 않음 */
    @Test
    fun only_users_who_see_ads_warm_up_the_sdk() = runTest {
        repo().warmUp()
        assertEquals(1, player.warmed)
        // 오늘 전면 광고 차례가 남아 미리 받아 둠
        assertEquals(1, player.prepared)

        plans.plan.value = Plan.PLUS
        repo().warmUp()
        assertEquals(1, player.warmed)
    }

    @Test
    fun showing_after_save_tells_whether_an_interstitial_was_due() = runTest {
        val repo = repo()
        player.ready = false
        // 차례였지만 광고를 받지 못함. 그 순간 리뷰 창도 띄우지 않게 참
        assertTrue(repo.showAfterSave())
        player.ready = true
        assertTrue(repo.showAfterSave())
        // 오늘 이미 봄
        assertFalse(repo.showAfterSave())

        plans.plan.value = Plan.PLUS
        now += 1.days
        assertFalse(repo().showAfterSave())
    }

    @Test
    fun an_ad_sdk_crash_still_counts_as_the_ad_turn() = runTest {
        val crashing = object : AdPlayer by player {
            override suspend fun showInterstitial(onImpression: () -> Unit): Boolean = throw IllegalStateException("SDK")
        }
        val repo = AdRepository(plans, store, crashing, { enabled }, clock, { TimeZone.UTC })
        assertTrue(repo.showAfterSave())
    }
}
