package org.sjhstudio.diary.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.IOException
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 광고는 앱의 곁일이다. 광고 쪽이 어떻게 실패해도 앱이 꺼지지 않고, 하루 한 번과 보상이 지켜져야 한다(헌법 III).
@OptIn(ExperimentalCoroutinesApi::class)
class AdRepositoryFailureTest {
    private val now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val plans = object : PlanRepository {
        override val plan = MutableStateFlow(Plan.FREE)
    }

    /** 끝나는 때를 시험이 정하는 광고. 전면 광고가 떠 있는 동안 화면이 다시 만들어지는 상황을 만든다. */
    private class HeldPlayer : AdPlayer {
        val closed = CompletableDeferred<Boolean>()
        var shown = 0
        var crash = false
        override val available = true

        override fun prepareInterstitial() {
            if (crash) error("광고 SDK 실패")
        }

        override suspend fun showInterstitial(): Boolean {
            if (crash) error("광고 SDK 실패")
            shown++
            return closed.await()
        }

        override suspend fun showRewarded(): Boolean {
            if (crash) error("광고 SDK 실패")
            return closed.await()
        }
    }

    /** 공간이 차서 쓰지 못하는 저장소. */
    private class FullDisk : DataStore<Preferences> {
        override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("공간 없음")
    }

    /** 읽지 못하는 저장소. */
    private class Unreadable : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("읽기 실패") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("읽기 실패")
    }

    private fun repo(player: AdPlayer, store: DataStore<Preferences> = InMemoryDataStore(emptyPreferences())) =
        AdRepository(plans, AdStore(store), player, { true }, clock, { TimeZone.UTC })

    /** 영상 광고를 가로로 보고 돌아오면 화면이 다시 만들어져 부른 쪽이 먼저 끝난다. 그래도 본 것은 본 것이다. */
    @Test
    fun an_interstitial_counts_when_the_screen_is_rebuilt_behind_it() = runTest(StandardTestDispatcher()) {
        val store = InMemoryDataStore<Preferences>(emptyPreferences())
        val player = HeldPlayer()
        val repository = repo(player, store)
        val job = launch { repository.backToList() }
        runCurrent()
        assertEquals(1, player.shown)

        job.cancel()
        player.closed.complete(true)
        advanceUntilIdle()

        assertNotNull(AdStore(store).current().lastInterstitialDay)
    }

    @Test
    fun a_finished_reward_is_kept_when_the_screen_is_rebuilt_behind_it() = runTest(StandardTestDispatcher()) {
        val store = InMemoryDataStore<Preferences>(emptyPreferences())
        val player = HeldPlayer()
        val repository = repo(player, store)
        val job = launch { repository.watchReward() }
        runCurrent()

        job.cancel()
        player.closed.complete(true)
        advanceUntilIdle()

        assertFalse(repository.showAds.first(), "끝까지 본 사람은 24시간 광고가 없다")
    }

    @Test
    fun a_full_disk_does_not_stop_the_app_or_show_a_second_interstitial() = runTest {
        val player = HeldPlayer().apply { closed.complete(true) }
        val repository = repo(player, FullDisk())

        repository.backToList()
        repository.backToList()

        assertEquals(1, player.shown)
    }

    @Test
    fun a_reward_that_cannot_be_written_says_it_failed() = runTest {
        val player = HeldPlayer().apply { closed.complete(true) }

        assertFalse(repo(player, FullDisk()).watchReward())
    }

    @Test
    fun an_unreadable_file_means_no_ads_and_no_crash() = runTest {
        val player = HeldPlayer().apply { closed.complete(true) }
        val repository = repo(player, Unreadable())

        assertFalse(repository.showAds.first())
        repository.afterSave()
        repository.backToList()
        assertFalse(repository.watchReward())
        assertEquals(0, player.shown)
    }

    @Test
    fun a_failing_ad_sdk_does_not_stop_the_app() = runTest {
        val player = HeldPlayer().apply { crash = true }
        val repository = repo(player)

        repository.afterSave()
        repository.backToList()

        assertFalse(repository.watchReward())
        assertTrue(repository.showAds.first(), "다음 기회에 다시 해 본다")
    }
}
