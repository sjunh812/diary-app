package org.sjhstudio.diary.debug

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

/**
 * 개발자 모드에서 고른 요금제
 * 개발 빌드에만 있고, 앱을 다시 켜도 남음
 */
class DevPlanRepository(context: Context) : PlanRepository {
    private val prefs = context.getSharedPreferences("dev_mode", Context.MODE_PRIVATE)
    private val chosen = MutableStateFlow(prefs.getString(KEY, null)?.let(Plan::valueOf) ?: Plan.FREE)

    override val plan: StateFlow<Plan> = chosen.asStateFlow()

    fun choose(plan: Plan) {
        prefs.edit { putString(KEY, plan.name) }
        chosen.value = plan
    }

    private companion object {
        const val KEY = "plan"
    }
}

/**
 * 개발자 모드의 가짜 결제
 * 스토어 없이 구매 흐름을 끝까지 돌아 봄
 */
class FakePurchases(private val plans: DevPlanRepository) : PurchaseRepository {
    override val manageUrl: String = "https://play.google.com/store/account/subscriptions"

    override suspend fun products(): List<StoreProduct> =
        listOf(StoreProduct(ProductId.AD_FREE, "₩9,900"), StoreProduct(ProductId.PLUS_YEARLY, "₩14,900"))

    override suspend fun buy(id: ProductId): PurchaseResult {
        plans.choose(if (id == ProductId.PLUS_YEARLY) Plan.PLUS else Plan.AD_FREE)
        return PurchaseResult.DONE
    }

    // 스토어처럼 고른 요금제가 없으면 복원할 것이 없음. 늘 복원했다고 하면 시험이 거짓 안내를 놓침
    override suspend fun restore(): PurchaseResult = if (plans.plan.value == Plan.FREE) PurchaseResult.NOTHING else PurchaseResult.DONE
}
