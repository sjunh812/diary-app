package org.sjhstudio.diary.debug

import java.io.File
import org.koin.android.ext.android.get
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.DiaryApplication
import org.sjhstudio.diary.core.migration.MigrationEvents

/** 개발 빌드의 앱. 기록 이전에 강제 종료 훅을 끼운다(T049). 출시 빌드에는 없다. */
class DebugApplication : DiaryApplication() {
    override fun onCreate() {
        super.onCreate()
        // 출시 빌드와 같은 알림(사용 통계)을 감싸서 덮어쓴다.
        val released = get<MigrationEvents>()
        loadKoinModules(
            module {
                single<MigrationEvents> { KillAtHook(File(filesDir, "debug/kill_at"), released) }
            }
        )
    }
}
