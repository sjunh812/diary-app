package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.diary.DefaultDraftRepository
import org.sjhstudio.diary.core.data.diary.entry
import org.sjhstudio.diary.core.data.diary.memoryDatabase
import org.sjhstudio.diary.core.data.diary.newDraft
import org.sjhstudio.diary.core.data.diary.photo
import org.sjhstudio.diary.core.data.diary.photoStore
import org.sjhstudio.diary.core.data.diary.seed
import org.sjhstudio.diary.core.data.diary.seedPhotos
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.TestClock

/**
 * 요금제마다 무엇이 달라지는지 표로 본다(`docs/business.md`, 스펙 006).
 * 표에 없는 것은 어느 요금제든 같다. 쓰던 기능은 그대로고 기록은 하나도 사라지지 않는다(헌법 I, III).
 */
class PlanTableTest {
    /** 표 한 줄. [ads]가 참이면 광고 자리, 전면 광고, 보상 광고가 모두 살아 있다. */
    private data class Row(
        val plan: Plan,
        val ads: Boolean,
        val photoLimit: Int,
        val canUpgradePhotos: Boolean,
        val checklist: Boolean,
        val advancedStats: Boolean
    )

    private val table =
        listOf(
            Row(Plan.FREE, ads = true, photoLimit = 10, canUpgradePhotos = true, checklist = false, advancedStats = false),
            Row(Plan.AD_FREE, ads = false, photoLimit = 10, canUpgradePhotos = true, checklist = false, advancedStats = false),
            Row(Plan.PLUS, ads = false, photoLimit = 30, canUpgradePhotos = false, checklist = true, advancedStats = true)
        )

    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val db = memoryDatabase()
    private val files = FakeFileSystem()
    private val testClock = TestClock(now)

    @AfterTest
    fun tearDown() = db.close()

    private class Player : AdPlayer {
        var prepared = 0
        var interstitials = 0
        var rewards = 0
        override val available = true

        override fun prepareInterstitial() {
            prepared++
        }

        override suspend fun showInterstitial(): Boolean {
            interstitials++
            return true
        }

        override suspend fun showRewarded(): Boolean {
            rewards++
            return true
        }
    }

    private class Ads(plan: Plan, private val clock: Clock) {
        val plans = FakePlanRepository(plan)
        val player = Player()

        /** 파일에 있는 값. 앱을 다시 켜도 남는다. */
        val file = InMemoryDataStore<Preferences>(emptyPreferences())

        /** 앱을 켤 때마다 새로 만든다. */
        fun repository() = AdRepository(plans, AdStore(file), player, { true }, clock, { TimeZone.UTC })
    }

    private fun drafts(limit: () -> Int) = DefaultDraftRepository(db, photoStore(files), testClock, limit)

    private fun image(text: String) = PickedImage { text.encodeToByteArray() }

    @Test
    fun every_plan_is_on_the_table() {
        assertEquals(Plan.entries.toSet(), table.map { it.plan }.toSet())
        for (row in table) {
            assertEquals(row.ads, row.plan.showsAds, "${row.plan} 광고")
            assertEquals(row.photoLimit, row.plan.photoLimit, "${row.plan} 사진")
            assertEquals(row.checklist, row.plan.checklist, "${row.plan} 체크리스트")
            assertEquals(row.advancedStats, row.plan.advancedStats, "${row.plan} 고급 통계")
        }
    }

    @Test
    fun ad_slots_follow_the_plan() = runTest {
        for (row in table) {
            assertEquals(row.ads, Ads(row.plan, clock).repository().showAds.first(), "${row.plan}")
        }
    }

    @Test
    fun interstitials_are_loaded_and_shown_only_for_plans_with_ads() = runTest {
        for (row in table) {
            val ads = Ads(row.plan, clock)
            val repository = ads.repository()

            repeat(3) {
                repository.afterSave()
                repository.backToList()
            }

            val expected = if (row.ads) 1 else 0
            assertEquals(expected, ads.player.interstitials, "${row.plan}는 하루 $expected 번")
            assertEquals(expected, ads.player.prepared, "${row.plan}는 받지도 않는다")
        }
    }

    @Test
    fun paid_plans_are_not_offered_a_rewarded_video() = runTest {
        for (row in table) {
            val ads = Ads(row.plan, clock)

            assertEquals(row.ads, ads.repository().watchReward(), "${row.plan}")
            assertEquals(if (row.ads) 1 else 0, ads.player.rewards, "${row.plan}")
        }
    }

    @Test
    fun new_photos_stop_at_the_plan_limit() = runTest {
        for (row in table) {
            val key = DraftKey.New("new-${row.plan.name}")
            val repository = drafts { row.plan.photoLimit }
            repository.save(newDraft().copy(key = key))

            val result = repository.addPhotos(key, List(row.photoLimit + 1) { image("${row.plan}-$it") })

            assertEquals(AddPhotosResult.LimitReached(row.photoLimit, row.canUpgradePhotos, added = row.photoLimit), result, "${row.plan}")
            assertEquals(row.photoLimit, repository.draft(key).first()?.photos?.size, "${row.plan}")
        }
    }

    /** 1.7.4는 한도가 없었다. 어느 요금제든 옮겨 온 사진은 모두 남고 고쳐 저장해도 그대로다. */
    @Test
    fun migrated_photos_stay_on_every_plan() = runTest {
        for (row in table) {
            val id = "old-${row.plan.name}"
            db.seed(entry(id).copy(legacyId = row.plan.ordinal))
            db.seedPhotos(*Array(35) { photo("$id-$it", id, it) })
            val repository = drafts { row.plan.photoLimit }
            assertNotNull(repository.openEdit(id))

            assertIs<AddPhotosResult.LimitReached>(repository.addPhotos(DraftKey.Edit(id), listOf(image("more"))), "${row.plan}")
            repository.commit(DraftKey.Edit(id))

            assertEquals(35, db.mediaDao().forEntry(id).count { it.status == MediaStatus.NORMAL.id }, "${row.plan}")
        }
    }

