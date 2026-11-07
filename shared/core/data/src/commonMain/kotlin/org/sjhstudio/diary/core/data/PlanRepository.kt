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
