package org.sjhstudio.diary.app.ads

import androidx.compose.runtime.mutableStateMapOf
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.AdPrivacy
import org.sjhstudio.diary.core.data.ReadyReward
import platform.UIKit.UIView

/**
 * Swift가 구현하는 구글 광고 다리(GoogleAdsBridge.swift)
 * 결과는 화면 스레드에서 콜백으로 돌려줌
 * 광고 요청에 일기 내용을 넣지 않음(헌법 II)
 */
interface IosAdBridge : IosConsentBridge {
    /** 광고 단위가 있어야 true. 출시 빌드에 값이 없으면 광고를 부르지 않음 */
    val available: Boolean

    fun warmUp()

    fun prepareInterstitial()

    /** 띄웠다 닫히면 true, 받아 둔 광고가 없거나 못 띄우면 false */
    fun showInterstitial(onImpression: () -> Unit, onDone: (Boolean) -> Unit)

    /** 보상 광고를 받음. 받았으면 true. 받는 동안 [cancelRewarded]를 부르면 false로 끝나고 받은 광고는 버림 */
    fun loadRewarded(onLoaded: (Boolean) -> Unit)

    fun cancelRewarded()

    /** 받아 둔 보상 광고를 띄움. 끝까지 보면 바로 [onEarned], 닫히면 [onDone] */
    fun showRewarded(onEarned: () -> Unit, onDone: () -> Unit)

    /** 네이티브 광고를 받음. 받으면 [onLoaded], 들고 있는 수를 넘거나 오래돼 치우면 [onDropped] */
    fun loadNative(key: String, onLoaded: () -> Unit, onDropped: () -> Unit)

    /** 받은 네이티브 광고를 그린 뷰. 받기 전이거나 치웠으면 null */
    fun nativeView(key: String, card: Boolean, style: IosAdStyle): UIView?

    /** 폭에 맞춘 뷰 높이(pt) */
    fun fittingHeight(view: UIView, width: Double): Double
}

/** Swift가 구현하는 광고 동의(UMP) 다리. [IosAdBridge]와 같은 객체 */
interface IosConsentBridge {
    /** 아직 동의 창에 답하지 않았으면 true */
    val consentNeeded: Boolean

    /** 광고를 받을 수 있는지와 동의를 다시 고르는 곳이 필요한지 알림. 등록하면 지금 값부터 */
    fun setConsentListener(listener: (allowed: Boolean, choicesRequired: Boolean) -> Unit)

    /** 동의 정보를 받은 뒤 답할 차례면 창을 받아 둠. 받으면 [onReady], 못 받으면 [onFailed]. 필요 없으면 부르지 않음 */
    fun prepareConsentForm(onReady: () -> Unit, onFailed: () -> Unit)

    /** 받아 둔 동의 창을 띄움. 닫히면 true, 앱이 앞에 없거나 받아 둔 창이 없으면 바로 false */
    fun presentConsentForm(onClosed: (Boolean) -> Unit)

    /** 광고 동의를 다시 고르는 창 */
    fun showPrivacyChoices()
}

/**
 * 앱 토큰 색(ARGB), 글꼴 파일, "광고" 표시. Swift가 Android 카드와 같은 꼴로 그림
 * [page]는 카드 둥근 모서리 바깥. 뷰 뒤 시스템 바탕이 비치지 않게 화면 바탕으로 칠함
 * 글꼴은 앱에 넣은 파일 이름이나 받은 파일 경로. null이면 시스템 글꼴
 */
class IosAdStyle(
    val label: String,
    val title: Long,
    val body: Long,
    val surface: Long,
    val page: Long,
    val corner: Double,
    val action: Long,
    val actionFill: Long,
    val mediaFill: Long,
    val regularFont: String?,
    val mediumFont: String?,
    val semiBoldFont: String?
)

/** iOS 광고 동의. Swift가 동의 정보를 받거나 바꿀 때마다 알려 줌. 창은 [gate]가 띄울 때를 정함 */
class IosAdConsent(private val bridge: IosConsentBridge) : AdPrivacy {
    private val required = MutableStateFlow(false)
    override val choicesRequired: StateFlow<Boolean> = required.asStateFlow()

    private val allowed = MutableStateFlow(false)

    /** 광고를 받을 수 있는지. 동의 창에 답하면 바뀌어 목록 광고 자리가 다시 받음 */
    val adsAllowed: StateFlow<Boolean> = allowed.asStateFlow()

    val gate = ConsentFormGate(needed = { bridge.consentNeeded }, present = bridge::presentConsentForm)

    init {
        bridge.setConsentListener { canRequest, choices ->
            allowed.value = canRequest
            required.value = choices
        }
    }

    internal fun prepare() = bridge.prepareConsentForm(onReady = gate::formReady, onFailed = gate::formFailed)

    override fun showChoices() = bridge.showPrivacyChoices()
}

/** iOS 광고. 전면·보상형은 콜백을 기다리고, 네이티브는 받은 자리를 상태로 알려 화면을 다시 그림 */
class IosAdPlayer(private val bridge: IosAdBridge) : AdPlayer {
    override val available: Boolean get() = bridge.available

    val consent = IosAdConsent(bridge)

    /** 받은 네이티브 자리. Swift가 받거나 치울 때 알려 바뀜 */
    internal val loaded = mutableStateMapOf<String, Boolean>()

    override fun warmUp() {
        if (!available) return
        bridge.warmUp()
        consent.prepare()
    }

    override fun prepareInterstitial() {
        if (available) bridge.prepareInterstitial()
    }

    override suspend fun showInterstitial(onImpression: () -> Unit): Boolean {
        if (!available) return false
        return suspendCancellableCoroutine { result ->
            bridge.showInterstitial(onImpression) { shown -> if (result.isActive) result.resume(shown) }
        }
    }

    override suspend fun loadRewarded(): ReadyReward? {
        if (!available) return null
        // 사용자가 눌러 부르는 자리라 아직 답하지 않았으면 바로 물음
        suspendCancellableCoroutine { result -> consent.gate.askNow { if (result.isActive) result.resume(Unit) } }
        val loaded = suspendCancellableCoroutine { result ->
            result.invokeOnCancellation { bridge.cancelRewarded() }
            bridge.loadRewarded { ok -> if (result.isActive) result.resume(ok) }
        }
        return if (loaded) {
            ReadyReward { onEarned ->
                suspendCancellableCoroutine { result ->
                    bridge.showRewarded(onEarned = onEarned) { if (result.isActive) result.resume(Unit) }
                }
            }
        } else {
            null
        }
    }

    internal fun loadNative(key: String) {
        if (!available || loaded[key] == true) return
        bridge.loadNative(key, onLoaded = { loaded[key] = true }, onDropped = { loaded.remove(key) })
    }

    internal fun nativeView(key: String, card: Boolean, style: IosAdStyle): UIView? = bridge.nativeView(key, card, style)

    internal fun fittingHeight(view: UIView, width: Double): Double = bridge.fittingHeight(view, width)
}
