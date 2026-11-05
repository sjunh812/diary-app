package org.sjhstudio.diary.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.data.AdPlayer

private const val TAG = "DiaryAds"

/** 매니페스트에 든 광고 단위. 개발 빌드는 시험 단위이고, 출시 빌드에 값이 없으면 광고를 부르지 않는다. */
class AdUnits(context: Context) {
    private val meta: Bundle? =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData

    val native: String = meta?.getString("org.sjhstudio.diary.ad_native").orEmpty()
    val interstitial: String = meta?.getString("org.sjhstudio.diary.ad_interstitial").orEmpty()
    val rewarded: String = meta?.getString("org.sjhstudio.diary.ad_rewarded").orEmpty()
}

/** AdMob 구현. 광고 요청에 일기 내용을 넣지 않는다(헌법 II). */
class AdMobPlayer(context: Context, val units: AdUnits) : AdPlayer {
    private val app = context.applicationContext as Application
    private var activity: Activity? = null
    private var interstitial: InterstitialAd? = null
    private var started = false

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

    /** 처음 광고를 부를 때 한 번 켠다. 유료 사용자는 켜지지 않는다. */
    fun start() {
        if (started) return
        started = true
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T).build()
        )
        MobileAds.initialize(app)
    }

    override fun prepareInterstitial() {
        if (!available || interstitial != null) return
        start()
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

    override suspend fun showInterstitial(): Boolean {
        val ad = interstitial ?: return false
        val screen = activity ?: return false
        interstitial = null
        return suspendCancellableCoroutine { result ->
            ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
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
        start()
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
        RewardedAd.load(
            app,
            units.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (result.isActive) result.resume(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (result.isActive) result.resume(null)
                }
            }
        )
    }
}
