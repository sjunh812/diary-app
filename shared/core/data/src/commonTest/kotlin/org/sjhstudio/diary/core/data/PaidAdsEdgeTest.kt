package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 산 사람에게 광고가 다시 나오는 경계 상황. 늦게 온 옛 답, 늦게 반영된 갱신
@OptIn(ExperimentalCoroutinesApi::class)
class PaidAdsEdgeTest {
    private val stored = ArrayList<Plan>()

    private inner class Store(private val now: () -> Instant) : StoreClient {
        var owned: Set<String> = emptySet()
        var expiresAt: Instant? = null

        /** 기간이 끝났지만 자동 갱신이 켜진 권한 */
        var renewing: Map<String, Instant> = emptyMap()

        fun answer() = StoreAnswer(owned, expiresAt, now(), renewing)

        override suspend fun entitlements() = answer()

        override suspend fun sync() = answer()

        override suspend fun prices(storeIds: List<String>): Map<String, Price> = emptyMap()

        override suspend fun purchase(storeId: String): StoreAnswer {
            owned = owned + if (storeId == StoreIds.PLUS_PRODUCT) StoreIds.PLUS_ENTITLEMENT else StoreIds.AD_FREE_ENTITLEMENT
            return answer()
        }

        override suspend fun restore() = answer()
    }

    private class Player : AdPlayer {
        var shown = 0
        override val available = true

        override fun prepareInterstitial() = Unit

        override suspend fun showInterstitial(onImpression: () -> Unit): Boolean {
            onImpression()
            shown++
            return true
        }

        override suspend fun loadRewarded(): ReadyReward? = null
    }

