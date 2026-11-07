package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

/**
 * 지금 요금제.
 * 화면과 기능은 이것만 본다.
 * 결제(스펙 006)가 붙으면 구현만 바뀐다.
 */
interface PlanRepository {
    val plan: StateFlow<Plan>

    /**
     * 요금제를 안다.
     * 재설치 직후처럼 아직 모르면 거짓이고, 그동안은 광고를 부르지 않는다.
     * 산 사람에게 광고가 스치면 안 된다.
     */
    val known: StateFlow<Boolean> get() = AlwaysKnown
}

private val AlwaysKnown: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

/**
 * 스토어 답을 요금제로 옮기는 규칙.
 * 재설치 직후 스토어의 첫 답은 구매를 가져오기 전이라 비어 있다.
 * 이 답을 믿으면 산 사람에게 광고가 나가고, 무료가 저장되어 다시 가져오지도 않는다.
 * 그래서 남긴 값이 없으면 구매를 가져온 답만 믿고, 가져오지 못하면 저장하지 않아 다음 실행에 다시 한다.
 */
class StorePlanState(private val saved: Plan?, private val remember: (Plan) -> Unit) {
    private val _plan = MutableStateFlow(saved ?: Plan.FREE)
    private val _known = MutableStateFlow(saved != null)
    private var synced = false

    val plan: StateFlow<Plan> = _plan.asStateFlow()
    val known: StateFlow<Boolean> = _known.asStateFlow()

    /** 스토어 답을 듣는다. 참이면 구매를 가져와 [onSynced]로 알려 줘야 한다. */
    fun onAnswer(plan: Plan): Boolean {
        if (saved == null && !synced && plan == Plan.FREE) return true
        confirm(plan)
        return false
    }

    fun onSynced(plan: Plan) {
        synced = true
        confirm(plan)
    }

    /** 스토어에 닿지 못했다. 남긴 값이 있으면 그대로 쓴다. */
    fun onUnreachable() {
        _known.value = true
    }

    private fun confirm(plan: Plan) {
        _plan.value = plan
        _known.value = true
        remember(plan)
    }
}

/** 스토어의 상품 ID와 권한 이름. 스토어와 RevenueCat에 같은 값을 넣는다(`docs/release.md`). */
object StoreIds {
    const val AD_FREE_PRODUCT = "ad_free"
    const val PLUS_PRODUCT = "plus_yearly"
    const val AD_FREE_ENTITLEMENT = "ad_free"
    const val PLUS_ENTITLEMENT = "plus"

    fun productOf(id: ProductId): String = when (id) {
        ProductId.AD_FREE -> AD_FREE_PRODUCT
        ProductId.PLUS_YEARLY -> PLUS_PRODUCT
    }

    /** 지금 켜진 권한으로 요금제를 정한다. 둘 다 있으면 Plus다. */
    fun planOf(active: Set<String>): Plan = when {
        PLUS_ENTITLEMENT in active -> Plan.PLUS
        AD_FREE_ENTITLEMENT in active -> Plan.AD_FREE
        else -> Plan.FREE
    }
}

/**
 * 결제가 붙기 전의 구현.
 * 언제나 무료다.
 */
class FreePlanRepository : PlanRepository {
    override val plan: StateFlow<Plan> = MutableStateFlow(Plan.FREE).asStateFlow()
}

/**
 * 구매.
 * 화면은 이것만 보고 결제 라이브러리를 직접 보지 않는다(FR-001).
 */
interface PurchaseRepository {
    /** 스토어 구독 관리 주소. */
    val manageUrl: String

    /**
     * 스토어에 닿지 못하면 빈 목록이다.
     * 가격을 지어내지 않는다.
     */
    suspend fun products(): List<StoreProduct>

    suspend fun buy(id: ProductId): PurchaseResult

    suspend fun restore(): PurchaseResult
}

/**
 * 결제가 붙기 전의 구현.
 * 살 수 있는 것이 없다.
 */
class NoPurchases : PurchaseRepository {
    override val manageUrl: String = ""

    override suspend fun products(): List<StoreProduct> = emptyList()

    override suspend fun buy(id: ProductId): PurchaseResult = PurchaseResult.FAILED

    override suspend fun restore(): PurchaseResult = PurchaseResult.FAILED
}
