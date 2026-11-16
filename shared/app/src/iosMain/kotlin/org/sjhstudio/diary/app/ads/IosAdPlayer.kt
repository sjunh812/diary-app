package org.sjhstudio.diary.app.ads

import androidx.compose.runtime.mutableStateMapOf
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.ReadyReward
import platform.UIKit.UIView

/**
 * Swift가 구현하는 구글 광고 다리(GoogleAdsBridge.swift)
 * 결과는 화면 스레드에서 콜백으로 돌려줌
 * 광고 요청에 일기 내용을 넣지 않음(헌법 II)
 */
interface IosAdBridge {
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

    fun loadNative(key: String, onLoaded: () -> Unit)

    /** 받은 네이티브 광고를 그린 뷰. 받기 전이거나 들고 있는 수를 넘어 치웠으면 null */
    fun nativeView(key: String, card: Boolean, style: IosAdStyle): UIView?

    /** 폭에 맞춘 뷰 높이(pt) */
    fun fittingHeight(view: UIView, width: Double): Double
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

/** iOS 광고. 전면·보상형은 콜백을 기다리고, 네이티브는 받은 자리를 상태로 알려 화면을 다시 그림 */
class IosAdPlayer(private val bridge: IosAdBridge) : AdPlayer {
    override val available: Boolean get() = bridge.available

    /** 받은 네이티브 자리 */
    internal val loaded = mutableStateMapOf<String, Boolean>()

    override fun warmUp() {
        if (available) bridge.warmUp()
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
        bridge.loadNative(key) { loaded[key] = true }
    }

    /** 치워진 자리는 다시 받음 */
    internal fun nativeView(key: String, card: Boolean, style: IosAdStyle): UIView? = bridge.nativeView(key, card, style) ?: run {
        loaded.remove(key)
        null
    }

    internal fun fittingHeight(view: UIView, width: Double): Double = bridge.fittingHeight(view, width)
}
