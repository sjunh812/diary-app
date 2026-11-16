package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.datastore.AdPrefs
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.model.quietly

/**
 * 광고 SDK. 공통 코드는 이것만 봄
 * Android는 `AdMobPlayer`, iOS는 `IosAdPlayer`
 */
interface AdPlayer {
    val available: Boolean

    /** 광고 SDK를 미리 켬. 첫 광고가 늦지 않게. 화면 스레드를 막지 않고, 실패해도 던지지 않음 */
    fun warmUp() {}

    /**
     * 전면 광고를 미리 받음
     * 저장한 순간 기다리지 않게 작성 화면을 열 때 호출
     */
    fun prepareInterstitial()

    /**
     * 받아 둔 광고를 띄웠으면 닫힌 뒤 true
     * 광고가 화면에 나와 노출이 잡히는 순간 [onImpression] 호출(AdMob `onAdImpression`)
     */
    suspend fun showInterstitial(onImpression: () -> Unit): Boolean

    /**
     * 보상 광고를 받음. 받지 못하면 null
     * 받는 동안은 취소할 수 있고, 취소하면 띄우지 않음(시트를 닫거나 앱이 잠김)
     */
    suspend fun loadRewarded(): ReadyReward?
}

/** 받아 둔 보상 광고 */
fun interface ReadyReward {
    /**
     * 띄우고 닫힐 때까지 기다림
     * 끝까지 보면 닫히기 전이라도 바로 [onEarned]. 닫기 전에 앱이 꺼져도 보상이 남게
     */
    suspend fun show(onEarned: () -> Unit)
}

object NoAdPlayer : AdPlayer {
    override val available = false

    override fun prepareInterstitial() = Unit

    override suspend fun showInterstitial(onImpression: () -> Unit) = false

    override suspend fun loadRewarded(): ReadyReward? = null
}

/**
 * 전면 광고의 원격 스위치(FR-007, 헌법 III)
 * 두 플랫폼 모두 Firebase Remote Config의 `interstitial_enabled`를 따름
 * 값을 받지 못하면 마지막 값, 처음이면 켜짐
 */
fun interface AdSwitch {
    fun enabled(): Boolean
}

/**
 * 광고를 보일지 정하는 곳
 * 자리와 빈도 규칙은 `docs/business.md` "광고 자리"
 */
