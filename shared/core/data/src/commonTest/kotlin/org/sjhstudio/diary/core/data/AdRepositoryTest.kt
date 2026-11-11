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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

        override suspend fun showRewarded() = reward
    }

    private val player = Player()
    private var enabled = true

    private fun repo() = AdRepository(plans, store, player, { enabled }, clock, { TimeZone.UTC })

    @Test
    fun no_interstitial_on_the_day_the_letter_was_read_and_back_the_next_day() = runTest {
        val repo = repo()

        repo.skipInterstitialToday()
        repo.afterSave()
        repo.backToList()
        assertEquals(0, player.shown)
        assertEquals(0, player.prepared)

        now += 1.days
        repo.backToList()
        assertEquals(1, player.shown)
    }

    @Test
    fun interstitial_shows_once_a_day() = runTest {
        val repo = repo()

        repo.backToList()
        repo.backToList()
        assertEquals(1, player.shown)

        now += 1.days
        repo.backToList()
        assertEquals(2, player.shown)
    }

    @Test
    fun clock_moved_back_never_shows_a_second_time() = runTest {
        val repo = repo()
        repo.backToList()

        now -= 2.days
        repo.backToList()

        assertEquals(1, player.shown)
    }

    @Test
    fun paid_plans_see_no_ads() = runTest {
        plans.plan.value = Plan.AD_FREE
        val repo = repo()

        assertFalse(repo.showAds.first())
        repo.backToList()
        assertEquals(0, player.shown)
    }

    @Test
    fun remote_switch_stops_the_interstitial_only() = runTest {
        enabled = false
        val repo = repo()

        assertTrue(repo.showAds.first())
        repo.afterSave()
        repo.backToList()
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
        repository.backToList()
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
        repository.backToList()
        assertEquals(0, player.shown)

        player.ready = true
        repository.backToList()
        assertEquals(1, player.shown)
    }

    /**
     * 광고가 화면에 나온 순간 오늘 본 것으로 기록
     * 보는 중에 앱이 꺼져도 다시 열었을 때 또 뜨지 않음
     */
    @Test
    fun the_day_is_written_when_the_ad_appears() = runTest {
        repo().backToList()

        assertEquals(null, player.dayBeforeImpression)
        assertEquals("2026-11-05", player.dayWhileShowing)
    }

    /** 광고가 나오기 전에 꺼졌으면 본 것이 아님. 다음에 목록으로 돌아오면 다시 띄움 */
    @Test
    fun an_ad_closed_before_it_appeared_shows_again_next_time() = runTest {
        val repository = repo()
        player.impression = false
        repository.backToList()
        assertEquals(null, store.current().lastInterstitialDay)

        player.impression = true
        repository.backToList()
        assertEquals(1, player.shown)
        assertEquals("2026-11-05", store.current().lastInterstitialDay)
    }

    @Test
    fun an_ad_that_could_not_show_leaves_no_record() = runTest {
        player.ready = false
        repo().backToList()

        assertEquals(null, store.current().lastInterstitialDay)
    }

    @Test
    fun a_new_day_shows_one_again_even_across_a_year() = runTest {
        now = Instant.parse("2026-12-31T12:00:00Z")
        val repository = repo()
        repository.backToList()
        now = Instant.parse("2027-01-01T01:00:00Z")
        repository.backToList()
        repository.backToList()
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
        repository.backToList()

        now = Instant.parse("2026-11-05T15:00:00Z")
        repository.backToList()

        assertEquals(2, player.shown, "서울은 자정이 지났다")
    }

    /** 시간대를 옮겨 날짜가 뒤로 가도 그 날짜는 이미 본 것으로 보아 덜 보이는 쪽으로 감 */
    @Test
    fun flying_west_does_not_show_a_second_time() = runTest {
        var zone = TimeZone.of("Asia/Seoul")
        val repository = AdRepository(plans, store, player, { true }, clock, { zone })
        now = Instant.parse("2026-11-05T15:30:00Z")
        repository.backToList()

        zone = TimeZone.of("America/Los_Angeles")
        repository.backToList()

        assertEquals(1, player.shown)
    }

    @Test
    fun a_day_already_shown_does_not_load_another_ad() = runTest {
        val repository = repo()
        repository.backToList()

        repository.afterSave()

        assertEquals(0, player.prepared)
    }

    @Test
    fun saving_loads_an_ad_when_one_is_due() = runTest {
        repo().afterSave()

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
        repository.backToList()
    }

    /** 광고를 받지 못한 날은 기록하지 않음. 다음 저장 때 다시 뜰 수 있음 */
    @Test
    fun a_day_without_an_ad_records_nothing() = runTest {
        player.ready = false
        repo().backToList()

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

    /** 오늘 전면 광고를 이미 넘긴 날은 미리 받지 않음 */
    @Test
    fun warm_up_does_not_fetch_an_interstitial_on_a_skipped_day() = runTest {
        val repo = repo()
        repo.skipInterstitialToday()
        repo.warmUp()
        assertEquals(1, player.warmed)
        assertEquals(0, player.prepared)
    }
}
