package org.sjhstudio.diary.app.billing

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.StoreBilling
import org.sjhstudio.diary.core.data.StoreClient
import org.sjhstudio.diary.core.data.StoreFailure
import org.sjhstudio.diary.core.model.Plan
import platform.Foundation.NSUserDefaults

private const val MANAGE_URL = "https://apps.apple.com/account/subscriptions"
private const val PLAN_KEY = "last_plan"

/** 스토어가 거절한 까닭. Swift가 RevenueCat 오류를 이것으로 바꿈 */
enum class IosStoreError { CANCELLED, PENDING, ALREADY_OWNED, OTHER }

/**
 * Swift가 구현하는 RevenueCat 다리(PurchasesBridge.swift)
 * 결과는 화면 스레드에서 콜백으로 돌려줌
 * 성공하면 지금 켜진 권한 이름 묶음, 실패하면 까닭
 */
interface IosStoreBridge {
    fun entitlements(done: (active: Set<String>?, error: IosStoreError?) -> Unit)

    /** Apple 계정에 남은 구매를 가져옴. 로그인 창을 띄우지 않음 */
    fun sync(done: (active: Set<String>?, error: IosStoreError?) -> Unit)

    /** 스토어 상품 ID와 그 나라 돈으로 쓴 가격. 스토어에 닿지 못하면 빈 묶음 */
    fun prices(storeIds: List<String>, done: (Map<String, String>) -> Unit)

    fun purchase(storeId: String, done: (active: Set<String>?, error: IosStoreError?) -> Unit)

    fun restore(done: (active: Set<String>?, error: IosStoreError?) -> Unit)

    /** 스토어가 권한이 바뀌었다고 알려 올 때마다 [listener] 호출 */
    fun onUpdate(listener: (active: Set<String>) -> Unit)
}

/** 요금제와 구매. 화면은 나뉜 두 인터페이스만 봄 */
internal class IosBilling(val plans: PlanRepository, val purchases: PurchaseRepository)

/**
 * 키 없이 만든 빌드는 다리가 없어 구매가 없는 무료 앱
 * [scope]는 화면 스레드여야 함. 다리의 콜백과 같은 스레드에서 요금제를 바꿈
 */
internal fun iosBilling(bridge: IosStoreBridge?, scope: CoroutineScope): IosBilling {
    bridge ?: return IosBilling(FreePlanRepository(), NoPurchases())
    val defaults = NSUserDefaults.standardUserDefaults
    val saved = defaults.stringForKey(PLAN_KEY)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
    val store = StoreBilling(IosStoreClient(bridge), saved, MANAGE_URL) { plan -> defaults.setObject(plan.name, PLAN_KEY) }
    bridge.onUpdate { active -> scope.launch { store.onUpdate(active) } }
    scope.launch { store.refresh() }
    return IosBilling(store, store)
}

/**
 * 다리의 콜백을 [StoreClient]로 옮기기만 하고, 판단은 [StoreBilling]이 함
 * 기다리던 쪽이 떠난 뒤에 온 답은 버림. 그때 바뀐 권한은 [IosStoreBridge.onUpdate]로 다시 옴
 */
internal class IosStoreClient(private val bridge: IosStoreBridge) : StoreClient {
    override suspend fun entitlements(): Set<String> = await(bridge::entitlements)

    override suspend fun sync(): Set<String> = await(bridge::sync)

    override suspend fun prices(storeIds: List<String>): Map<String, String> = suspendCancellableCoroutine { result ->
        bridge.prices(storeIds) { prices -> if (result.isActive) result.resume(prices) }
    }

    override suspend fun purchase(storeId: String): Set<String> = await { done -> bridge.purchase(storeId, done) }

    override suspend fun restore(): Set<String> = await(bridge::restore)

    private suspend fun await(call: (done: (Set<String>?, IosStoreError?) -> Unit) -> Unit): Set<String> =
        suspendCancellableCoroutine { result ->
            call { active, error ->
                when {
                    !result.isActive -> Unit
                    active != null -> result.resume(active)
                    else -> result.resumeWithException(StoreFailure(error.reason))
                }
            }
        }
}

private val IosStoreError?.reason: StoreFailure.Reason
    get() = when (this) {
        IosStoreError.CANCELLED -> StoreFailure.Reason.CANCELLED
        IosStoreError.PENDING -> StoreFailure.Reason.PENDING
        IosStoreError.ALREADY_OWNED -> StoreFailure.Reason.ALREADY_OWNED
        IosStoreError.OTHER, null -> StoreFailure.Reason.OTHER
    }
