package org.sjhstudio.diary.feature.plus

/**
 * Plus 혜택
 * 결제 화면은 핵심만 설명까지, 다른 혜택은 [모두 보기] 시트에서
 * 핵심을 바꾸면 결제 화면 그림(`plus_benefits.json`)도 함께 고침. 핵심 차례는 그림의 아이콘 차례와 같음
 * [newIn]은 그 혜택이 들어온 버전. 같은 기능 업데이트(앞 두 자리) 동안만 "새 기능"을 보임
 */
enum class PlusBenefit(internal val core: Boolean, internal val newIn: String? = null) {
    AD_FREE(core = true),
    PHOTOS(core = true),
    STATS(core = true),

    /** 테마 색 직접 고르기(스펙 010) */
    THEME(core = true),
    CHECKLIST(core = false),
    LIST_FORMATTING(core = false),

    /** Plus 기분 테마(스펙 013) */
    MOOD_THEME(core = false, newIn = "2.3")
}

internal val coreBenefits: List<PlusBenefit> = PlusBenefit.entries.filter { it.core }

/** 다른 혜택에서 들어왔으면 그 혜택. 누르고 온 것이 결제 화면에 보이게. 핵심은 이미 보여 null */
internal fun enteredOther(from: PlusBenefit?): PlusBenefit? = from?.takeUnless { it.core }

/** 지금 앱 버전에서 새로 나온 혜택. 다음 기능 업데이트 때 저절로 빠져 오래된 "새 기능"이 남지 않음 */
fun newBenefitsIn(appVersion: String): Set<PlusBenefit> = PlusBenefit.entries.filterTo(mutableSetOf()) { benefit ->
    benefit.newIn?.let { featureRelease(it) == featureRelease(appVersion) } ==
        true
}

private fun featureRelease(version: String): List<String> = version.trim().split('.').take(2)