    /** 사진을 빼서 한도 아래로 내려가면 그때부터 다시 더할 수 있다(FR-006). */
    @Test
    fun an_entry_over_the_limit_opens_up_only_below_the_limit() = runTest {
        db.seed(entry("e1"))
        db.seedPhotos(*Array(11) { photo("p$it", "e1", it) })
        val repository = drafts { PhotoLimit.FREE }
        repository.openEdit("e1")
        val key = DraftKey.Edit("e1")

        repository.removePhoto(key, 0)
        assertIs<AddPhotosResult.LimitReached>(repository.addPhotos(key, listOf(image("a"))), "10장이면 아직 못 더한다")

        repository.removePhoto(key, 0)
        assertEquals(AddPhotosResult.Added(1, 0), repository.addPhotos(key, listOf(image("b"))))
        repository.commit(key)
        assertEquals(10, db.mediaDao().forEntry("e1").size)
    }

    /** 구독이 끝나도 30장 일기는 그대로고 저장할 수 있다. 새로 붙일 때만 10장 한도가 걸린다(US4). */
    @Test
    fun when_plus_ends_thirty_photos_stay_and_only_adding_stops() = runTest {
        val plans = FakePlanRepository(Plan.PLUS)
        val repository = drafts { plans.plan.value.photoLimit }
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), List(30) { image("p$it") })
        val id = repository.commit(DraftKey.New())

        plans.plan.value = Plan.FREE
        repository.openEdit(id)
        val key = DraftKey.Edit(id)
        assertEquals(AddPhotosResult.LimitReached(10, canUpgrade = true, added = 0), repository.addPhotos(key, listOf(image("more"))))
        repository.save(assertNotNull(repository.draft(key).first()).copy(bodyHtml = "<p>고침</p>"))
        repository.commit(key)

        assertEquals(30, db.mediaDao().forEntry(id).size)
    }

    /** 쓰던 임시 저장본도 마찬가지다. 구독이 끝났다고 저장을 막지 않는다. */
    @Test
    fun a_draft_with_many_photos_can_still_be_saved_after_plus_ends() = runTest {
        var limit = PhotoLimit.PLUS
        val repository = drafts { limit }
        repository.save(newDraft())
        repository.addPhotos(DraftKey.New(), List(15) { image("p$it") })

        limit = PhotoLimit.FREE
        val id = repository.commit(DraftKey.New())

        assertEquals(15, db.mediaDao().forEntry(id).size)
    }

    @Test
    fun ads_come_back_at_once_when_a_paid_plan_ends() = runTest {
        for (paid in listOf(Plan.AD_FREE, Plan.PLUS)) {
            val ads = Ads(paid, clock)
            val repository = ads.repository()
            repository.backToList()
            assertEquals(0, ads.player.interstitials)

            ads.plans.plan.value = Plan.FREE

            assertTrue(repository.showAds.first(), "$paid 환불이나 만료 뒤")
            repository.backToList()
            assertEquals(1, ads.player.interstitials, "$paid 만료 뒤")
        }
    }

    @Test
    fun buying_stops_ads_at_once_even_if_one_was_ready() = runTest {
        for (paid in listOf(Plan.AD_FREE, Plan.PLUS)) {
            val ads = Ads(Plan.FREE, clock)
            val repository = ads.repository()
            repository.afterSave()
            assertEquals(1, ads.player.prepared)

            ads.plans.plan.value = paid

            assertFalse(repository.showAds.first(), "$paid")
            repository.backToList()
            assertEquals(0, ads.player.interstitials, "$paid")
        }
    }

    @Test
    fun plus_buyer_who_owned_ad_free_keeps_no_ads_when_plus_ends() = runTest {
        val ads = Ads(Plan.PLUS, clock)
        val repository = ads.repository()

        // 광고 제거는 평생권이라 구독이 끝나면 광고 제거로 돌아간다.
        ads.plans.plan.value = Plan.AD_FREE

        assertFalse(repository.showAds.first())
        repository.backToList()
        assertEquals(0, ads.player.interstitials)
    }

    @Test
    fun todays_interstitial_is_remembered_after_a_restart() = runTest {
        val ads = Ads(Plan.FREE, clock)
        ads.repository().backToList()

        ads.repository().backToList()
        assertEquals(1, ads.player.interstitials)

        now += 1.days
        ads.repository().backToList()
        assertEquals(2, ads.player.interstitials)
    }

    @Test
    fun a_reward_is_remembered_after_a_restart_and_ends_on_time() = runTest {
        val ads = Ads(Plan.FREE, clock)
        assertTrue(ads.repository().watchReward())

        now += 23.hours
        assertFalse(ads.repository().showAds.first(), "다시 켜도 24시간은 광고가 없다")
        ads.repository().backToList()
        assertEquals(0, ads.player.interstitials)

        now += 2.hours
        assertTrue(ads.repository().showAds.first())
    }

    /** 보상이 끝나기 전에 산 사람도, 보상이 남은 채 구독이 끝난 사람도 표대로다. */
    @Test
    fun a_reward_outlives_the_plan_change_only_for_its_day() = runTest {
        val ads = Ads(Plan.FREE, clock)
        val repository = ads.repository()
        repository.watchReward()

        ads.plans.plan.value = Plan.PLUS
        now += 1.hours
        ads.plans.plan.value = Plan.FREE
        assertFalse(repository.showAds.first(), "보상이 남아 있으면 아직 광고가 없다")

        now += 24.hours
        assertTrue(repository.showAds.first())
    }
}
