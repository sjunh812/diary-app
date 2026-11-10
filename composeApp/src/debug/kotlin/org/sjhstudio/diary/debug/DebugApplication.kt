package org.sjhstudio.diary.debug

import android.content.Intent
import java.io.File
import org.koin.android.ext.android.get
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.DiaryApplication
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.migration.MigrationEvents
import org.sjhstudio.diary.core.platform.DevMenu

/**
 * 개발 빌드의 앱.
 * 기록 이전에 강제 종료 훅을 끼우고 요금제를 바꿀 수 있게 함(T049).
 * 출시 빌드에는 없음.
 */
class DebugApplication : DiaryApplication() {
    override fun onCreate() {
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
                single<DevMenu> {
                    DevMenu {
                        startActivity(Intent(this@DebugApplication, DevModeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        )
    }
}
