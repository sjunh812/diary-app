package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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

    /** 화면에 보이기 시작함. 그날은 다른 팁을 띄우지 않음(스펙 008 FR-011) */
    fun shown(tip: ContextTip) = Unit

    /** 일이 생겨 여는 팁([ContextTip.byEvent])을 엶 */
    fun open(tip: ContextTip) = Unit

    /** 3줄 보기에서 잘린 일기를 상세로 엶. 여러 번이면 전체 보기 팁을 엶(스펙 008 FR-010) */
    fun clippedOpened() = Unit

    /** 팁이 열리거나 닫히면 바뀜. 같은 자리 후보를 다시 고를 때 씀 */
    val revision: StateFlow<Int> get() = NoRevision
}

private val NoRevision: StateFlow<Int> = MutableStateFlow(0)

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
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
    private val migrated: suspend () -> Int?
) : WelcomeRepository {
    /** 보일 안내. 기록을 읽기 전이면 null */
    private val openTips = MutableStateFlow<Set<ContextTip>?>(null)

    /** 일이 생겨 열린 팁 */
    private val opened = MutableStateFlow<Set<ContextTip>>(emptySet())

    /** 그날 보인 팁. 하루에 하나 */
    private val shownOn = MutableStateFlow<Pair<String, String>?>(null)

    private val clipped = MutableStateFlow(0)

    private val changes = MutableStateFlow(0)
    override val revision: StateFlow<Int> = changes

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

    override fun isOpen(tip: ContextTip): Boolean? {
        val unseen = openTips.value ?: return null
        val day = shownOn.value
        return when {
            tip !in unseen -> false

            tip.byEvent && tip !in opened.value -> false

            // 같은 날 이미 다른 팁이 보였으면 다음 날
            day != null && day.first == today().toString() && day.second != tip.id -> false

            else -> true
        }
    }

    /** 시작 단계와 팁 자리가 함께 불러도 한 번만 읽음. 늦게 끝난 쪽이 오늘 보인 팁 기록을 되돌리지 않게 */
    private val loading = Mutex()

    override suspend fun load() = loading.withLock {
        if (openTips.value != null) return@withLock
        // 기록을 읽지 못하면 팁을 보이지 않음(덜 보이는 쪽)
        val prefs = orElseOnFailure(null) { store.current() }
        if (prefs != null) {
            // 읽기 전에 시작 단계가 연 팁(옛 설정 이음)은 기록이 늦게 써져도 남김
            opened.update { it + ContextTip.entries.filter { tip -> tip.id in prefs.tipsOpened } }
            // 읽는 동안 화면이 먼저 적은 오늘 팁이 있으면 그쪽이 새것
            shownOn.compareAndSet(null, prefs.lastTipDay?.let { day -> prefs.lastTip?.let { day to it } })
            clipped.value = prefs.clippedOpens
        }
        val unseen = if (prefs == null) emptySet() else ContextTip.entries.filterNot { it.id in prefs.tipsSeen }.toSet()
        openTips.compareAndSet(null, unseen - opened.value.flatMap { replaced[it].orEmpty() }.toSet())
        changes.update { it + 1 }
    }

    override fun shown(tip: ContextTip) {
        val day = today().toString()
        if (shownOn.value == (day to tip.id)) return
        shownOn.value = day to tip.id
        changes.update { it + 1 }
        scope.launch { quietly { store.tipShown(day, tip.id) } }
    }

    override fun open(tip: ContextTip) {
        if (tip in opened.value) return
        opened.update { it + tip }
        changes.update { it + 1 }
        scope.launch { quietly { store.tipOpened(tip.id) } }
        replaced[tip]?.forEach(::done)
    }

    override fun clippedOpened() {
        if (openTips.value?.contains(ContextTip.HOME_FULL_PREVIEW) != true) return
        val count = clipped.updateAndGet { it + 1 }
        scope.launch { quietly { store.clippedOpens(count) } }
        if (count >= CLIPPED_OPENS_FOR_TIP) open(ContextTip.HOME_FULL_PREVIEW)
    }

    private companion object {
        /** 잘린 일기를 이만큼 열면 전체 보기를 알림. 출발점(스펙 008 가정) */
        const val CLIPPED_OPENS_FOR_TIP = 3

        /** 열리면 다시 띄우지 않는 팁. 옛 설정을 이었다고 알린 사람에게 전체 보기를 다시 권하지 않음(스펙 008 US4-4) */
        val replaced = mapOf(ContextTip.HOME_FULL_PREVIEW_KEPT to setOf(ContextTip.HOME_FULL_PREVIEW))
    }

    // 공간이 차 못 적어도 이 프로세스 안에서는 다시 보이지 않음
    override fun done(tip: ContextTip) {
        openTips.update { it?.minus(tip) }
        changes.update { it + 1 }
        scope.launch { quietly { store.tipSeen(tip.id) } }
    }
}
