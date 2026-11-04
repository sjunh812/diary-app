package org.sjhstudio.diary.app

import kotlin.experimental.ExperimentalNativeApi
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.app.di.initKoin
import org.sjhstudio.diary.app.lock.LockController

/**
 * iosApp의 `iOSApp.init`이 부른다(contracts/app-shell.md 시작 순서 1).
 * Koin을 켜고 시작 단계를 돌린다.
 */
@OptIn(ExperimentalNativeApi::class)
fun startApp() {
    initKoin(isDebug = Platform.isDebugBinary)
    KoinPlatform.getKoin().get<Startup>().start()
}

/** 앱이 비활성일 때 화면을 덮을지(iosApp `PrivacyCover`, FR-018). 잠금이 켜져 있을 때만 덮는다. */
fun isPrivacyCoverNeeded(): Boolean = KoinPlatform.getKoin().get<LockController>().lockEnabled
