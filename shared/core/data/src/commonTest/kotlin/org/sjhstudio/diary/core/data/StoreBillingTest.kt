package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

// 결제 뒤에 켜져야 할 것이 켜지는지, 실패·취소·대기·복원에서 돈과 요금제가 맞는지 봄
// 스토어는 가짜로 두고 우리 판단만 시험
class StoreBillingTest {
    private val stored = ArrayList<Plan>()

    private class FakeStore : StoreClient {
        var owned: Set<String> = emptySet()
        var accountOwned: Set<String> = emptySet()
        var failNext: StoreFailure.Reason? = null
        var offline = false
        var syncs = 0
        val bought = ArrayList<String>()

        private fun check() {
            if (offline) throw StoreFailure(StoreFailure.Reason.OTHER)
        }

        override suspend fun entitlements(): Set<String> = owned.also { check() }

        override suspend fun sync(): Set<String> {
            check()
            syncs++
            owned = owned + accountOwned
            return owned
        }

        override suspend fun prices(storeIds: List<String>): Map<String, Price> {
            check()
            return mapOf(StoreIds.AD_FREE_PRODUCT to won("3,300", 3_300), StoreIds.PLUS_PRODUCT to won("12,000", 12_000)).filterKeys {
                it in
                    storeIds
            }
        }

        override suspend fun purchase(storeId: String): Set<String> {
            check()
            failNext?.let {
                failNext = null
                throw StoreFailure(it)
            }
            bought += storeId
            owned = owned + if (storeId == StoreIds.PLUS_PRODUCT) StoreIds.PLUS_ENTITLEMENT else StoreIds.AD_FREE_ENTITLEMENT
            return owned
        }

        override suspend fun restore(): Set<String> {
            failNext?.let {
                failNext = null
                throw StoreFailure(it)
            }
            return sync()
        }
    }

    private val store = FakeStore()

    private fun billing(saved: Plan? = Plan.FREE) = StoreBilling(store, saved, "manage") { stored += it }

    @Test
    fun buying_ad_free_turns_it_on_and_remembers_it() = runTest {
        val billing = billing()
        assertEquals(PurchaseResult.DONE, billing.buy(ProductId.AD_FREE))
        assertEquals(Plan.AD_FREE, billing.plan.value)
        assertEquals(listOf(StoreIds.AD_FREE_PRODUCT), store.bought)
        assertEquals(Plan.AD_FREE, stored.last())
    }

    @Test
    fun buying_plus_asks_the_store_for_the_plus_product() = runTest {
        val billing = billing()
        billing.buy(ProductId.PLUS_YEARLY)
        assertEquals(listOf(StoreIds.PLUS_PRODUCT), store.bought)
        assertEquals(Plan.PLUS, billing.plan.value)
    }

