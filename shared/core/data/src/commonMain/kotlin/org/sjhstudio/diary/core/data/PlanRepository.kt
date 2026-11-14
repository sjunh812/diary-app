package org.sjhstudio.diary.core.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct

/**
 * 지금 요금제
 * 화면과 기능은 이것만 봄
 * 결제(스펙 006)가 붙으면 구현만 바뀜
 */
interface PlanRepository {
    val plan: StateFlow<Plan>

    /**
     * 요금제를 알면 참
     * 재설치 직후처럼 아직 모르면 거짓이고, 그동안은 광고를 부르지 않음
     * 산 사람에게 광고가 스치면 안 됨
     */
    val known: StateFlow<Boolean> get() = AlwaysKnown

    /**
     * 확정된 요금제. 재설치 직후 스토어 답이 오기 전에는 null
     * 광고, 사진 한도, 체크리스트, 고급 통계는 [plan] 대신 이것을 봄
     * 그래야 산 사람에게 광고나 Plus 권유가 스치지 않음
     */
    val confirmed: Flow<Plan?> get() = combine(plan, known) { plan, known -> plan.takeIf { known } }

    /** 지금 확정된 요금제. 모르면 null */
    val confirmedNow: Plan? get() = plan.value.takeIf { known.value }

    /**
     * 요금제가 확정될 때까지 [wait]만큼 기다림
     * 이미 쓰던 사람은 기기에 남긴 값이 있어 바로 돌아오고, 기다림은 재설치 직후에만 생김
     * 오프라인이라 끝내 모르면 지금 값을 씀
     */
    suspend fun awaitConfirmed(wait: Duration = CONFIRM_WAIT): Plan =
        withTimeoutOrNull(wait) { confirmed.filterNotNull().first() } ?: plan.value
}

private val CONFIRM_WAIT = 3.seconds

private val AlwaysKnown: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

/**
 * 스토어 답을 요금제로 옮기는 규칙
 * 재설치 직후 스토어의 첫 답은 구매를 가져오기 전이라 비어 있음
 * 이 답을 믿으면 산 사람에게 광고가 나가고, 무료가 저장돼 다시 가져오지도 않음
 * 그래서 남긴 값이 없으면 구매를 가져온 답만 믿고, 못 가져오면 모르는 채로 두어 광고를 부르지 않음
 * 저장하지 않으니 앱이 다시 앞으로 올 때나 다음 실행에 다시 시도
 * 남긴 값이 있으면 스토어에 닿지 못해도 그 값을 씀
 */
class StorePlanState(private val saved: Plan?, private val remember: (Plan) -> Unit) {
    private val _plan = MutableStateFlow(saved ?: Plan.FREE)
    private val _known = MutableStateFlow(saved != null)
    private var synced = false

    val plan: StateFlow<Plan> = _plan.asStateFlow()
    val known: StateFlow<Boolean> = _known.asStateFlow()

    /** 스토어 답을 받음. 참이면 구매를 가져와 [onSynced]로 알려야 함 */
    fun onAnswer(plan: Plan): Boolean {
        if (saved == null && !synced && plan == Plan.FREE) return true
        confirm(plan)
        return false
    }

    fun onSynced(plan: Plan) {
        synced = true
        confirm(plan)
    }

    private fun confirm(plan: Plan) {
        _plan.value = plan
        _known.value = true
        remember(plan)
    }
}

/** 스토어의 상품 ID와 권한 이름. 스토어와 RevenueCat에 같은 값을 넣음(`docs/release.md`) */
object StoreIds {
    const val AD_FREE_PRODUCT = "ad_free"
    const val PLUS_PRODUCT = "plus_yearly"
    const val AD_FREE_ENTITLEMENT = "ad_free"
    const val PLUS_ENTITLEMENT = "plus"

    fun productOf(id: ProductId): String = when (id) {
        ProductId.AD_FREE -> AD_FREE_PRODUCT
        ProductId.PLUS_YEARLY -> PLUS_PRODUCT
    }

    /**
     * 스토어가 돌려준 상품 ID에서 기본 ID만 떼어 냄
     * 구독은 `plus_yearly:yearly`처럼 기본 요금제 이름이 붙어 옴
     */
    fun baseOf(storeProductId: String): String = storeProductId.substringBefore(':')

    /** 지금 켜진 권한으로 요금제를 정함. 둘 다 있으면 Plus */
    fun planOf(active: Set<String>): Plan = when {
        PLUS_ENTITLEMENT in active -> Plan.PLUS
        AD_FREE_ENTITLEMENT in active -> Plan.AD_FREE
        else -> Plan.FREE
    }
}

/**
 * 결제가 붙기 전 구현
 * 언제나 무료
 */
class FreePlanRepository : PlanRepository {
    override val plan: StateFlow<Plan> = MutableStateFlow(Plan.FREE).asStateFlow()
}

/**
 * 구매
 * 화면은 이것만 보고 결제 라이브러리를 직접 보지 않음(FR-001)
 */
interface PurchaseRepository {
    /** 스토어 구독 관리 주소 */
    val manageUrl: String

    /**
     * 스토어에 닿지 못하면 빈 목록
     * 가격을 지어내지 않음
     */
    suspend fun products(): List<StoreProduct>

    suspend fun buy(id: ProductId): PurchaseResult

    suspend fun restore(): PurchaseResult
}

/**
 * 결제가 붙기 전 구현
 * 살 수 있는 것이 없음
 */
class NoPurchases : PurchaseRepository {
    override val manageUrl: String = ""

    override suspend fun products(): List<StoreProduct> = emptyList()

    override suspend fun buy(id: ProductId): PurchaseResult = PurchaseResult.FAILED

    override suspend fun restore(): PurchaseResult = PurchaseResult.FAILED
}
