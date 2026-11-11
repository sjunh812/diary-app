package org.sjhstudio.diary.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.AdPlayer

private const val TAG = "DiaryAds"

/**
 * 들고 있는 네이티브 광고 수의 끝
 * 광고는 메모리를 많이 써 스크롤로 자리가 늘어도 이만큼만 둠
 */
private const val MAX_NATIVES = 5

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
class AdMobPlayer(context: Context, val units: AdUnits) : AdPlayer {
    private val app = context.applicationContext as Application
    private var activity: Activity? = null
    private var interstitial: InterstitialAd? = null

    // 시작 단계(뒤 스레드)와 화면이 함께 부를 수 있어 한 번만 켜지게
    private val started = AtomicBoolean(false)

    // 초기화가 끝나기 전에 화면 스레드에서 광고를 만들면 SDK 잠금을 기다리다 ANR이 남. 끝난 뒤에만 부름
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var failed = false
    private val waiting = mutableListOf<Pair<() -> Unit, () -> Unit>>()

    init {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    this@AdMobPlayer.activity = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (this@AdMobPlayer.activity === activity) this@AdMobPlayer.activity = null
                }

                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

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
        whenReady { requestNative(key) }
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

    /**
     * 새 광고를 두고, 끝을 넘으면 가장 오래된 광고를 치움
     * 치운 자리가 다시 보이면 새로 받음
     */
    private fun keepNative(key: String, ad: NativeAd) {
        natives[key] = ad
        nativeOrder.addLast(key)
        while (nativeOrder.size > MAX_NATIVES) natives.remove(nativeOrder.removeFirst())?.destroy()
    }

    /**
     * 처음 광고를 부를 때 한 번 켬
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
            } catch (e: Exception) {
                // 광고 SDK가 실패해도 앱은 그대로. 광고만 부르지 않음
                Log.w(TAG, "광고 SDK를 켜지 못함: ${e::class.simpleName}")
                main.post {
                    failed = true
                    waiting.forEach { (_, onFailed) -> onFailed() }
                    waiting.clear()
                }
            }
        }
    }

    /** 화면 스레드에서 부름. 초기화가 끝났으면 바로, 아니면 끝난 뒤에. 초기화를 못 하면 [onFailed] */
    private fun whenReady(onFailed: () -> Unit = {}, request: () -> Unit) {
        if (failed) return onFailed()
        start()
        if (ready) request() else waiting += request to onFailed
    }

    override fun warmUp() {
        if (available) start()
    }

    override fun prepareInterstitial() {
        // 앱 시작 단계(뒤 스레드)에서도 불러, 광고 상태는 화면 스레드에서만 만짐
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { prepareInterstitial() }
            return
        }
        if (!available || interstitial != null) return
        whenReady {
            if (interstitial != null) return@whenReady
            InterstitialAd.load(
                app,
                units.interstitial,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "전면 광고를 받지 못함: ${error.code}")
                    }
                }
            )
        }
    }

    override suspend fun showInterstitial(onImpression: () -> Unit): Boolean {
        val ad = interstitial ?: return false
        val screen = activity ?: return false
        interstitial = null
        return suspendCancellableCoroutine { result ->
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

    override suspend fun showRewarded(): Boolean {
        if (!available) return false
        val ad = loadRewarded() ?: return false
        val screen = activity ?: return false
        return suspendCancellableCoroutine { result ->
            var earned = false
            ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        if (result.isActive) result.resume(earned)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        if (result.isActive) result.resume(false)
                    }
                }
            ad.show(screen) { earned = true }
        }
    }

    private suspend fun loadRewarded(): RewardedAd? = suspendCancellableCoroutine { result ->
        // 초기화를 못 하면 기다리지 않고 없음으로
        whenReady(onFailed = { if (result.isActive) result.resume(null) }) {
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
