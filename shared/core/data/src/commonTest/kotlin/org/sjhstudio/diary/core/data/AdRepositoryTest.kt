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
        var reward = true
        var ready = true
        override val available = true

        override fun prepareInterstitial() = Unit

        override suspend fun showInterstitial(): Boolean {
            if (!ready) return false
            shown++
            return true
        }

        override suspend fun showRewarded() = reward
    }

    private val player = Player()
    private var enabled = true

    private fun repo() = AdRepository(plans, store, player, { enabled }, clock, { TimeZone.UTC })

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
    fun remote_switch_stops_ads() = runTest {
        enabled = false
        val repo = repo()

        assertFalse(repo.showAds.first())
        repo.backToList()
        assertEquals(0, player.shown)
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
}
