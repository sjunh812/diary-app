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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
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

        var loads = 0
        val loadGate = CompletableDeferred<Unit>()
        var waitInLoad = false

        override suspend fun products(): List<StoreProduct> {
            loads++
            if (waitInLoad) loadGate.await()
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

    /** 결제가 끝났다는 이벤트를 모음 */
    private fun TestScope.purchases(viewModel: PlusViewModel) = mutableListOf<ProductId>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.purchased.collect {
                list +=
                    it
            }
        }
    }

    @Test
    fun no_store_shows_no_price() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store())
        advanceUntilIdle()

        assertNull(viewModel.state.value.price(ProductId.PLUS_YEARLY))
        assertEquals(false, viewModel.state.value.loading)
    }

    @Test
    fun price_is_what_the_store_gave() = runTest(dispatcher) {
        val store = Store(list = listOf(StoreProduct(ProductId.PLUS_YEARLY, won("14,900", 14_900))))
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        assertEquals(won("14,900", 14_900), viewModel.state.value.price(ProductId.PLUS_YEARLY))
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
        val bought = purchases(viewModel)
        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()
        assertEquals(emptyList(), bought)
    }

    @Test
    fun a_payment_waiting_for_approval_is_not_called_a_failure() = runTest(dispatcher) {
        val store = Store(PurchaseResult.PENDING)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        val bought = purchases(viewModel)
        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()

        assertEquals(PlusMessage.Pending, viewModel.messages.first())
        assertEquals(false, viewModel.state.value.buyFailed)
        assertEquals(false, viewModel.state.value.busy)
        // 승인되기 전에는 산 것처럼 화면을 닫지 않음
        assertEquals(emptyList(), bought)
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

    /** 구매를 취소할 때처럼 아무 말 없이 그대로 돌아옴 */
    @Test
    fun closing_the_sign_in_during_restore_says_nothing() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.CANCELLED))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertNull(withTimeoutOrNull(1) { viewModel.messages.first() })
        assertNull(viewModel.state.value.restoreNotice)
        assertEquals(false, viewModel.state.value.busy)
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
        store.list = listOf(StoreProduct(ProductId.PLUS_YEARLY, won("14,900", 14_900)))
        viewModel.load()
        advanceUntilIdle()

        assertEquals(won("14,900", 14_900), viewModel.state.value.price(ProductId.PLUS_YEARLY))
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

        val bought = purchases(viewModel)
        viewModel.buy(ProductId.PLUS_YEARLY)
        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()

        assertEquals(0, store.buys)
        assertEquals(listOf(ProductId.PLUS_YEARLY, ProductId.AD_FREE), bought)
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

    /** 광고 제거 시트는 토스트가 없는 화면(홈, 돌아보기)에서도 열림. 실패를 상태로 들고 있어야 시트가 보임 */
    @Test
    fun a_failed_purchase_stays_visible_until_the_next_try() = runTest(dispatcher) {
        val store = Store(PurchaseResult.FAILED)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.buyFailed)

        store.result = PurchaseResult.CANCELLED
        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.buyFailed, "취소는 실패가 아니다")
    }

    @Test
    fun a_store_crash_counts_as_a_failed_purchase() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(crash = true))
        advanceUntilIdle()

        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.buyFailed)
    }

    @Test
    fun closing_the_sheet_forgets_the_failure() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, Store(PurchaseResult.FAILED))
        advanceUntilIdle()
        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()

        viewModel.failureShown()

        assertEquals(false, viewModel.state.value.buyFailed)
    }

    /** 스토어가 구매나 복원을 알리면 화면은 요금제만 보고 바뀜(FR-001) */
    @Test
    fun a_purchase_reaches_the_screen_through_the_plan() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, FlippingStore(plans))
        advanceUntilIdle()

        val bought = purchases(viewModel)
        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()

        assertEquals(listOf(ProductId.PLUS_YEARLY), bought)
        assertEquals(Plan.PLUS, viewModel.plan.value)
    }

    @Test
    fun a_restored_purchase_reaches_the_screen_through_the_plan() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, FlippingStore(plans, restored = Plan.AD_FREE))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(PlusMessage.Restored, viewModel.messages.first())
        assertEquals(Plan.AD_FREE, viewModel.plan.value)
    }

    /** 구독이 끝나면 다시 살 수 있음. 앞서 샀다고 스토어에 보내지 않으면 안 됨 */
    @Test
    fun plus_can_be_bought_again_after_it_ends() = runTest(dispatcher) {
        val store = Store()
        val viewModel = PlusViewModel(plans, store)
        plans.plan.value = Plan.PLUS
        advanceUntilIdle()

        plans.plan.value = Plan.FREE
        advanceUntilIdle()
        viewModel.buy(ProductId.PLUS_YEARLY)
        advanceUntilIdle()

        assertEquals(1, store.buys)
    }

    /** 시트가 열 때마다 부름. 처음 오프라인이었어도 다시 열면 가격이 옴 */
    @Test
    fun opening_again_fetches_the_price_that_was_missing() = runTest(dispatcher) {
        val store = Store(crash = true)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()
        assertNull(viewModel.state.value.price(ProductId.AD_FREE))

        store.crash = false
        store.list = listOf(StoreProduct(ProductId.AD_FREE, won("9,900", 9_900)))
        viewModel.load()
        advanceUntilIdle()

        assertEquals(won("9,900", 9_900), viewModel.state.value.price(ProductId.AD_FREE))
    }

    /** 받아 둔 가격이 있으면 다시 받기에 실패해도 사라지지 않음 */
    @Test
    fun a_failed_refresh_keeps_the_known_price() = runTest(dispatcher) {
        val store = Store(list = listOf(StoreProduct(ProductId.AD_FREE, won("9,900", 9_900))))
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        store.crash = true
        viewModel.load()
        advanceUntilIdle()

        assertEquals(won("9,900", 9_900), viewModel.state.value.price(ProductId.AD_FREE))
        assertEquals(false, viewModel.state.value.loading)
    }

    /** 뷰모델이 생기며 받는 중에 시트가 열려도 스토어를 두 번 부르지 않음 */
    @Test
    fun opening_while_loading_does_not_ask_the_store_twice() = runTest(dispatcher) {
        val store = Store().apply { waitInLoad = true }
        val viewModel = PlusViewModel(plans, store)
        runCurrent()

        viewModel.load()
        store.loadGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, store.loads)
    }

    /** 광고 제거 시트에는 토스트가 없어서 복원 결과를 상태로 들고 있어야 보임 */
    @Test
    fun the_sheet_sees_what_the_restore_found() = runTest(dispatcher) {
        val store = Store(PurchaseResult.NOTHING)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()
        assertEquals(PlusMessage.RestoreNone, viewModel.state.value.restoreNotice)

        store.result = PurchaseResult.FAILED
        viewModel.restore()
        advanceUntilIdle()
        assertEquals(PlusMessage.RestoreFailed, viewModel.state.value.restoreNotice)

        viewModel.failureShown()
        assertNull(viewModel.state.value.restoreNotice)
    }

    @Test
    fun a_finished_restore_leaves_no_notice() = runTest(dispatcher) {
        val viewModel = PlusViewModel(plans, FlippingStore(plans, restored = Plan.AD_FREE))
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertNull(viewModel.state.value.restoreNotice)
    }

    /** 결제 실패 안내를 보고 구매 복원을 누르면 지난 실패 문구는 사라짐 */
    @Test
    fun restoring_clears_the_payment_failure() = runTest(dispatcher) {
        val store = Store(PurchaseResult.FAILED)
        val viewModel = PlusViewModel(plans, store)
        advanceUntilIdle()
        viewModel.buy(ProductId.AD_FREE)
        advanceUntilIdle()

        store.result = PurchaseResult.NOTHING
        viewModel.restore()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.buyFailed)
    }

    /** 표: 요금제마다 스토어로 가는 구매 */
    @Test
    fun what_each_plan_can_still_buy() = runTest(dispatcher) {
        val table =
            mapOf(
                Plan.FREE to setOf(ProductId.AD_FREE, ProductId.PLUS_YEARLY),
                Plan.AD_FREE to setOf(ProductId.PLUS_YEARLY),
                Plan.PLUS to emptySet()
            )
        for ((plan, buyable) in table) {
            for (product in ProductId.entries) {
                val store = Store()
                plans.plan.value = plan
                val viewModel = PlusViewModel(plans, store)
                advanceUntilIdle()

                viewModel.buy(product)
                advanceUntilIdle()

                assertEquals(if (product in buyable) 1 else 0, store.buys, "$plan $product")
            }
        }
    }

    private class FlippingStore(private val plans: PlanRepository, private val restored: Plan = Plan.PLUS) : PurchaseRepository {
        override val manageUrl = ""

        override suspend fun products(): List<StoreProduct> = emptyList()

        override suspend fun buy(id: ProductId): PurchaseResult {
            (plans.plan as MutableStateFlow<Plan>).value = if (id == ProductId.PLUS_YEARLY) Plan.PLUS else Plan.AD_FREE
            return PurchaseResult.DONE
        }

        override suspend fun restore(): PurchaseResult {
            (plans.plan as MutableStateFlow<Plan>).value = restored
            return PurchaseResult.DONE
        }
    }
}

/** 스토어가 주는 원화 가격 */
private fun won(text: String, value: Long) = Price("₩$text", value * 1_000_000, "KRW")
