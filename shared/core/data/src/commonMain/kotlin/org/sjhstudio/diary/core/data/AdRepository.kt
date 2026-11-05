package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.datastore.AdPrefs
import org.sjhstudio.diary.core.datastore.AdStore

/** 광고 SDK. 공통 코드는 이것만 본다. 안드로이드만 구현이 있고 iOS는 R2에 붙인다. */
interface AdPlayer {
    val available: Boolean

    /** 전면 광고를 미리 받는다. 돌아갈 때 기다리지 않게 하려고 저장할 때 부른다. */
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

/** 원격 스위치(FR-007). 지금은 늘 켜짐이고 Firebase Remote Config는 스펙 005에서 붙인다. */
fun interface AdSwitch {
    fun enabled(): Boolean
}

/** 광고를 보일지 정하는 곳. 자리와 빈도 규칙은 `docs/business.md` "광고 자리"다. */
class AdRepository(
    private val plans: PlanRepository,
    private val store: AdStore,
    private val player: AdPlayer,
    private val switch: AdSwitch = AdSwitch { true },
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    val showAds: Flow<Boolean> =
        combine(plans.plan, store.prefs) { plan, prefs -> plan.showsAds && adsAllowed(prefs) }.distinctUntilChanged()

    private fun adsAllowed(prefs: AdPrefs) = switch.enabled() && player.available && !rewarded(prefs.rewardUntil)

    /** 기기 시각을 앞으로 돌려 받은 값은 믿지 않는다. */
    private fun rewarded(until: Instant?): Boolean {
        val left = (until ?: return false) - clock.now()
        return left.isPositive() && left <= REWARD
    }

    private fun today(): String = clock.now().toLocalDateTime(timeZone()).date.toString()

    /** 오늘 아직 안 봤으면 미리 받는다. */
    suspend fun afterSave() {
        if (interstitialDue()) player.prepareInterstitial()
    }

    /** 저장 뒤 목록으로 돌아왔을 때 부른다. 하루 한 번만 띄운다. 시각이 뒤로 가면 덜 보이는 쪽으로 본다. */
    suspend fun backToList() {
        if (interstitialDue() && player.showInterstitial()) store.interstitialShown(today())
    }

    private suspend fun interstitialDue(): Boolean {
        val prefs = store.current()
        val shown = prefs.lastInterstitialDay
        return plans.plan.value.showsAds && adsAllowed(prefs) && (shown == null || shown < today())
    }

    suspend fun watchReward(): Boolean {
        if (!showAds.first() || !player.showRewarded()) return false
        store.rewardUntil(clock.now() + REWARD)
        return true
    }

    private companion object {
        val REWARD = 24.hours
    }
}
