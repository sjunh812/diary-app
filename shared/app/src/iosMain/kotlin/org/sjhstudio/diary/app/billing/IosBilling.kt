package org.sjhstudio.diary.app.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitGetProducts
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.ktx.awaitSyncPurchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.StoreBilling
import org.sjhstudio.diary.core.data.StoreClient
import org.sjhstudio.diary.core.data.StoreFailure
import org.sjhstudio.diary.core.model.Plan
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val MANAGE_URL = "https://apps.apple.com/account/subscriptions"
private const val PLAN_KEY = "last_plan"
private const val KEY_INFO = "RevenueCatKey"

/** 요금제와 구매. 화면은 나뉜 두 인터페이스만 봄 */
internal class IosBilling(val plans: PlanRepository, val purchases: PurchaseRepository)

/**
 * 키 없이 만든 빌드는 구매가 없는 무료 앱
 * [scope]는 화면 스레드. 스토어가 알려 온 권한으로 요금제를 바꿈
 */
internal fun iosBilling(scope: CoroutineScope): IosBilling {
    val key = NSBundle.mainBundle.objectForInfoDictionaryKey(KEY_INFO) as? String
    if (key.isNullOrEmpty()) return IosBilling(FreePlanRepository(), NoPurchases())
    val defaults = NSUserDefaults.standardUserDefaults
    val saved = defaults.stringForKey(PLAN_KEY)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
    Purchases.configure(PurchasesConfiguration(apiKey = key))
    val store = StoreBilling(RevenueCatClient, saved, MANAGE_URL) { plan -> defaults.setObject(plan.name, PLAN_KEY) }
    Purchases.sharedInstance.delegate =
        object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                scope.launch { store.onUpdate(customerInfo.active) }
            }

            // 앱스토어 홍보 구매는 쓰지 않음
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
                ) -> Unit
            ) = Unit
        }
    scope.launch { store.refresh() }
    return IosBilling(store, store)
}

private val CustomerInfo.active: Set<String> get() = entitlements.active.keys

/**
 * RevenueCat에 닿는 자리
 * 라이브러리 오류를 [StoreFailure]로 바꾸기만 하고, 판단은 [StoreBilling]이 함
 */
private object RevenueCatClient : StoreClient {
    override suspend fun entitlements(): Set<String> = store { Purchases.sharedInstance.awaitCustomerInfo().active }

    override suspend fun sync(): Set<String> = store { Purchases.sharedInstance.awaitSyncPurchases().active }

    override suspend fun prices(storeIds: List<String>): Map<String, String> = store {
        Purchases.sharedInstance.awaitGetProducts(storeIds).associate { it.id to it.price.formatted }
    }

    override suspend fun purchase(storeId: String): Set<String> = store {
        val product =
            Purchases.sharedInstance.awaitGetProducts(listOf(storeId)).firstOrNull { it.id == storeId }
                ?: throw StoreFailure(StoreFailure.Reason.OTHER)
        Purchases.sharedInstance.awaitPurchase(product).customerInfo.active
    }

    override suspend fun restore(): Set<String> = store { Purchases.sharedInstance.awaitRestore().active }

    private inline fun <T> store(block: () -> T): T = try {
        block()
    } catch (e: PurchasesException) {
        throw storeFailureOf(e)
    }
}

/** 취소는 사용자가 닫은 것, 대기는 승인 기다림(구입 요청), 이미 가짐은 재설치 뒤 다시 산 경우 */
internal fun storeFailureOf(e: PurchasesException): StoreFailure {
    val reason = when {
        e is PurchasesTransactionException && e.userCancelled -> StoreFailure.Reason.CANCELLED
        e.code == PurchasesErrorCode.PurchaseCancelledError -> StoreFailure.Reason.CANCELLED
        e.code == PurchasesErrorCode.PaymentPendingError -> StoreFailure.Reason.PENDING
        e.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> StoreFailure.Reason.ALREADY_OWNED
        else -> StoreFailure.Reason.OTHER
    }
    return StoreFailure(reason, e)
}
