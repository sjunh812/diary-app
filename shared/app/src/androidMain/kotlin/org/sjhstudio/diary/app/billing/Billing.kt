package org.sjhstudio.diary.app.billing

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetProducts
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.app.di.appCoroutineScope
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.StoreAnswer
import org.sjhstudio.diary.core.data.StoreBilling
import org.sjhstudio.diary.core.data.StoreClient
import org.sjhstudio.diary.core.data.StoreFailure
import org.sjhstudio.diary.core.data.StoreIds
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.core.platform.AppForeground

private const val MANAGE_URL = "https://play.google.com/store/account/subscriptions"
private const val PREFS = "plan"
private const val PLAN_KEY = "last_plan"

/** 요금제와 구매. 화면은 나뉜 두 인터페이스만 봄 */
class Billing(val plans: PlanRepository, val purchases: PurchaseRepository)

/** 키 없이 만든 빌드는 구매가 없는 무료 앱 */
fun billing(context: Context, current: CurrentActivity): Billing {
    val app = context.applicationContext as Application
    val key =
        app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("org.sjhstudio.diary.revenuecat_key").orEmpty()
    if (key.isEmpty()) return Billing(FreePlanRepository(), NoPurchases())
    val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(PLAN_KEY, null)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
    val client = RevenueCatClient(app, key, current)
    // 결제 라이브러리가 뜻밖의 예외를 던져도 앱이 꺼지지 않게 처리기가 있는 범위를 씀
    val scope = appCoroutineScope(Dispatchers.Main.immediate)
    val store = StoreBilling(client, saved, MANAGE_URL, scope) { plan -> prefs.edit { putString(PLAN_KEY, plan.name) } }
    Purchases.sharedInstance.updatedCustomerInfoListener =
        UpdatedCustomerInfoListener { info -> scope.launch { store.onUpdate(info.answer) } }
    // 켤 때와 다시 앞으로 올 때마다 서버 답을 새로 받음. 환불·만료가 구매 복원 없이 반영됨
    // 이미 앞에 있으면 바로 한 번 불리고, 관찰자는 화면 스레드에서 붙여야 해 scope 안에서 붙임
    scope.launch { AppForeground.observe(onBackground = {}, onForeground = { scope.launch { store.refresh() } }) }
    return Billing(store, store)
}

private val CustomerInfo.answer: StoreAnswer
    get() =
        StoreAnswer(
            active = entitlements.active.keys,
            expiresAt = entitlements.active.values.mapNotNull { it.expirationDate?.time }.minOrNull()?.let(Instant::fromEpochMilliseconds)
        )

/**
 * RevenueCat에 닿는 자리
 * 라이브러리 오류를 [StoreFailure]로 바꾸기만 하고, 판단은 [StoreBilling]이 함
 */
private class RevenueCatClient(app: Application, key: String, private val current: CurrentActivity) : StoreClient {
    init {
        Purchases.configure(PurchasesConfiguration.Builder(app, key).build())
    }

    override suspend fun entitlements(): StoreAnswer = store {
        Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT).answer
    }

    override suspend fun sync(): StoreAnswer = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.syncPurchases(
            object : SyncPurchasesCallback {
                override fun onSuccess(customerInfo: CustomerInfo) = cont.resume(customerInfo.answer)

                override fun onError(error: PurchasesError) = cont.resumeWithException(StoreFailure(StoreFailure.Reason.OTHER))
            }
        )
    }

    override suspend fun prices(storeIds: List<String>): Map<String, Price> = store {
        Purchases.sharedInstance.awaitGetProducts(storeIds).associate { product ->
            StoreIds.baseOf(product.id) to product.price.let { Price(it.formatted, it.amountMicros, it.currencyCode) }
        }
    }

    override suspend fun purchase(storeId: String): StoreAnswer = store {
        val screen = current.value ?: throw StoreFailure(StoreFailure.Reason.OTHER)
        // 구독은 기본 요금제마다 상품이 따로 올 수 있어 ID로 정확히 고름
        val product =
            Purchases.sharedInstance.awaitGetProducts(listOf(storeId)).firstOrNull { StoreIds.baseOf(it.id) == storeId }
                ?: throw StoreFailure(StoreFailure.Reason.OTHER)
        Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(screen, product).build()).customerInfo.answer
    }

    override suspend fun restore(): StoreAnswer = store { Purchases.sharedInstance.awaitRestore().answer }

    private inline fun <T> store(block: () -> T): T = try {
        block()
    } catch (e: PurchasesTransactionException) {
        val reason = when {
            e.userCancelled -> StoreFailure.Reason.CANCELLED
            e.code == PurchasesErrorCode.PaymentPendingError -> StoreFailure.Reason.PENDING
            e.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> StoreFailure.Reason.ALREADY_OWNED
            else -> StoreFailure.Reason.OTHER
        }
        throw StoreFailure(reason, e)
    } catch (e: PurchasesException) {
        throw StoreFailure(StoreFailure.Reason.OTHER, e)
    }
}
