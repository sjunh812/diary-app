package org.sjhstudio.diary.core.model

/** 요금제에 따라 달라지는 것(`docs/business.md`). 쓰던 기능은 어느 요금제든 그대로(헌법 III) */
enum class Plan(val showsAds: Boolean, val photoLimit: Int, val advancedStats: Boolean, val checklist: Boolean) {
    FREE(showsAds = true, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    AD_FREE(showsAds = false, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    PLUS(showsAds = false, photoLimit = PhotoLimit.PLUS, advancedStats = true, checklist = true)
}

/** 파는 상품. 첫 출시는 둘(D9) */
enum class ProductId { AD_FREE, PLUS_YEARLY }

/** 스토어에서 받은 상품. 가격은 스토어가 준 글자 그대로 보임(FR-004) */
data class StoreProduct(val id: ProductId, val price: String)

/**
 * [NOTHING]은 복원할 구매가 없음. 스토어에 닿지 못한 [FAILED]와 다름.
 * [PENDING]은 결제 승인 대기 중이고, 승인되면 혜택이 저절로 켜짐.
 */
enum class PurchaseResult { DONE, CANCELLED, NOTHING, FAILED, PENDING }
