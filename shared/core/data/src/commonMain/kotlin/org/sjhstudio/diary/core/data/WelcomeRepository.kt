package org.sjhstudio.diary.core.data

import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.model.WelcomeStep

/**
 * 시작 흐름과 맥락 팁의 상태
 * 각 단계는 끝나면 다시 뜨지 않음(FR-001)
 */
interface WelcomeRepository : ContextTips {
    suspend fun step(): WelcomeStep

    /**
     * 옮긴 일기 수
     * 이전한 적이 없으면 null
     */
    suspend fun migratedEntries(): Int?

    suspend fun letterRead()

    suspend fun onboardingFinished()
}

class DefaultWelcomeRepository(private val store: WelcomeStore, private val migrated: suspend () -> Int?) : WelcomeRepository {
    // 이번 세션에 띄운 안내. 화면을 오가도 닫거나 할 일을 누르기 전까지 같은 안내를 다시 보임
    private var claimed: ContextTip? = null

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

    override suspend fun migratedEntries(): Int? = migrated()

    override suspend fun letterRead() {
        store.letterSeen()
    }

    override suspend fun onboardingFinished() = store.onboardingDone().let { }

    override suspend fun claim(tip: ContextTip): Boolean {
        if (tip.id in store.current().tipsSeen || (claimed != null && claimed != tip)) return false
        claimed = tip
        return true
    }

    override suspend fun done(tip: ContextTip) = store.tipSeen(tip.id).let { }
}
