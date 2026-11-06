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
