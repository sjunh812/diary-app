package org.sjhstudio.diary.core.data

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import okio.IOException
import org.sjhstudio.diary.core.datastore.AdPrefs
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure

/**
 * 광고 SDK. 공통 코드는 이것만 본다.
 * 안드로이드만 구현이 있고 iOS는 R2에 붙인다.
 */
interface AdPlayer {
    val available: Boolean

    /**
     * 전면 광고를 미리 받는다.
     * 돌아갈 때 기다리지 않게 하려고 저장할 때 부른다.
     */
    fun prepareInterstitial()

    /** 받아 둔 광고가 있어 띄웠으면 true다. */
    suspend fun showInterstitial(): Boolean

    /** 끝까지 봐서 보상을 받았으면 true다. */
    suspend fun showRewarded(): Boolean
}

object NoAdPlayer : AdPlayer {
    override val available = false

    override fun prepareInterstitial() = Unit

    override suspend fun showInterstitial() = false

    override suspend fun showRewarded() = false
}

/**
 * 전면 광고의 원격 스위치(FR-007, 헌법 III).
 * 안드로이드는 Firebase Remote Config의 `interstitial_enabled`를 따른다.
 * 값을 받지 못하면 마지막 값을 쓰고, 처음이면 켜짐이다.
 */
fun interface AdSwitch {
    fun enabled(): Boolean
}

/**
 * 광고를 보일지 정하는 곳.
 * 자리와 빈도 규칙은 `docs/business.md` "광고 자리"다.
 */
class AdRepository(
    private val plans: PlanRepository,
    private val store: AdStore,
    private val player: AdPlayer,
    private val switch: AdSwitch = AdSwitch { true },
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    /** 저장 공간이 차서 파일에 적지 못해도 이 프로세스 안에서는 하루 한 번을 지킨다. */
    private var shownDay: String? = null

    val showAds: Flow<Boolean> =
        combine(plans.plan, plans.known, store.prefs) { plan, known, prefs -> known && plan.showsAds && adsAllowed(prefs) }
            // 기록을 읽지 못하면 덜 보이는 쪽으로 본다.
            .catch { if (it is IOException) emit(false) else throw it }
            .distinctUntilChanged()

    private fun adsAllowed(prefs: AdPrefs) = player.available && !rewarded(prefs.rewardUntil)

    /** 기기 시각을 앞으로 돌려 받은 값은 믿지 않는다. */
    private fun rewarded(until: Instant?): Boolean {
        val left = (until ?: return false) - clock.now()
        return left.isPositive() && left <= REWARD
    }

    private fun today(): String = currentDate(clock, timeZone()).toString()

    /** 오늘 아직 안 봤으면 미리 받는다. */
    suspend fun afterSave() {
        quietly(Unit) { if (interstitialDue()) player.prepareInterstitial() }
    }

    /**
     * 저장 뒤 목록으로 돌아왔을 때 부른다.
     * 하루 한 번만 띄운다.
     * 시각이 뒤로 가면 덜 보이는 쪽으로 본다.
     */
    suspend fun backToList() {
        onceStarted(Unit) {
            if (interstitialDue() && player.showInterstitial()) {
                val day = today()
                shownDay = day
                store.interstitialShown(day)
            }
        }
    }

    /**
     * 오늘은 전면 광고를 띄우지 않는다.
     * 업데이트 뒤 개발자 편지를 처음 읽은 날에 부른다(`docs/business.md` 광고 자리).
     * 이미 띄운 날로 적으므로 날짜가 바뀌면 평소대로 돌아간다.
     */
    suspend fun skipInterstitialToday() {
        onceStarted(Unit) {
            val day = today()
            shownDay = day
            store.interstitialShown(day)
        }
    }

    private suspend fun interstitialDue(): Boolean {
        val prefs = store.current()
        val shown = listOfNotNull(prefs.lastInterstitialDay, shownDay).maxOrNull()
        val allowed = plans.known.value && plans.plan.value.showsAds && switch.enabled() && adsAllowed(prefs)
        return allowed && (shown == null || shown < today())
    }

    suspend fun watchReward(): Boolean = onceStarted(false) {
        if (!showAds.first() || !player.showRewarded()) return@onceStarted false
        store.rewardUntil(clock.now() + REWARD)
        true
    }

    /**
     * 광고가 뜨면 끝까지 기다린다.
     * 영상을 가로로 보고 돌아오면 화면이 다시 만들어져 부른 쪽이 먼저 끝난다.
     * 그때 멈추면 본 광고가 기록되지 않아 같은 날 또 뜨고, 끝까지 본 보상을 잃는다.
     */
    private suspend fun <T> onceStarted(failed: T, block: suspend () -> T): T = quietly(failed) { withContext(NonCancellable) { block() } }

    /** 광고는 곁일이다. 광고 쪽 실패로 앱이 꺼지면 안 된다. 다음 기회에 다시 해 본다. */
    private suspend fun <T> quietly(failed: T, block: suspend () -> T): T = orElseOnFailure(failed) { block() }

    private companion object {
        val REWARD = 24.hours
    }
}
