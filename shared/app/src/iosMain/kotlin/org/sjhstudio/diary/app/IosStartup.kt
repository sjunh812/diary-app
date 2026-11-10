package org.sjhstudio.diary.app

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.app.di.APP_SCOPE
import org.sjhstudio.diary.app.di.initKoin
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.navigation.AppLaunch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.platform.IosDriveBridge
import org.sjhstudio.diary.core.platform.IosReminderScheduler
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.TelemetrySink

/** iosApp의 `iOSApp.init`에서 Koin을 켜고 시작 단계를 돌림(contracts/app-shell.md 시작 순서 1) */
@OptIn(ExperimentalNativeApi::class)
fun startApp(driveBridge: GoogleDrive, firebase: FirebaseReporting?) {
    IosDrive.bridge = driveBridge.asBridge()
    IosTelemetry.sink = firebase?.asSink() ?: NoTelemetrySink
    initKoin(isDebug = Platform.isDebugBinary)
    if (Platform.isDebugBinary) applyDebugPlan()
    KoinPlatform.getKoin().get<Startup>().start()
}

/** Swift의 Firebase 구현이 맞추는 모양. 설정 파일이 없는 빌드는 null을 넘김 */
interface FirebaseReporting {
    fun log(event: String, params: Map<String, String>)

    fun setCustomKey(key: String, value: String)
}

internal object IosTelemetry {
    var sink: TelemetrySink = NoTelemetrySink
}

private fun FirebaseReporting.asSink(): TelemetrySink {
    val firebase = this
    return object : TelemetrySink {
        override fun log(event: String, params: Map<String, String>) = firebase.log(event, params)

        override fun setCustomKey(key: String, value: String) = firebase.setCustomKey(key, value)
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

/** 앱이 비활성일 때 화면을 덮을지(iosApp `PrivacyCover`, FR-018). 잠금이 켜져 있을 때만 덮음 */
fun isPrivacyCoverNeeded(): Boolean = KoinPlatform.getKoin().get<LockController>().lockEnabled

/**
 * 사용자가 알림을 누름.
 * 매일 알림이면 쓰기 화면을 엶. 잠겨 있으면 잠금을 푼 뒤에 열림
 */
fun onNotificationTapped(identifier: String) {
    if (identifier == IosReminderScheduler.REQUEST_ID) KoinPlatform.getKoin().get<AppLaunch>().requestWrite()
}

/** 시스템의 남은 시간이 끝날 때 Swift가 멈출 수 있게 돌려줌 */
class BackupTaskHandle internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

/** 백그라운드 작업용. 때가 됐으면 백업하고, 끝나거나 멈추면 [done]을 부름(research R15) */
fun runDailyBackup(done: () -> Unit): BackupTaskHandle {
    val koin = KoinPlatform.getKoin()
    val job =
        koin.get<CoroutineScope>(APP_SCOPE).launch {
            val backup = koin.get<BackupRepository>()
            backup.runIfDue()
            // 사용자가 누른 백업이 도는 중이면 시스템이 준 시간을 끝까지 씀
            backup.status.first { it.running == null }
        }
    job.invokeOnCompletion { done() }
    return BackupTaskHandle(job)
}
