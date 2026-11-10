package org.sjhstudio.diary.app.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.edit
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.StoreBilling
import org.sjhstudio.diary.core.data.StoreClient
import org.sjhstudio.diary.core.data.StoreFailure
import org.sjhstudio.diary.core.data.StoreIds
import org.sjhstudio.diary.core.model.Plan

private const val MANAGE_URL = "https://play.google.com/store/account/subscriptions"
private const val PREFS = "plan"
private const val PLAN_KEY = "last_plan"

/** 요금제와 구매. 화면은 나뉜 두 인터페이스만 봄 */
class Billing(val plans: PlanRepository, val purchases: PurchaseRepository)

/** 키 없이 만든 빌드는 구매가 없는 무료 앱 */
fun billing(context: Context): Billing {
    val app = context.applicationContext as Application
    val key =
        app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("org.sjhstudio.diary.revenuecat_key").orEmpty()
    if (key.isEmpty()) return Billing(FreePlanRepository(), NoPurchases())
    val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(PLAN_KEY, null)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
    val client = RevenueCatClient(app, key)
    val store = StoreBilling(client, saved, MANAGE_URL) { plan -> prefs.edit { putString(PLAN_KEY, plan.name) } }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    Purchases.sharedInstance.updatedCustomerInfoListener =
        UpdatedCustomerInfoListener { info -> scope.launch { store.onUpdate(info.active) } }
    scope.launch { store.refresh() }
    return Billing(store, store)
}

private val CustomerInfo.active: Set<String> get() = entitlements.active.keys

/**
 * RevenueCat에 닿는 자리
 * 라이브러리 오류를 [StoreFailure]로 바꾸기만 하고, 판단은 [StoreBilling]이 함
 */
private class RevenueCatClient(private val app: Application, key: String) : StoreClient {
    private var activity: Activity? = null

    init {
        Purchases.configure(PurchasesConfiguration.Builder(app, key).build())
        watchActivity()
    }

    override suspend fun entitlements(): Set<String> = store { Purchases.sharedInstance.awaitCustomerInfo().active }

    override suspend fun sync(): Set<String> = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.syncPurchases(
            object : SyncPurchasesCallback {
                override fun onSuccess(customerInfo: CustomerInfo) = cont.resume(customerInfo.active)

                override fun onError(error: PurchasesError) = cont.resumeWithException(StoreFailure(StoreFailure.Reason.OTHER))
            }
        )
    }

    override suspend fun prices(storeIds: List<String>): Map<String, String> = store {
        Purchases.sharedInstance.awaitGetProducts(storeIds).associate { StoreIds.baseOf(it.id) to it.price.formatted }
    }

    override suspend fun purchase(storeId: String): Set<String> = store {
        val screen = activity ?: throw StoreFailure(StoreFailure.Reason.OTHER)
        // 구독은 기본 요금제마다 상품이 따로 올 수 있어 ID로 정확히 고름
        val product =
            Purchases.sharedInstance.awaitGetProducts(listOf(storeId)).firstOrNull { StoreIds.baseOf(it.id) == storeId }
                ?: throw StoreFailure(StoreFailure.Reason.OTHER)
        Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(screen, product).build()).customerInfo.active
    }

    override suspend fun restore(): Set<String> = store { Purchases.sharedInstance.awaitRestore().active }

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

    private fun watchActivity() {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    this@RevenueCatClient.activity = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (this@RevenueCatClient.activity === activity) this@RevenueCatClient.activity = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }
}
