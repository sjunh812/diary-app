package org.sjhstudio.diary.app

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.app.ads.IosAdBridge
import org.sjhstudio.diary.app.di.APP_SCOPE
import org.sjhstudio.diary.app.di.initKoin
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.lock.LockState
import org.sjhstudio.diary.app.navigation.AppLaunch
import org.sjhstudio.diary.app.review.IosStoreReviewBridge
import org.sjhstudio.diary.core.data.AdSwitch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.platform.IosDriveBridge
import org.sjhstudio.diary.core.platform.IosReminderScheduler
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.TelemetrySink

/** iosApp의 `iOSApp.init`에서 Koin을 켜고 시작 단계를 돌림(contracts/app-shell.md 시작 순서 1) */
@OptIn(ExperimentalNativeApi::class)
fun startApp(driveBridge: GoogleDrive, firebase: FirebaseReporting?, ads: IosAdBridge, review: IosStoreReviewBridge) {
    IosDrive.bridge = driveBridge.asBridge()
    IosAds.bridge = ads
    IosReview.bridge = review
    firebase?.let { IosAds.switch = AdSwitch(it::interstitialEnabled) }
    IosTelemetry.sink = firebase?.asSink() ?: NoTelemetrySink
    initKoin(isDebug = Platform.isDebugBinary)
    if (Platform.isDebugBinary) applyDebugPlan()
    val koin = KoinPlatform.getKoin()
    koin.get<Startup>().start()
    BackgroundTime(koin.get()).start(koin.get(APP_SCOPE))
}

/** Swift의 Firebase 구현이 맞추는 모양. 설정 파일이 없는 빌드는 null을 넘김 */
interface FirebaseReporting {
    fun log(event: String, params: Map<String, String>)

    fun setCustomKey(key: String, value: String)

    /**
     * 안전망이 받은 예외를 Crashlytics "처리된 예외"로 남김
     * 메시지는 일기 글이 섞일 수 있어 빼고 종류와 호출 위치만(헌법 II)
     */
    fun recordCaught(kind: String, addresses: List<Long>)

    /** 전면 광고 원격 스위치(Remote Config `interstitial_enabled`). 받지 못하면 마지막 값, 처음이면 켜짐 */
    fun interstitialEnabled(): Boolean
}

internal object IosAds {
    var bridge: IosAdBridge? = null

    /** Firebase 설정 파일 없이 만든 빌드는 늘 켜짐 */
    var switch: AdSwitch = AdSwitch { true }
}

/** Swift 리뷰 창 다리. 앱 시작 때 받음 */
internal object IosReview {
    var bridge: IosStoreReviewBridge? = null
}

internal object IosTelemetry {
    var sink: TelemetrySink = NoTelemetrySink
}

private fun FirebaseReporting.asSink(): TelemetrySink {
    val firebase = this
    return object : TelemetrySink {
        override fun log(event: String, params: Map<String, String>) = firebase.log(event, params)

        override fun setCustomKey(key: String, value: String) = firebase.setCustomKey(key, value)

        @OptIn(ExperimentalNativeApi::class)
        override fun recordCaught(error: Throwable) =
            firebase.recordCaught(error::class.qualifiedName ?: "Exception", error.getStackTraceAddresses())
    }
}

/** Swift의 GoogleSignIn 구현이 맞추는 모양. 공유 프레임워크에 그대로 나감(스펙 003 research R2) */
interface GoogleDrive {
    fun connect(hint: String?, done: (key: String?, email: String?, error: String?) -> Unit)

    fun accessToken(done: (token: String?, key: String?, email: String?, error: String?) -> Unit)

    fun disconnect(done: () -> Unit)
}

private fun GoogleDrive.asBridge(): IosDriveBridge {
    val drive = this
    return object : IosDriveBridge {
        override fun connect(hint: String?, done: (String?, String?, String?) -> Unit) = drive.connect(hint, done)

        override fun accessToken(done: (String?, String?, String?, String?) -> Unit) = drive.accessToken(done)

        override fun disconnect(done: () -> Unit) = drive.disconnect(done)
    }
}

/** Swift가 건 구글 드라이브 다리. Koin이 켜지기 전에 받아 둠 */
internal object IosDrive {
    var bridge: IosDriveBridge? = null
}

/**
 * 앱이 비활성일 때 화면을 덮을지(iosApp `PrivacyCover`, FR-018)
 * 잠금이 켜져 있고 지금 풀려 있을 때만. 잠긴 화면은 일기가 없어 덮지 않음(Face ID 창마다 깜빡임 방지)
 */
fun isPrivacyCoverNeeded(): Boolean = KoinPlatform.getKoin().get<LockController>().let {
    it.lockEnabled &&
        it.state.value == LockState.Unlocked
}

/**
 * 사용자가 알림을 누름
 * 매일 알림이면 쓰기 화면 열기. 잠겨 있으면 잠금을 푼 뒤에 열림
 */
fun onNotificationTapped(identifier: String) {
    if (identifier == IosReminderScheduler.REQUEST_ID) KoinPlatform.getKoin().get<AppLaunch>().requestWrite()
}

/** 시스템의 남은 시간이 끝날 때 Swift가 멈출 수 있게 돌려줌 */
class BackupTaskHandle internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

/**
 * 백그라운드 작업용. 때가 됐으면 백업하고, 끝나거나 멈추면 [done]을 부름(research R15)
 * 사용자가 누른 백업·복원의 시간은 [BackgroundTime]이 받아 둠
 */
fun runDailyBackup(done: () -> Unit): BackupTaskHandle {
    val koin = KoinPlatform.getKoin()
    val job =
        koin.get<CoroutineScope>(APP_SCOPE).launch {
            koin.get<BackupRepository>().runIfDue()
        }
    job.invokeOnCompletion { done() }
    return BackupTaskHandle(job)
}
