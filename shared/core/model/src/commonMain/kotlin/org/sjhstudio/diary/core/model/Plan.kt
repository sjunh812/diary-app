package org.sjhstudio.diary.core.model

/** 요금제에 따라 달라지는 것(`docs/business.md`). 쓰던 기능은 어느 요금제든 그대로다(헌법 III). */
enum class Plan(val showsAds: Boolean, val photoLimit: Int, val advancedStats: Boolean, val checklist: Boolean) {
    FREE(showsAds = true, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    AD_FREE(showsAds = false, photoLimit = PhotoLimit.FREE, advancedStats = false, checklist = false),
    PLUS(showsAds = false, photoLimit = PhotoLimit.PLUS, advancedStats = true, checklist = true)
}

/** 파는 상품. 첫 출시는 둘이다(D9). */
enum class ProductId { AD_FREE, PLUS_YEARLY }

/** 스토어에서 받은 상품. 가격은 스토어가 준 글자 그대로 보인다(FR-004). */
data class StoreProduct(val id: ProductId, val price: String)

enum class PurchaseResult { DONE, CANCELLED, FAILED }
