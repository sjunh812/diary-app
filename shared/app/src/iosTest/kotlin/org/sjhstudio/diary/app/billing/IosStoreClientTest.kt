package org.sjhstudio.diary.app.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.data.StoreBilling
import org.sjhstudio.diary.core.data.StoreFailure
import org.sjhstudio.diary.core.data.StoreIds
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult

private typealias Answer = (Set<String>?, IosStoreError?) -> Unit

// Swift 다리의 답이 결제 판단(StoreBilling)까지 맞게 건너가는지 봄
// 다리는 가짜로 두고, 답을 늦게 주거나 실패로 줄 수 있게 함
@OptIn(ExperimentalCoroutinesApi::class)
class IosStoreClientTest {
    private class FakeBridge : IosStoreBridge {
        var owned: Set<String> = emptySet()
        var accountOwned: Set<String> = emptySet()
        var failure: IosStoreError? = null
        var hold = false
        var listener: ((Set<String>) -> Unit)? = null
        val held = ArrayList<Answer>()
        val bought = ArrayList<String>()

        private fun answer(done: Answer) {
            when {
                hold -> held += done
                failure != null -> done(null, failure)
                else -> done(owned, null)
            }
        }

        override fun entitlements(done: Answer) = answer(done)

        override fun sync(done: Answer) {
            if (failure == null) owned = owned + accountOwned
            answer(done)
        }

        override fun prices(storeIds: List<String>, done: (Map<String, String>) -> Unit) =
            done(if (failure == null) storeIds.associateWith { "₩9,900" } else emptyMap())

        override fun purchase(storeId: String, done: Answer) {
            if (failure == null) {
                bought += storeId
                owned = owned + if (storeId == StoreIds.PLUS_PRODUCT) StoreIds.PLUS_ENTITLEMENT else StoreIds.AD_FREE_ENTITLEMENT
            }
            answer(done)
        }

        override fun restore(done: Answer) = sync(done)

        override fun onUpdate(listener: (Set<String>) -> Unit) {
            this.listener = listener
        }
    }

    private val bridge = FakeBridge()
    private val stored = ArrayList<Plan>()

    private fun billing(saved: Plan? = Plan.FREE) = StoreBilling(IosStoreClient(bridge), saved, "manage") { stored += it }

    @Test
    fun buying_changes_the_plan_and_remembers_it() = runTest {
        val billing = billing()

        assertEquals(PurchaseResult.DONE, billing.buy(ProductId.AD_FREE))

        assertEquals(Plan.AD_FREE, billing.plan.value)
        assertEquals(listOf(StoreIds.AD_FREE_PRODUCT), bridge.bought)
        assertEquals(Plan.AD_FREE, stored.last())
    }

    @Test
    fun bridge_failure_reasons_become_purchase_results() = runTest {
        val billing = billing()
        val expected =
            mapOf(
                IosStoreError.CANCELLED to PurchaseResult.CANCELLED,
                IosStoreError.PENDING to PurchaseResult.PENDING,
                IosStoreError.OTHER to PurchaseResult.FAILED
            )

        expected.forEach { (error, result) ->
            bridge.failure = error
            assertEquals(result, billing.buy(ProductId.PLUS_YEARLY))
        }

        assertEquals(Plan.FREE, billing.plan.value)
        assertTrue(bridge.bought.isEmpty())
    }

    @Test
    fun a_failure_without_a_reason_is_still_a_failure() = runTest {
        val client = IosStoreClient(
            object : IosStoreBridge by bridge {
                override fun entitlements(done: Answer) = done(null, null)
            }
        )

        val failure = assertFailsWith<StoreFailure> { client.entitlements() }

        assertEquals(StoreFailure.Reason.OTHER, failure.reason)
    }

    @Test
    fun first_launch_after_reinstall_brings_back_account_purchases() = runTest {
        bridge.accountOwned = setOf(StoreIds.PLUS_ENTITLEMENT)
        val billing = billing(saved = null)

        billing.refresh()

        assertEquals(Plan.PLUS, billing.plan.value)
        assertTrue(billing.known.value)
    }

    @Test
    fun unreachable_store_gives_no_prices_and_a_failed_restore() = runTest {
        val billing = billing()
        bridge.failure = IosStoreError.OTHER

        assertTrue(billing.products().isEmpty())
        assertEquals(PurchaseResult.FAILED, billing.restore())
    }

    @Test
    fun prices_arrive_for_each_product() = runTest {
        val products = billing().products()

        assertEquals(ProductId.entries.toSet(), products.map { it.id }.toSet())
        assertTrue(products.all { it.price == "₩9,900" })
    }

    @Test
    fun restoring_with_nothing_owned_says_nothing() = runTest {
        assertEquals(PurchaseResult.NOTHING, billing().restore())
    }

    @Test
    fun a_late_answer_after_leaving_is_dropped_safely() = runTest {
        val client = IosStoreClient(bridge)
        bridge.hold = true
        val waiting = async { client.purchase(StoreIds.AD_FREE_PRODUCT) }
        runCurrent()

        waiting.cancelAndJoin()
        bridge.held.single()(setOf(StoreIds.AD_FREE_ENTITLEMENT), null)

        assertTrue(waiting.isCancelled)
    }

    @Test
    fun store_updates_change_the_plan() = runTest {
        val billing = billing()
        bridge.onUpdate { active -> backgroundScope.async { billing.onUpdate(active) } }

        bridge.listener?.invoke(setOf(StoreIds.PLUS_ENTITLEMENT))
        runCurrent()

        assertEquals(Plan.PLUS, billing.plan.value)
    }
}
