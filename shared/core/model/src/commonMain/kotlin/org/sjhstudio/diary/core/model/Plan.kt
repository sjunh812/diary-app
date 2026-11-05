package org.sjhstudio.diary.core.model

/** 요금제에 따라 달라지는 것(`docs/business.md`). 쓰던 기능은 어느 요금제든 그대로다(헌법 III). */
enum class Plan(val showsAds: Boolean, val photoLimit: Int, val advancedStats: Boolean) {
    FREE(showsAds = true, photoLimit = PhotoLimit.FREE, advancedStats = false),
    AD_FREE(showsAds = false, photoLimit = PhotoLimit.FREE, advancedStats = false),
    PLUS(showsAds = false, photoLimit = PhotoLimit.PLUS, advancedStats = true)
}
