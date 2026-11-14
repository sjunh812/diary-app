package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

/**
 * 결제 라이브러리에 닿는 가장 얇은 자리
 * 플랫폼 구현(RevenueCat)은 이것만 채우고, 무엇을 언제 믿을지는 [StoreBilling]이 정함
 * 권한은 지금 켜진 권한 이름 묶음으로 돌려줌
 * 스토어에 닿지 못하면 [StoreFailure]를 던짐
 */
interface StoreClient {
    suspend fun entitlements(): Set<String>

    /** 구글 계정에 남은 구매를 가져옴 */
    suspend fun sync(): Set<String>

    /** 스토어 상품 ID와 그 나라 돈으로 쓴 가격 */
    suspend fun prices(storeIds: List<String>): Map<String, Price>

    suspend fun purchase(storeId: String): Set<String>

    suspend fun restore(): Set<String>
}

/** 스토어가 거절했거나 닿지 못함 */
class StoreFailure(val reason: Reason, cause: Throwable? = null) : Exception(cause) {
    enum class Reason { CANCELLED, PENDING, ALREADY_OWNED, OTHER }
}

/**
 * 요금제와 구매를 한곳에서 맡음
 * 마지막 요금제를 기기에 남겨 산 사람이 앱을 켤 때나 오프라인에서도 광고를 보지 않음
 * 재설치로 남긴 값이 없으면 스토어 답을 들을 때까지 요금제를 모른다고 알림
 */
class StoreBilling(private val client: StoreClient, saved: Plan?, override val manageUrl: String, remember: (Plan) -> Unit) :
    PlanRepository,
    PurchaseRepository {
    private val state = StorePlanState(saved, remember)

    override val plan: StateFlow<Plan> = state.plan
    override val known: StateFlow<Boolean> = state.known

    /**
     * 앱을 켜거나 다시 앞으로 올 때 호출. 기기에 남은 답이 아니라 서버 답을 받아 환불·만료를 바로 반영
     * 처음 켠 기기는 구글 계정의 구매를 조용히 가져와 재설치한 사람이 다시 사지 않게 함
     */
    suspend fun refresh() {
        try {
            onUpdate(client.entitlements())
        } catch (_: StoreFailure) {
            // 오프라인이면 남긴 값을 씀
            state.onUnreachable()
        }
    }

    /** 스토어가 권한이 바뀌었다고 알려 올 때도 호출 */
    suspend fun onUpdate(active: Set<String>) {
        if (!state.onAnswer(StoreIds.planOf(active))) return
        try {
            state.onSynced(StoreIds.planOf(client.sync()))
        } catch (_: StoreFailure) {
            state.onUnreachable()
        }
    }

    override suspend fun products(): List<StoreProduct> = try {
        val prices = client.prices(ProductId.entries.map(StoreIds::productOf))
        ProductId.entries.mapNotNull { id -> prices[StoreIds.productOf(id)]?.let { StoreProduct(id, it) } }
    } catch (_: StoreFailure) {
        emptyList()
    }

    override suspend fun buy(id: ProductId): PurchaseResult = try {
        onUpdate(client.purchase(StoreIds.productOf(id)))
        PurchaseResult.DONE
    } catch (e: StoreFailure) {
        when (e.reason) {
            StoreFailure.Reason.CANCELLED -> PurchaseResult.CANCELLED

            StoreFailure.Reason.PENDING -> PurchaseResult.PENDING

            // 재설치 뒤 구매를 못 가져온 채 다시 산 경우. 돈을 또 받지 않고 구매를 가져옴
            StoreFailure.Reason.ALREADY_OWNED -> restore().takeIf { it == PurchaseResult.DONE } ?: PurchaseResult.FAILED

            StoreFailure.Reason.OTHER -> PurchaseResult.FAILED
        }
    }

    override suspend fun restore(): PurchaseResult = try {
        val active = client.restore()
        state.onSynced(StoreIds.planOf(active))
        if (StoreIds.planOf(active) == Plan.FREE) PurchaseResult.NOTHING else PurchaseResult.DONE
    } catch (e: StoreFailure) {
        // iOS는 복원 때 Apple 계정 로그인 창을 띄울 수 있음. 닫은 것은 실패가 아님
        if (e.reason == StoreFailure.Reason.CANCELLED) PurchaseResult.CANCELLED else PurchaseResult.FAILED
    }
}
