package org.sjhstudio.diary.app

import kotlin.experimental.ExperimentalNativeApi
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.app.di.initKoin
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.core.platform.IosDriveBridge

/**
 * iosApp의 `iOSApp.init`이 부른다(contracts/app-shell.md 시작 순서 1).
 * Koin을 켜고 시작 단계를 돌린다.
 */
@OptIn(ExperimentalNativeApi::class)
fun startApp(driveBridge: GoogleDrive) {
    IosDrive.bridge = driveBridge.asBridge()
    initKoin(isDebug = Platform.isDebugBinary)
    KoinPlatform.getKoin().get<Startup>().start()
}

/** Swift의 GoogleSignIn 구현이 맞추는 모양. 공유 프레임워크에 그대로 나간다(스펙 003 research R2). */
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

/** Swift가 건 구글 드라이브 다리. Koin이 켜지기 전에 받아 둔다. */
internal object IosDrive {
    var bridge: IosDriveBridge? = null
}

/** 앱이 비활성일 때 화면을 덮을지(iosApp `PrivacyCover`, FR-018). 잠금이 켜져 있을 때만 덮는다. */
fun isPrivacyCoverNeeded(): Boolean = KoinPlatform.getKoin().get<LockController>().lockEnabled
