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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.StoreIds
import org.sjhstudio.diary.core.data.StorePlanState
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

private const val MANAGE_URL = "https://play.google.com/store/account/subscriptions"
private const val PREFS = "plan"
private const val PLAN_KEY = "last_plan"

/** 요금제와 구매를 한곳에서 맡는다. 화면은 나뉜 두 인터페이스만 본다. */
interface Billing :
    PlanRepository,
    PurchaseRepository

private class NoBilling(plans: PlanRepository, purchases: PurchaseRepository) :
    Billing,
    PlanRepository by plans,
    PurchaseRepository by purchases

/** 키 없이 만든 빌드는 구매가 없는 무료 앱이다. */
fun billing(context: Context): Billing {
    val app = context.applicationContext as Application
    val key =
        app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("org.sjhstudio.diary.revenuecat_key").orEmpty()
    return if (key.isEmpty()) NoBilling(FreePlanRepository(), NoPurchases()) else RevenueCatBilling(app, key)
}

/**
 * RevenueCat 구현.
 * 마지막 요금제를 기기에 남겨서 산 사람이 앱을 켤 때나 오프라인에서도 광고를 보지 않는다.
 * 재설치로 남긴 값이 없으면 스토어 답을 들을 때까지 요금제를 모른다고 말한다.
 */
private class RevenueCatBilling(private val app: Application, key: String) : Billing {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val saved = prefs.getString(PLAN_KEY, null)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
    private val state = StorePlanState(saved) { plan -> prefs.edit { putString(PLAN_KEY, plan.name) } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activity: Activity? = null

    override val plan: StateFlow<Plan> = state.plan
    override val known: StateFlow<Boolean> = state.known
    override val manageUrl: String = MANAGE_URL

    init {
        Purchases.configure(PurchasesConfiguration.Builder(app, key).build())
        watchActivity()
        Purchases.sharedInstance.updatedCustomerInfoListener =
            com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener { apply(it) }
        scope.launch { refresh() }
    }

    /** 처음 켠 기기는 구글 계정의 구매를 조용히 가져온다. 재설치한 사람이 다시 사지 않게 한다. */
    private suspend fun refresh() {
        try {
            apply(Purchases.sharedInstance.awaitCustomerInfo())
        } catch (_: PurchasesException) {
            // 오프라인이면 남긴 값을 쓴다.
            state.onUnreachable()
        }
    }

    private fun sync() {
        Purchases.sharedInstance.syncPurchases(
            object : SyncPurchasesCallback {
                override fun onSuccess(customerInfo: CustomerInfo) = state.onSynced(planOf(customerInfo))

                override fun onError(error: PurchasesError) = state.onUnreachable()
            }
        )
    }

    private fun planOf(info: CustomerInfo) = StoreIds.planOf(info.entitlements.active.keys)

    private fun apply(info: CustomerInfo) {
        if (state.onAnswer(planOf(info))) sync()
    }

    override suspend fun products(): List<StoreProduct> = try {
        val ids = ProductId.entries.associateBy(StoreIds::productOf)
        Purchases.sharedInstance.awaitGetProducts(ids.keys.toList()).mapNotNull { product ->
            ids[product.id.substringBefore(':')]?.let { StoreProduct(it, product.price.formatted) }
        }
    } catch (_: PurchasesException) {
        emptyList()
    }

    override suspend fun buy(id: ProductId): PurchaseResult = try {
        val screen = checkNotNull(activity) { "화면이 없다" }
        val product = Purchases.sharedInstance.awaitGetProducts(listOf(StoreIds.productOf(id))).first()
        apply(Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(screen, product).build()).customerInfo)
        PurchaseResult.DONE
    } catch (e: PurchasesTransactionException) {
        when {
            e.userCancelled -> PurchaseResult.CANCELLED

            e.code == PurchasesErrorCode.PaymentPendingError -> PurchaseResult.PENDING

            // 재설치 뒤 구매를 아직 못 가져온 사람이 다시 샀다. 돈을 또 받지 않고 구매를 가져온다.
            e.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> restore().takeIf { it == PurchaseResult.DONE }
                ?: PurchaseResult.FAILED

            else -> PurchaseResult.FAILED
        }
    } catch (_: PurchasesException) {
        PurchaseResult.FAILED
    } catch (_: IllegalStateException) {
        PurchaseResult.FAILED
    } catch (_: NoSuchElementException) {
        PurchaseResult.FAILED
    }

    override suspend fun restore(): PurchaseResult = try {
        val info = Purchases.sharedInstance.awaitRestore()
        state.onSynced(planOf(info))
        if (info.entitlements.active.isEmpty()) PurchaseResult.NOTHING else PurchaseResult.DONE
    } catch (_: PurchasesException) {
        PurchaseResult.FAILED
    }

    private fun watchActivity() {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    this@RevenueCatBilling.activity = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (this@RevenueCatBilling.activity === activity) this@RevenueCatBilling.activity = null
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
