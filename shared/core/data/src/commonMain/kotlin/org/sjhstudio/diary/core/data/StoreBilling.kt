package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Price
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

/**
 * 결제 라이브러리에 닿는 가장 얇은 자리
 * 플랫폼 구현(RevenueCat)은 이것만 채우고, 무엇을 언제 믿을지는 [StoreBilling]이 정함
 * 권한은 지금 켜진 권한과 가장 먼저 끝나는 시각으로 돌려줌([StoreAnswer])
 * 스토어에 닿지 못하면 [StoreFailure]를 던짐
 */
interface StoreClient {
    suspend fun entitlements(): StoreAnswer

    /** 스토어 계정에 남은 구매를 가져옴. iOS는 로그인 창을 띄울 수 있어 설치마다 한 번만 부름 */
    suspend fun sync(): StoreAnswer

    /** 스토어 상품 ID와 그 나라 돈으로 쓴 가격 */
    suspend fun prices(storeIds: List<String>): Map<String, Price>

    suspend fun purchase(storeId: String): StoreAnswer

    suspend fun restore(): StoreAnswer
}

/**
 * 스토어 답. [active]는 지금 켜진 권한 이름
 * [expiresAt]은 켜진 권한 가운데 가장 먼저 끝나는 시각. 평생 상품뿐이면 null
 * [answeredAt]은 서버가 답한 시각. 기기 시계와 달라도 남은 시간을 서버 시계로 잼
 */
data class StoreAnswer(val active: Set<String>, val expiresAt: Instant? = null, val answeredAt: Instant? = null)

/** 스토어가 거절했거나 닿지 못함 */
class StoreFailure(val reason: Reason, cause: Throwable? = null) : Exception(cause) {
    enum class Reason { CANCELLED, PENDING, ALREADY_OWNED, OTHER }
}

/**
 * 요금제와 구매를 한곳에서 맡음
 * 마지막 요금제를 기기에 남겨 산 사람이 앱을 켤 때나 오프라인에서도 광고를 보지 않음
 * 재설치로 남긴 값이 없으면 스토어 답을 들을 때까지 요금제를 모른다고 알림
 * 화면 스레드에서만 부름. 상태를 잠금 없이 다룸
 */
class StoreBilling(
    private val client: StoreClient,
    saved: Plan?,
    override val manageUrl: String,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    remember: (Plan) -> Unit
) : PlanRepository,
    PurchaseRepository {
    private val state = StorePlanState(saved, remember)

    /** 구독이 끝나는 시각에 다시 묻는 일. 앱을 켜 둔 채 만료돼도 혜택이 남지 않음 */
    private var expiryCheck: Job? = null

    override val plan: StateFlow<Plan> = state.plan
    override val known: StateFlow<Boolean> = state.known

    /**
     * 앱이 앞으로 올 때마다(켤 때 포함) 서버 답을 받음. 그만 들을 때까지 돌아오지 않음
     * 서버 답으로 환불·만료를 반영하고, 처음 설치라 요금제를 모르면 계정의 구매를 가져옴
     * 하나씩 차례로 처리해 구매 가져오기가 겹치지 않음
     */
    suspend fun follow(foregrounds: Flow<Unit>) {
        foregrounds.conflate().collect {
            refresh()
            if (!state.known.value) recover()
        }
    }

    /** 스토어가 권한이 바뀌었다고 알려 올 때 호출. 요금제만 바꿈 */
    fun onUpdate(answer: StoreAnswer) {
        watchExpiry(answer)
        state.onAnswer(StoreIds.planOf(answer.active))
    }

    internal suspend fun refresh() {
        try {
            onUpdate(client.entitlements())
        } catch (_: StoreFailure) {
            // 닿지 못하면 남긴 값 그대로. 없으면 모르는 채로
        }
    }

    /** 계정에 남은 구매를 가져옴(재설치 뒤 다시 사지 않게) */
    private suspend fun recover() {
        try {
            val answer = client.sync()
            watchExpiry(answer)
            state.onRecovered(StoreIds.planOf(answer.active))
        } catch (e: StoreFailure) {
            // 로그인 창을 닫은 것은 가져올 구매가 없다는 답
            if (e.reason == StoreFailure.Reason.CANCELLED) state.onRecovered(Plan.FREE)
            // 그 밖은 무료로 정하지 않고 다음에 앞으로 올 때 다시(구매자에게 광고 방지)
        }
    }

    private fun watchExpiry(answer: StoreAnswer) {
        expiryCheck?.cancel()
        val at = answer.expiresAt ?: return
        val left = at - (answer.answeredAt ?: clock.now())
        // 이미 지났는데 켜져 있으면 스토어가 처리 중. 바로 다시 묻지 않고 앱이 앞으로 올 때 물음
        if (!left.isPositive()) return
        expiryCheck =
            scope.launch {
                // 스토어가 만료를 처리할 틈을 두고 물음
                delay(left + EXPIRY_GRACE)
                refresh()
            }
    }

    override suspend fun products(): List<StoreProduct> = try {
        val prices = client.prices(ProductId.entries.map(StoreIds::productOf))
        ProductId.entries.mapNotNull { id -> prices[StoreIds.productOf(id)]?.let { StoreProduct(id, it) } }
    } catch (_: StoreFailure) {
        emptyList()
    }

    override suspend fun buy(id: ProductId): PurchaseResult = try {
        val answer = client.purchase(StoreIds.productOf(id))
        onUpdate(answer)
        // 결제가 끝나도 혜택이 켜지지 않았으면 성공으로 닫지 않음. 실패 안내가 [구매 복원]을 알려 줌
        if (StoreIds.planOf(answer.active).includes(id)) PurchaseResult.DONE else PurchaseResult.FAILED
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
        val answer = client.restore()
        watchExpiry(answer)
        state.onRecovered(StoreIds.planOf(answer.active))
        if (StoreIds.planOf(answer.active) == Plan.FREE) PurchaseResult.NOTHING else PurchaseResult.DONE
    } catch (e: StoreFailure) {
        // iOS는 복원 때 Apple 계정 로그인 창을 띄울 수 있음. 닫은 것은 실패가 아님
        if (e.reason == StoreFailure.Reason.CANCELLED) PurchaseResult.CANCELLED else PurchaseResult.FAILED
    }
}

private val EXPIRY_GRACE = 5.seconds
