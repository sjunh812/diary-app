package org.sjhstudio.diary.feature.plus

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

@OptIn(ExperimentalCoroutinesApi::class)
class PlusViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val plans = object : PlanRepository {
        override val plan = MutableStateFlow(Plan.FREE)
    }

    private class Store(
        var result: PurchaseResult = PurchaseResult.DONE,
        var list: List<StoreProduct> = emptyList(),
        var crash: Boolean = false
    ) : PurchaseRepository {
        val gate = CompletableDeferred<Unit>()
        var bought: ProductId? = null
        var buys = 0
        var restores = 0
        override val manageUrl = ""

        override suspend fun products(): List<StoreProduct> {
            if (crash) error("store")
            return list
        }

        override suspend fun buy(id: ProductId): PurchaseResult {
            buys++
            if (crash) error("store")
            bought = id
            if (waitInBuy) gate.await()
            return result
        }

        var waitInBuy = false

        override suspend fun restore(): PurchaseResult {
            restores++
            if (crash) error("store")
            return result
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun no_store_shows_no_price() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store())
        advanceUntilIdle()

        assertNull(viewModel.state.value.price(ProductId.PLUS_YEARLY))
        assertEquals(false, viewModel.state.value.loading)
    }

    @Test
    fun price_is_what_the_store_gave() = runTest(dispatcher) {
        val store = Store(list = listOf(StoreProduct(ProductId.PLUS_YEARLY, "₩14,900")))
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        assertEquals("₩14,900", viewModel.state.value.price(ProductId.PLUS_YEARLY))
    }

    @Test
    fun failed_payment_says_so_and_cancel_says_nothing() = runTest(dispatcher) {
        val store = Store(PurchaseResult.FAILED)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()
        assertEquals(PlusMessage.Failed, viewModel.messages.first())
        assertEquals(false, viewModel.state.value.busy)

        store.result = PurchaseResult.CANCELLED
        var done = false
        viewModel.buy(ProductId.PLUS_YEARLY) { done = true }
        advanceUntilIdle()
        assertEquals(false, done)
    }

    @Test
    fun restore_tells_when_nothing_was_found() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.NOTHING))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.RestoreNone, viewModel.messages.first())
    }

    @Test
    fun restore_failure_does_not_claim_there_is_nothing() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.FAILED))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.RestoreFailed, viewModel.messages.first())
    }

    @Test
    fun finished_restore_says_so() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.DONE))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.Restored, viewModel.messages.first())
    }

    @Test
    fun store_crash_while_buying_fails_softly_and_frees_the_button() = runTest(dispatcher) {
        val store = Store(crash = true)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()

        assertEquals(PlusMessage.Failed, viewModel.messages.first())
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun store_crash_while_restoring_says_it_failed() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(crash = true))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.RestoreFailed, viewModel.messages.first())
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun store_crash_while_loading_leaves_a_retry() = runTest(dispatcher) {
        val store = Store(crash = true)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.loading)
        assertNull(viewModel.state.value.price(ProductId.PLUS_YEARLY))

        store.crash = false
        store.list = listOf(StoreProduct(ProductId.PLUS_YEARLY, "₩14,900"))
        viewModel.load()
        advanceUntilIdle()

        assertEquals("₩14,900", viewModel.state.value.price(ProductId.PLUS_YEARLY))
    }

    @Test
    fun double_tap_buys_once() = runTest(dispatcher) {
        val store = Store().apply { waitInBuy = true }
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        viewModel.buy(ProductId.PLUS_YEARLY)
        runCurrent()
        viewModel.buy(ProductId.PLUS_YEARLY)
        viewModel.restore()
        store.gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, store.buys)
        assertEquals(0, store.restores)
    }

    @Test
    fun owned_products_never_reach_the_store() = runTest(dispatcher) {
        val store = Store()
        val viewModel = PlusViewModel(plans, store)
        plans.plan.value = Plan.PLUS
        advanceUntilIdle()

        var done = 0
        viewModel.buy(ProductId.PLUS_YEARLY) { done++ }
        viewModel.buy(ProductId.AD_FREE) { done++ }
        advanceUntilIdle()

        assertEquals(0, store.buys)
        assertEquals(2, done)
    }

    @Test
    fun ad_free_owner_can_still_buy_plus_but_not_ad_free_again() = runTest(dispatcher) {
        val store = Store()
        val viewModel = PlusViewModel(plans, store)
        plans.plan.value = Plan.AD_FREE
        advanceUntilIdle()

        viewModel.buy(ProductId.AD_FREE)
        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()

        assertEquals(1, store.buys)
        assertEquals(ProductId.PLUS_YEARLY, store.bought)
    }
}
