package org.sjhstudio.diary.core.data

import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.model.WelcomeStep

/**
 * 시작 흐름과 맥락 팁의 상태.
 * 각 단계는 끝나면 다시 뜨지 않는다(FR-001).
 */
interface WelcomeRepository : ContextTips {
    suspend fun step(): WelcomeStep

    /**
     * 옮긴 일기 수.
     * 이전한 적이 없으면 null이다.
     */
    fun migratedEntries(): Int?

    suspend fun letterRead()

    suspend fun onboardingFinished()
}

class DefaultWelcomeRepository(
    private val store: WelcomeStore,
    private val onLetterRead: suspend () -> Unit = {},
    private val migrated: () -> Int?
) : WelcomeRepository {
    private var tipShown = false

    override suspend fun step(): WelcomeStep {
        val prefs = store.current()
        val updated = migrated() != null
        return when {
            updated && !prefs.letterSeen -> WelcomeStep.LETTER
            prefs.onboardingDone -> WelcomeStep.DONE
            updated -> WelcomeStep.ONBOARDING_UPDATED
            else -> WelcomeStep.ONBOARDING_NEW
        }
    }

    override fun migratedEntries(): Int? = migrated()

    override suspend fun letterRead() {
        store.letterSeen()
        onLetterRead()
    }

    override suspend fun onboardingFinished() = store.onboardingDone().let { }

    override suspend fun claim(tip: ContextTip): Boolean {
        if (tipShown || tip.id in store.current().tipsSeen) return false
        tipShown = true
        return true
    }

    override suspend fun done(tip: ContextTip) = store.tipSeen(tip.id).let { }
}