    @Test
    fun cancelling_changes_nothing() = runTest {
        val billing = billing()
        store.failNext = StoreFailure.Reason.CANCELLED
        assertEquals(PurchaseResult.CANCELLED, billing.buy(ProductId.AD_FREE))
        assertEquals(Plan.FREE, billing.plan.value)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun a_pending_payment_does_not_turn_anything_on_yet() = runTest {
        val billing = billing()
        store.failNext = StoreFailure.Reason.PENDING
        assertEquals(PurchaseResult.PENDING, billing.buy(ProductId.PLUS_YEARLY))
        assertEquals(Plan.FREE, billing.plan.value)
    }

    @Test
    fun a_failed_payment_says_failed_and_keeps_the_plan() = runTest {
        val billing = billing(Plan.AD_FREE)
        store.failNext = StoreFailure.Reason.OTHER
        assertEquals(PurchaseResult.FAILED, billing.buy(ProductId.PLUS_YEARLY))
        assertEquals(Plan.AD_FREE, billing.plan.value)
    }

    @Test
    fun buying_again_after_reinstall_restores_instead_of_charging_twice() = runTest {
        val billing = billing(null)
        store.accountOwned = setOf(StoreIds.AD_FREE_ENTITLEMENT)
        store.failNext = StoreFailure.Reason.ALREADY_OWNED
        assertEquals(PurchaseResult.DONE, billing.buy(ProductId.AD_FREE))
        assertTrue(store.bought.isEmpty())
        assertEquals(Plan.AD_FREE, billing.plan.value)
    }

    @Test
    fun already_owned_but_nothing_to_restore_is_a_failure() = runTest {
        val billing = billing()
        store.failNext = StoreFailure.Reason.ALREADY_OWNED
        assertEquals(PurchaseResult.FAILED, billing.buy(ProductId.AD_FREE))
        assertEquals(Plan.FREE, billing.plan.value)
    }

    @Test
    fun restore_with_nothing_bought_says_nothing() = runTest {
        val billing = billing()
        assertEquals(PurchaseResult.NOTHING, billing.restore())
        assertEquals(Plan.FREE, billing.plan.value)
    }

    @Test
    fun restore_brings_back_plus_bought_on_another_phone() = runTest {
        val billing = billing()
        store.accountOwned = setOf(StoreIds.PLUS_ENTITLEMENT)
        assertEquals(PurchaseResult.DONE, billing.restore())
        assertEquals(Plan.PLUS, billing.plan.value)
    }

    /** iOS 복원은 Apple 계정 로그인 창을 띄울 수 있음. 닫으면 실패가 아니라 취소 */
    @Test
    fun closing_the_sign_in_during_restore_is_a_cancel() = runTest {
        val billing = billing(Plan.AD_FREE)
        store.failNext = StoreFailure.Reason.CANCELLED
        assertEquals(PurchaseResult.CANCELLED, billing.restore())
        assertEquals(Plan.AD_FREE, billing.plan.value)
    }

    @Test
    fun restore_offline_fails_and_keeps_the_plan() = runTest {
        val billing = billing(Plan.PLUS)
        store.offline = true
        assertEquals(PurchaseResult.FAILED, billing.restore())
        assertEquals(Plan.PLUS, billing.plan.value)
    }

    @Test
    fun a_reinstalled_buyer_is_unknown_until_the_sync_then_gets_plus() = runTest {
        val billing = billing(null)
        assertFalse(billing.known.value)
        store.accountOwned = setOf(StoreIds.PLUS_ENTITLEMENT)
        billing.refresh()
        assertEquals(1, store.syncs)
        assertTrue(billing.known.value)
        assertEquals(Plan.PLUS, billing.plan.value)
    }

    @Test
    fun an_offline_launch_keeps_the_saved_plan() = runTest {
        val billing = billing(Plan.AD_FREE)
        store.offline = true
        billing.refresh()
        assertTrue(billing.known.value)
        assertEquals(Plan.AD_FREE, billing.plan.value)
    }

    @Test
    fun a_known_user_does_not_sync_on_every_launch() = runTest {
        val billing = billing(Plan.FREE)
        billing.refresh()
        assertEquals(0, store.syncs)
    }

    @Test
    fun a_refund_turns_the_plan_back_to_free() = runTest {
        val billing = billing(Plan.PLUS)
        billing.onUpdate(emptySet())
        assertEquals(Plan.FREE, billing.plan.value)
        assertEquals(Plan.FREE, stored.last())
    }

    @Test
    fun products_keep_store_prices_and_offline_shows_none() = runTest {
        val billing = billing()
        assertEquals(
            listOf(StoreProduct(ProductId.AD_FREE, won("3,300", 3_300)), StoreProduct(ProductId.PLUS_YEARLY, won("12,000", 12_000))),
            billing.products()
        )
        store.offline = true
        assertTrue(billing.products().isEmpty())
    }

    @Test
    fun subscription_ids_lose_their_base_plan_suffix() {
        assertEquals("plus_yearly", StoreIds.baseOf("plus_yearly:yearly"))
        assertEquals("ad_free", StoreIds.baseOf("ad_free"))
    }
}

/** 스토어가 주는 원화 가격 */
private fun won(text: String, value: Long) = Price("₩$text", value * 1_000_000, "KRW")
