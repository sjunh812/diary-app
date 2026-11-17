package org.sjhstudio.diary.feature.plus

/**
 * Plus 혜택
 * 핵심은 결제 화면에 설명까지, 다른 혜택은 이름만
 * 핵심을 바꾸면 결제 화면 그림(`plus_benefits.json`)도 함께 고침
 */
enum class PlusBenefit(internal val core: Boolean) {
    AD_FREE(core = true),
    PHOTOS(core = true),
    STATS(core = true),
    CHECKLIST(core = false),
    LIST_FORMATTING(core = false)
}

/** 결제 화면에 보이는 다른 혜택 줄 수. 넘치면 [모두 보기] 시트에서 봄 */
internal const val OTHER_BENEFIT_LINES = 3

internal val coreBenefits: List<PlusBenefit> = PlusBenefit.entries.filter { it.core }

/** 결제 화면의 다른 혜택. 들어온 혜택을 맨 위로 */
internal fun otherBenefits(from: PlusBenefit?, limit: Int = OTHER_BENEFIT_LINES): List<PlusBenefit> =
    PlusBenefit.entries.filterNot { it.core }.sortedByDescending { it == from }.take(limit)
