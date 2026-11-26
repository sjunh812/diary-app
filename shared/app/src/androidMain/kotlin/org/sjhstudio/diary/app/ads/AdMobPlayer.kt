package org.sjhstudio.diary.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.ReadyReward

private const val TAG = "DiaryAds"

/**
 * 들고 있는 네이티브 광고 수의 끝
 * 광고는 메모리를 많이 써 스크롤로 자리가 늘어도 이만큼만 둠
 */
private const val MAX_NATIVES = 5

/** 전면 광고를 받아 두는 시간. 1시간보다 조금 짧게 */
private const val INTERSTITIAL_FRESH_MILLIS = 55 * 60 * 1000L

/** 네이티브 광고를 받을 때의 선택 */
internal fun nativeAdOptions(): NativeAdOptions = NativeAdOptions.Builder()
    // 영상은 소리 없이 시작(`docs/business.md`)
    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
    // 기본 자리는 오른쪽 위라 앱의 ×와 겹침
    // AdChoices는 가리면 안 되어 ×가 없는 오른쪽 아래로 보냄
    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT)
    .build()

/**
 * 매니페스트에 든 광고 단위
 * 개발 빌드는 시험 단위, 출시 빌드에 값이 없으면 광고를 부르지 않음
 */
class AdUnits(context: Context) {
    private val meta: Bundle? =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData

    val native: String = meta?.getString("org.sjhstudio.diary.ad_native").orEmpty()
    val interstitial: String = meta?.getString("org.sjhstudio.diary.ad_interstitial").orEmpty()
    val rewarded: String = meta?.getString("org.sjhstudio.diary.ad_rewarded").orEmpty()
}

/**
 * AdMob 구현
 * 광고 요청에 일기 내용을 넣지 않음(헌법 II)
 */
class AdMobPlayer(context: Context, val units: AdUnits, private val current: CurrentActivity, private val consent: AdConsent) : AdPlayer {
    private val app = context.applicationContext as Application
    private var interstitial: InterstitialAd? = null

    // 받은 전면 광고는 1시간이 지나면 띄우지 않음(Google 안내). 조금 앞서 버리고 새로 받음
    private var interstitialAt = 0L
    private var loadingInterstitial = false
    private val interstitialFresh: () -> Boolean = {
        interstitial != null && SystemClock.elapsedRealtime() - interstitialAt < INTERSTITIAL_FRESH_MILLIS
    }

    // 시작 단계(뒤 스레드)와 화면이 함께 부를 수 있어 한 번만 켜지게
    private val started = AtomicBoolean(false)

    // 초기화가 끝나기 전에 화면 스레드에서 광고를 만들면 SDK 잠금을 기다리다 ANR이 남. 끝난 뒤에만 부름
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var failed = false
    private val waiting = mutableListOf<Pair<() -> Unit, () -> Unit>>()

    override val available: Boolean = units.native.isNotEmpty()

    /**
     * 받아 둔 네이티브 광고
     * 스크롤해 다시 보여도 최근 자리는 다시 요청하지 않음
     */
    val natives = mutableStateMapOf<String, NativeAd>()
    private val loading = mutableSetOf<String>()

    /**
     * [natives]의 받은 순서
     * 오래된 자리부터 내보냄
     */
    private val nativeOrder = ArrayDeque<String>()

    fun loadNative(key: String) {
        if (!available || key in natives || !loading.add(key)) return
        whenReady(askConsent = true, onFailed = { loading -= key }) { requestNative(key) }
    }

