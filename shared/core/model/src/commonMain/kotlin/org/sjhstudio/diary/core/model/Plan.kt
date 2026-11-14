package org.sjhstudio.diary.core.model

/** 요금제에 따라 달라지는 것(`docs/business.md`). 쓰던 기능은 어느 요금제든 그대로(헌법 III) */
enum class Plan(val showsAds: Boolean, val photoLimit: Int, val advancedStats: Boolean, val checklist: Boolean) {
    FREE(showsAds = true, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    AD_FREE(showsAds = false, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    PLUS(showsAds = false, photoLimit = PhotoLimit.PLUS, advancedStats = true, checklist = true);

    /** 이 요금제에 [product]의 혜택이 들어 있음. Plus는 광고 제거도 포함 */
    fun includes(product: ProductId): Boolean = when (product) {
        ProductId.AD_FREE -> this != FREE
        ProductId.PLUS_YEARLY -> this == PLUS
    }
}

/** 파는 상품. 첫 출시는 둘(D9) */
enum class ProductId { AD_FREE, PLUS_YEARLY }

/** 스토어에서 받은 상품(FR-004) */
data class StoreProduct(val id: ProductId, val price: Price)

/**
 * 스토어 가격. [formatted]는 스토어가 기기 지역에 맞춰 준 글자
 * 금액은 스토어 값 그대로 쓰고, 원화만 화면에서 "14,900원" 꼴로 보임
 */
data class Price(val formatted: String, val amountMicros: Long, val currencyCode: String)

/**
 * [NOTHING]은 복원할 구매가 없음. 스토어에 닿지 못한 [FAILED]와 다름
 * [PENDING]은 결제 승인 대기 중이고, 승인되면 혜택이 저절로 켜짐
 */
enum class PurchaseResult { DONE, CANCELLED, NOTHING, FAILED, PENDING }
