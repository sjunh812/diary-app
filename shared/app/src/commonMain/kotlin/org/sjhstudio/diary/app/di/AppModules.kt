package org.sjhstudio.diary.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.core.data.DefaultSettingsRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.datastore.DataStoreFactory
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.feature.migration.migrationFeatureModule

/** 앱 프로세스와 함께 사는 코루틴 범위. DataStore와 시작 작업이 쓴다. */
internal val APP_SCOPE = named("appScope")

/** 플랫폼마다 다른 것: 파일 자리([AppFiles])와 DB 만들기([DatabaseFactory]). */
internal expect val platformModule: Module

internal fun appModules(isDebug: Boolean): List<Module> = listOf(
    platformModule,
    migrationFeatureModule,
    module {
        single(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
        single { DataStoreFactory(get<AppFiles>().dataStoreDirectory, get(APP_SCOPE)) }
        single { SettingsStore(get<DataStoreFactory>().settings()) }
        single { LockStore(get<DataStoreFactory>().lock()) }
        single<SettingsRepository> { DefaultSettingsRepository(get()) }
        // Firebase 설정 파일이 생기면 플랫폼 구현으로 바꾼다(스펙 001 T028).
        single<TelemetrySink> { NoTelemetrySink }
        single { Telemetry(get(), strict = isDebug) }
        single { Startup(get<AppFiles>()::ensureDirectories, get(), get(), get(APP_SCOPE)) }
        // 처음 쓸 때 연다. 시작 단계에서는 열지 않는다.
        single { get<DatabaseFactory>().create() }
    }
)