    private fun requestNative(key: String) {
        AdLoader.Builder(app, units.native)
            .forNativeAd { ad ->
                loading -= key
                keepNative(key, ad)
            }.withNativeAdOptions(nativeAdOptions())
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading -= key
                        Log.w(TAG, "네이티브 광고를 받지 못함: ${error.code}")
                    }
                }
            ).build()
            .loadAd(AdRequest.Builder().build())
    }

    /** 그려진 목록 광고 자리(지연 목록이 미리 그린 것 포함). 카드가 붙고 떨어질 때 바뀜 */
    internal val onScreen = mutableSetOf<String>()

    /**
     * 새 광고를 두고, 끝을 넘으면 화면에 없는 것 중 가장 오래된 광고를 치움
     * 보이는 카드를 치우면 그 자리가 비어 목록이 튐. 치운 자리가 다시 보이면 새로 받음
     */
    private fun keepNative(key: String, ad: NativeAd) {
        natives[key] = ad
        nativeOrder.addLast(key)
        while (nativeOrder.size > MAX_NATIVES) {
            val oldest = nativeOrder.firstOrNull { it != key && it !in onScreen } ?: break
            nativeOrder.remove(oldest)
            natives.remove(oldest)?.destroy()
        }
    }

    /**
     * 동의를 받을 수 있으면 SDK를 한 번 켬
     * 유료 사용자는 켜지지 않음
     * 초기화는 디스크를 읽어 Google 안내대로 뒤 스레드에서. 등급 설정은 첫 요청보다 먼저라 바로 둠
     */
    @Suppress("TooGenericExceptionCaught")
    private fun start() {
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T).build()
        )
        thread(name = "ads-init") {
            try {
                MobileAds.initialize(app) {
                    main.post {
                        ready = true
                        waiting.forEach { (request, _) -> request() }
                        waiting.clear()
                    }
                }
            } catch (e: Throwable) {
                // 광고 SDK가 실패해도 앱은 그대로. 광고만 부르지 않음. 클래스를 못 찾는 Error까지 받음
                Log.w(TAG, "광고 SDK를 켜지 못함: ${e::class.simpleName}")
                main.post {
                    failed = true
                    waiting.forEach { (_, onFailed) -> onFailed() }
                    waiting.clear()
                }
            }
        }
    }

    /**
     * 화면 스레드에서 부름. 광고를 받을 수 있으면 초기화 뒤에 [request], 아니면 [onFailed]
     * [askConsent]면 아직 답하지 않은 사람에게 동의 창을 띄움. 사용자가 광고를 보는 자리(목록, 보상형)에서만
     */
    private fun whenReady(askConsent: Boolean, onFailed: () -> Unit = {}, request: () -> Unit) {
        if (failed) return onFailed()
        consent.afterUpdate {
            when {
                consent.canRequestAds -> {
                    start()
                    if (ready) request() else waiting += request to onFailed
                }

                askConsent -> consent.showFormIfRequired { if (consent.canRequestAds) whenReady(false, onFailed, request) else onFailed() }

                else -> onFailed()
            }
        }
    }

    override fun warmUp() {
        if (!available) return
        // 앱 시작 단계(뒤 스레드)에서 불러 화면 스레드로 옮김. 지난번 답이 있으면 바로 켬
        main.post { consent.afterUpdate { if (consent.canRequestAds) start() } }
    }

    override fun prepareInterstitial() {
        // 앱 시작 단계(뒤 스레드)에서도 불러, 광고 상태는 화면 스레드에서만 만짐
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { prepareInterstitial() }
            return
        }
        if (!available || loadingInterstitial || interstitialFresh()) return
        interstitial = null
        loadingInterstitial = true
        // 작성 화면을 열 때 받으므로 동의 창은 띄우지 않음
        whenReady(askConsent = false, onFailed = { loadingInterstitial = false }) {
            InterstitialAd.load(
                app,
                units.interstitial,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        interstitialAt = SystemClock.elapsedRealtime()
                        loadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingInterstitial = false
                        Log.w(TAG, "전면 광고를 받지 못함: ${error.code}")
                    }
                }
            )
        }
    }

    // 광고 SDK와 광고 상태는 화면 스레드에서만. 부르는 쪽 스레드와 상관없이 여기서 옮김
    override suspend fun showInterstitial(onImpression: () -> Unit): Boolean = withContext(Dispatchers.Main.immediate) {
        val ad = interstitial?.takeIf { interstitialFresh() } ?: return@withContext false
        val screen = current.value ?: return@withContext false
        interstitial = null
        suspendCancellableCoroutine { result ->
            ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdImpression() = onImpression()

                    override fun onAdDismissedFullScreenContent() {
                        if (result.isActive) result.resume(true)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        if (result.isActive) result.resume(false)
                    }
                }
            ad.show(screen)
        }
    }

    override suspend fun loadRewarded(): ReadyReward? = withContext(Dispatchers.Main.immediate) {
        if (!available) return@withContext null
        // 받는 동안 취소되면 늦게 온 광고는 버려짐(아래 isActive)
        val ad = requestRewardedAd()
        ad?.let {
            ReadyReward { onEarned ->
                withContext(Dispatchers.Main.immediate) { current.value?.let { ad.showUntilClosed(it, onEarned) } }
            }
        }
    }

    private suspend fun requestRewardedAd(): RewardedAd? = suspendCancellableCoroutine { result ->
        // 초기화를 못 하면 기다리지 않고 없음으로
        whenReady(askConsent = true, onFailed = { if (result.isActive) result.resume(null) }) {
            requestRewarded { ad -> if (result.isActive) result.resume(ad) }
        }
    }

    private fun requestRewarded(done: (RewardedAd?) -> Unit) {
        RewardedAd.load(
            app,
            units.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) = done(ad)

                override fun onAdFailedToLoad(error: LoadAdError) = done(null)
            }
        )
    }
}

/** 띄우고 닫힐 때까지 기다림. 끝까지 본 순간 [onEarned]라 닫기 전에 앱이 꺼져도 보상이 남음 */
private suspend fun RewardedAd.showUntilClosed(screen: Activity, onEarned: () -> Unit) = suspendCancellableCoroutine { result ->
    fullScreenContentCallback =
        object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (result.isActive) result.resume(Unit)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (result.isActive) result.resume(Unit)
            }
        }
    show(screen) { onEarned() }
}