class AdRepository(
    private val plans: PlanRepository,
    private val store: AdStore,
    private val player: AdPlayer,
    private val switch: AdSwitch = AdSwitch { true },
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    /** 저장 공간이 차 파일에 못 적어도 이 프로세스 안에서는 하루 한 번을 지킴 */
    private var shownDay: String? = null

    val showAds: Flow<Boolean> =
        combine(plans.confirmed, store.prefs) { plan, prefs -> plan?.showsAds == true && adsAllowed(prefs) }
            // 기록을 읽지 못하면 덜 보이는 쪽으로 판단
            .catch { emit(false) }
            .distinctUntilChanged()

    /**
     * 광고를 볼 사용자만 앱 시작 뒤 SDK를 미리 켬. Plus 사용자는 켜지 않음
     * 오늘 전면 광고 차례가 남았으면 미리 받아 둠. 저장한 순간 바로 띄워야 해서
     */
    suspend fun warmUp() {
        quietly(Unit) {
            if (!showAds.first()) return@quietly
            player.warmUp()
            if (interstitialDue()) player.prepareInterstitial()
        }
    }

    private fun adsAllowed(prefs: AdPrefs) = player.available && !rewarded(prefs.rewardUntil)

    /** 기기 시각을 앞으로 돌려 받은 값은 믿지 않음 */
    private fun rewarded(until: Instant?): Boolean {
        val left = (until ?: return false) - clock.now()
        return left.isPositive() && left <= REWARD
    }

    private fun today(): String = currentDate(clock, timeZone()).toString()

    /** 작성 화면을 열 때 호출. 오늘 아직 안 봤으면 저장한 순간 띄울 광고를 미리 받음 */
    suspend fun prepareAfterSave() {
        quietly(Unit) { if (interstitialDue()) player.prepareInterstitial() }
    }

    /**
     * 일기를 저장해 작성 화면이 닫힐 때 호출
     * 하루 한 번만 띄움
     * 시각이 뒤로 가면 덜 보이는 쪽으로 판단
     * 전면 광고 차례였으면 참. 못 띄웠어도 차례였으면 참이라 그 순간에는 리뷰 창을 요청하지 않음(스펙 007 FR-007)
     */
    suspend fun showAfterSave(): Boolean = onceStarted(false) {
        if (!interstitialDue()) return@onceStarted false
        // 광고가 화면에 나와 노출이 잡힌 순간 기록
        // 전면 광고 수익은 끝까지 보기가 아니라 노출로 잡혀, 그 뒤 앱이 꺼져도 같은 날 다시 띄우지 않음
        // 나오기 전에 꺼지거나 못 띄웠으면 기록하지 않아 다음 저장에 다시 띄움
        // 공간이 차 못 적어도 이 프로세스 안에서는 [shownDay]가 하루 한 번을 지킴
        // 광고 SDK가 실패해도 차례였던 사실은 그대로
        val day = today()
        quietly(Unit) {
            coroutineScope {
                val seen = CompletableDeferred<Unit>()
                val record = launch {
                    seen.await()
                    quietly { store.interstitialShown(day) }
                }
                val shown = player.showInterstitial {
                    shownDay = day
                    seen.complete(Unit)
                }
                when {
                    // 노출 알림 없이 닫혀도 화면에는 나왔으니 본 것으로 기록
                    shown -> {
                        shownDay = day
                        seen.complete(Unit)
                    }

                    !seen.isCompleted -> record.cancel()
                }
            }
        }
        true
    }

    /**
     * 이번 전면 광고 앞에 첫 안내를 띄울지. 안내에 답하기 전까지 광고 차례마다 참
     * 기록을 읽지 못하면 안내 없이 지금처럼 광고만
     */
    suspend fun interstitialNoticeDue(): Boolean = quietly(false) { interstitialDue() && !store.current().interstitialNoticed }

    /**
     * 안내에서 버튼을 누름. 어느 버튼이든 다시 띄우지 않음(`docs/business.md` 광고 자리)
     * 답하기 전에 잠기거나 앱이 꺼졌으면 다음 차례에 다시 띄움
     */
    suspend fun interstitialNoticeAnswered() {
        quietly(Unit) { store.interstitialNoticed() }
    }

    private suspend fun interstitialDue(): Boolean {
        val prefs = store.current()
        val shown = listOfNotNull(prefs.lastInterstitialDay, shownDay).maxOrNull()
        val allowed = plans.confirmedNow?.showsAds == true && switch.enabled() && adsAllowed(prefs)
        return allowed && (shown == null || shown < today())
    }

    /** 보상 광고는 한 번에 하나. 앞서 부른 보기가 끝날 때까지 기다림 */
    private val rewardTurn = Mutex()

    /**
     * 끝까지 봐서 보상을 받았으면 true
     * 받는 동안 취소되면(시트를 닫음) 띄우지 않고 CancellationException
     * 띄운 뒤로는 화면이 다시 만들어져도 끝까지 감
     */
    suspend fun watchReward(): Boolean = quietly(false) {
        rewardTurn.withLock {
            // 앞서 누른 보기가 이미 보상을 받았으면 또 띄우지 않고 그 결과를 알림
            if (rewarded(store.prefs.first().rewardUntil)) return@withLock true
            if (!showAds.first()) return@withLock false
            val reward = player.loadRewarded() ?: return@withLock false
            withContext(NonCancellable) { reward.watch { store.rewardUntil(clock.now() + REWARD) } }
        }
    }

    private companion object {
        val REWARD = 24.hours
    }
}

/**
 * 광고가 뜨면 끝까지 기다림
 * 영상을 가로로 보고 돌아오면 화면이 다시 만들어져 부른 쪽이 먼저 끝남
 * 그때 멈추면 본 광고가 기록되지 않아 같은 날 또 뜨고, 끝까지 본 보상도 잃음
 */
private suspend fun <T> onceStarted(failed: T, block: suspend () -> T): T = quietly(failed) { withContext(NonCancellable) { block() } }

/** 광고는 곁일. 광고 쪽 실패로 앱이 꺼지면 안 됨. 다음 기회에 다시 시도 */
private suspend fun <T> quietly(failed: T, block: suspend () -> T): T = orElseOnFailure(failed) { block() }

/**
 * 띄우고 끝까지 본 순간 [record]로 보상을 적음. 닫기 전에 앱이 꺼져도 하루 광고 없음이 남음
 * 적지 못했으면(공간 부족) 받지 못한 것으로 알림
 */
private suspend fun ReadyReward.watch(record: suspend () -> Unit): Boolean = coroutineScope {
    val earned = CompletableDeferred<Unit>()
    val written = async {
        earned.await()
        orElseOnFailure(false) {
            record()
            true
        }
    }
    show { earned.complete(Unit) }
    if (!earned.isCompleted) written.cancel()
    earned.isCompleted && written.await()
}
