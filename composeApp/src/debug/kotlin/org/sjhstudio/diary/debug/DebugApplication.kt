package org.sjhstudio.diary.debug

import android.content.Intent
import android.os.StrictMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.BuildConfig
import org.sjhstudio.diary.DiaryApplication
import org.sjhstudio.diary.app.ads.FirebaseRemote
import org.sjhstudio.diary.app.update.PlayStoreUpdates
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.RemoteValues
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.migration.MigrationEvents
import org.sjhstudio.diary.core.platform.DevMenu

/**
 * 개발 빌드의 앱
 * 기록 이전에 강제 종료 훅을 끼우고 요금제를 바꿀 수 있게 함(T049)
 * 출시 빌드에는 없음
 */
class DebugApplication : DiaryApplication() {
    override fun onCreate() {
        // 화면 스레드의 디스크·네트워크 작업과 닫지 않은 자원을 로그(StrictMode 태그)로 잡음. 앱은 끄지 않음
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build()
        )
        super.onCreate()
        // 출시 빌드와 같은 알림(사용 통계)을 감싸서 덮어씀
        val released = get<MigrationEvents>()
        loadKoinModules(
            module {
                single<MigrationEvents> { KillAtHook(File(filesDir, "debug/kill_at"), released) }
                // 개발자 모드에서 고른 요금제를 앱 전체가 따름
                single { DevPlanRepository(this@DebugApplication) }
                single<PlanRepository> { get<DevPlanRepository>() }
                single<PurchaseRepository> { FakePurchases(get()) }
                // 개발자 모드의 새 버전 흉내(스펙 009)
                // 처음 쓸 때 만들어 앱 시작을 늦추지 않음
                single {
                    val play = PlayStoreUpdates(this@DebugApplication, get(), BuildConfig.VERSION_CODE.toString())
                    DevStoreUpdates(this@DebugApplication, play, CoroutineScope(SupervisorJob() + Dispatchers.Main))
                }
                single<StoreUpdates> { get<DevStoreUpdates>() }
                single<RemoteValues> { get<DevStoreUpdates>().remote(get<FirebaseRemote>().values) }
                single<DevMenu> {
                    DevMenu {
                        startActivity(Intent(this@DebugApplication, DevModeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        )
    }
}