    private fun TestScope.clock() = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(testScheduler.currentTime)
    }

    private fun TestScope.store() = clock().let { c -> Store(c::now) }

    private fun TestScope.billing(store: Store, saved: Plan? = Plan.FREE) =
        StoreBilling(store, saved, "manage", backgroundScope, clock()) { stored += it }

    private fun TestScope.ads(plans: PlanRepository, player: Player) =
        AdRepository(plans, AdStore(InMemoryDataStore(emptyPreferences())), player, clock = clock(), timeZone = { TimeZone.UTC })

    /**
     * 결제 전에 보낸 확인의 답이 결제 뒤에 스토어 알림(RevenueCat 리스너·delegate)으로 들어옴
     * [StoreBilling.refresh]로 들어오면 버리지만, 같은 옛 답이 [StoreBilling.onUpdate]로 오면 결제를 덮음
     */
    @Test
    fun an_old_store_update_after_buying_plus_does_not_bring_ads_back() = runTest {
        val store = store()
        val billing = billing(store)
        val oldAnswer = store.answer()
        advanceTimeBy(1.seconds)

        assertEquals(PurchaseResult.DONE, billing.buy(ProductId.PLUS_YEARLY))
        billing.onUpdate(oldAnswer)

        assertEquals(Plan.PLUS, billing.plan.value)
        assertEquals(Plan.PLUS, stored.last())
        val player = Player()
        ads(billing, player).showAfterSave()
        assertEquals(0, player.shown)
    }

    /** 재설치 뒤 [구매 복원]으로 Plus를 가져온 다음, 그 전에 보낸 확인의 답이 스토어 알림으로 옴 */
    @Test
    fun an_old_store_update_after_restoring_does_not_bring_ads_back() = runTest {
        val store = store()
        val billing = billing(store, saved = null)
        val oldAnswer = store.answer()
        advanceTimeBy(1.seconds)

        store.owned = setOf(StoreIds.PLUS_ENTITLEMENT)
        assertEquals(PurchaseResult.DONE, billing.restore())
        billing.onUpdate(oldAnswer)

        assertEquals(Plan.PLUS, billing.plan.value)
        val player = Player()
        ads(billing, player).showAfterSave()
        assertEquals(0, player.shown)
    }

    /** 광고 제거와 Plus를 둘 다 가지면 Plus, Plus가 끝나도 광고 제거는 남음 */
    @Test
    fun owning_both_is_plus_and_ad_free_stays_after_plus_ends() = runTest {
        val store = store()
        store.owned = setOf(StoreIds.AD_FREE_ENTITLEMENT, StoreIds.PLUS_ENTITLEMENT)
        store.expiresAt = Instant.fromEpochMilliseconds(testScheduler.currentTime) + 60.seconds
        val billing = billing(store)
        billing.refresh()
        assertEquals(Plan.PLUS, billing.plan.value)

        store.owned = setOf(StoreIds.AD_FREE_ENTITLEMENT)
        store.expiresAt = null
        advanceTimeBy(2.minutes)
        assertEquals(Plan.AD_FREE, billing.plan.value)
        val player = Player()
        ads(billing, player).showAfterSave()
        assertEquals(0, player.shown)
    }

    /** 연 구독 갱신이 만료 시각 5초 뒤까지 스토어에 반영되지 않아도(Play 갱신 알림이 늦게 옴) 광고가 나오지 않음 */
    @Test
    fun a_renewal_the_store_reports_late_does_not_show_ads_to_a_plus_subscriber() = runTest {
        val store = store()
        val end = Instant.fromEpochMilliseconds(testScheduler.currentTime) + 60.seconds
        store.owned = setOf(StoreIds.PLUS_ENTITLEMENT)
        store.expiresAt = end
        val billing = billing(store, Plan.PLUS)
        billing.refresh()

        // 만료 시각 + 5초에 다시 물을 때는 아직 갱신 전. 자동 갱신은 켜져 있음
        store.owned = emptySet()
        store.expiresAt = null
        store.renewing = mapOf(StoreIds.PLUS_ENTITLEMENT to end)
        advanceTimeBy(66.seconds)
        // 30초 뒤 갱신 반영
        store.owned = setOf(StoreIds.PLUS_ENTITLEMENT)
        store.expiresAt = end + 365.days
        store.renewing = emptyMap()
        advanceTimeBy(30.seconds)

        // 앱을 켜 둔 채 일기를 저장
        advanceTimeBy(5.minutes)
        assertEquals(Plan.PLUS, billing.plan.value)
        val player = Player()
        ads(billing, player).showAfterSave()
        assertEquals(0, player.shown)
        assertFalse(Plan.FREE in stored)
    }

    /** 갱신이 끝내 오지 않으면(결제 수단 문제로 보류) 기다림이 끝날 때 무료로 돌아감 */
    @Test
    fun a_renewal_that_never_comes_turns_free_after_the_wait() = runTest {
        val store = store()
        val end = Instant.fromEpochMilliseconds(testScheduler.currentTime) + 60.seconds
        store.owned = setOf(StoreIds.PLUS_ENTITLEMENT)
        store.expiresAt = end
        val billing = billing(store, Plan.PLUS)
        billing.refresh()

        store.owned = emptySet()
        store.expiresAt = null
        store.renewing = mapOf(StoreIds.PLUS_ENTITLEMENT to end)
        advanceTimeBy(66.seconds)
        assertEquals(Plan.PLUS, billing.plan.value)

        advanceTimeBy(6.hours)
        assertEquals(Plan.FREE, billing.plan.value)
        assertEquals(Plan.FREE, stored.last())
    }

    /** 결제 승인 대기 뒤 승인되면 스토어 알림으로 켜지고 광고가 멈춤 */
    @Test
    fun an_approved_pending_payment_stops_ads() = runTest {
        val store = store()
        val billing = billing(store)
        store.owned = setOf(StoreIds.AD_FREE_ENTITLEMENT)
        billing.onUpdate(store.answer())
        assertEquals(Plan.AD_FREE, billing.plan.value)
        val player = Player()
        ads(billing, player).showAfterSave()
        assertEquals(0, player.shown)
    }
}
