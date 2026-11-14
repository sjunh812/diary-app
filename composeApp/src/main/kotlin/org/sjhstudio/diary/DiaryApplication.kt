package org.sjhstudio.diary

import android.app.Application
import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.di.initKoin

// 개발 빌드는 DebugApplication이 이어받아 시험용 훅을 끼움(src/debug)
open class DiaryApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // 받은 글꼴 모듈(fonts/*)의 자산을 앱을 다시 켜지 않고 읽음
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        initKoin(isDebug = BuildConfig.DEBUG) { androidContext(this@DiaryApplication) }
        // 첫 화면 전에 설정과 잠금을 읽음(contracts/app-shell.md 시작 순서 1)
        get<Startup>().start()
    }
}
