package org.sjhstudio.diary.core.model

/** 앱을 연 뒤 일기 전에 지나는 곳(스펙 004). */
enum class WelcomeStep { LETTER, ONBOARDING_UPDATED, ONBOARDING_NEW, DONE }

/**
 * 처음 만날 때 한 번 보이는 팁.
 * [id]가 저장 값이다.
 */
enum class ContextTip(val id: String) {
    INSIGHT_MOOD("insight_mood")
}

/** 세션당 하나, 팁마다 한 번(FR-011). */
interface ContextTips {
    /**
     * 지금 보여도 되면 true다.
     * 한 세션에 하나만 받는다.
     */
    suspend fun claim(tip: ContextTip): Boolean

    suspend fun done(tip: ContextTip)
}
