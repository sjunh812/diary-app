package org.sjhstudio.diary.feature.plus

/**
 * Plus 혜택
 * 결제 화면은 핵심만 설명까지, 다른 혜택은 [모두 보기] 시트에서
 * 핵심을 바꾸면 결제 화면 그림(`plus_benefits.json`)도 함께 고침. 핵심 차례는 그림의 아이콘 차례와 같음
 */
enum class PlusBenefit(internal val core: Boolean) {
    AD_FREE(core = true),
    PHOTOS(core = true),
    STATS(core = true),

    /** 테마 색 직접 고르기(스펙 010) */
    THEME(core = true),
    CHECKLIST(core = false),
    LIST_FORMATTING(core = false),

    /** Plus 기분 테마(스펙 013) */
    MOOD_THEME(core = false)
}

internal val coreBenefits: List<PlusBenefit> = PlusBenefit.entries.filter { it.core }

/** 다른 혜택에서 들어왔으면 그 혜택. 누르고 온 것이 결제 화면에 보이게. 핵심은 이미 보여 null */
internal fun enteredOther(from: PlusBenefit?): PlusBenefit? = from?.takeUnless { it.core }
