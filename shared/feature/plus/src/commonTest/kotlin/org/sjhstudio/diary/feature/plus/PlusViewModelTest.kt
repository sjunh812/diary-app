package org.sjhstudio.diary.feature.plus

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

    private class Store(var result: PurchaseResult = PurchaseResult.DONE, var list: List<StoreProduct> = emptyList()) : PurchaseRepository {
        var bought: ProductId? = null
        override val manageUrl = ""

        override suspend fun products() = list

        override suspend fun buy(id: ProductId): PurchaseResult {
            bought = id
            return result
        }

        override suspend fun restore() = result
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
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.FAILED))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.RestoreNone, viewModel.messages.first())
    }
}
