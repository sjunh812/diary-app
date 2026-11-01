package org.sjhstudio.diary

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.di.initKoin

class DiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(isDebug = BuildConfig.DEBUG) { androidContext(this@DiaryApplication) }
        // 첫 화면 전에 설정과 잠금을 읽는다(contracts/app-shell.md 시작 순서 1).
        get<Startup>().start()
    }
}
