package org.sjhstudio.diary.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.model.quietly

/**
 * 안내마다 따로, 닫거나 할 일을 누를 때까지 보임(스펙 004 FR-011)
 * 앱이 켜져 있는 동안 쥐고 있음
 * 탭을 오가 화면이 새로 그려져도 첫 프레임부터 같은 자리
 */
interface ContextTips {
    /** 보일 안내면 참. 기록을 읽기 전이면 null */
    fun isOpen(tip: ContextTip): Boolean?

    /** 기록을 읽어둠. 이미 읽었으면 그대로 */
    suspend fun load()

    /** 바로 빼고 기록은 앱 범위에서. 부른 화면이 사라져도 끝까지 씀 */
    fun done(tip: ContextTip)
}

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

class DefaultWelcomeRepository(
    private val store: WelcomeStore,
    private val scope: CoroutineScope,
    private val migrated: suspend () -> Int?
) : WelcomeRepository {
    /** 보일 안내. 기록을 읽기 전이면 null */
    private val openTips = MutableStateFlow<Set<ContextTip>?>(null)

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

    override fun isOpen(tip: ContextTip): Boolean? = openTips.value?.contains(tip)

    override suspend fun load() {
        if (openTips.value != null) return
        // 기록을 읽지 못하면 팁을 보이지 않음(덜 보이는 쪽)
        val seen = orElseOnFailure(null) { store.current().tipsSeen }
        val unseen = if (seen == null) emptySet() else ContextTip.entries.filterNot { it.id in seen }.toSet()
        openTips.compareAndSet(null, unseen)
    }

    // 공간이 차 못 적어도 이 프로세스 안에서는 다시 보이지 않음
    override fun done(tip: ContextTip) {
        openTips.update { it?.minus(tip) }
        scope.launch { quietly { store.tipSeen(tip.id) } }
    }
}
