package org.sjhstudio.diary.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.font.ChosenFontDownload
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.core.data.DefaultSettingsRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.data.reminder.ReminderRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.DataStoreFactory
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.migration.LegacyLockBootstrap
import org.sjhstudio.diary.core.migration.LegacySettingsBootstrap
import org.sjhstudio.diary.core.migration.MigrationFollowUp
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.ImageDecoder
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.core.platform.devicePinKeyStore
import org.sjhstudio.diary.feature.lock.lockFeatureModule
import org.sjhstudio.diary.feature.migration.migrationFeatureModule
import org.sjhstudio.diary.feature.settings.settingsFeatureModule

/** 앱 프로세스와 함께 사는 코루틴 범위. DataStore와 시작 작업이 쓴다. */
internal val APP_SCOPE = named("appScope")

/** 플랫폼마다 다른 것: 파일 자리([AppFiles])와 DB 만들기([DatabaseFactory]). */
internal expect val platformModule: Module

internal fun appModules(isDebug: Boolean): List<Module> = listOf(
    platformModule,
    lockFeatureModule,
    migrationFeatureModule,
    settingsFeatureModule,
    module {
        single(APP_SCOPE) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
        single { DataStoreFactory(get<AppFiles>().dataStoreDirectory, get(APP_SCOPE)) }
        single { SettingsStore(get<DataStoreFactory>().settings()) }
        single { LockStore(get<DataStoreFactory>().lock()) }
        single<SettingsRepository> { DefaultSettingsRepository(get()) }
        // 잠금 확인값은 기기 키로 만든다(research R11).
        single<LockRepository> { DefaultLockRepository(get(), devicePinKeyStore()) }
        single { LockController(get(), get(APP_SCOPE)) }
        // 매일 알림(FR-022). 앱을 열 때와 알림 수신기가 다시 예약한다.
        single { ReminderRepository(get(), get()) }
        single { ChosenFontDownload(get(), get(), get(APP_SCOPE)) }
        // Firebase 설정 파일이 생기면 플랫폼 구현으로 바꾼다(스펙 001 T028).
        single<TelemetrySink> { NoTelemetrySink }
        single { Telemetry(get(), strict = isDebug) }
        single {
            // 옛 앱이 없는 iOS에는 잠금·설정 잇기가 없다.
            val legacyLock = getOrNull<LegacyLockBootstrap>()
            val legacySettings = getOrNull<LegacySettingsBootstrap>()
            Startup(
                get<AppFiles>()::ensureDirectories,
                get(),
                get(),
                get(APP_SCOPE),
                bootstrapLegacyLock = { legacyLock?.run() },
                bootstrapLegacySettings = { legacySettings?.run() },
                afterReady = {
                    get<ReminderRepository>().reschedule()
                    // 고른 글꼴이 받는 글꼴인데 없으면 받기 시작한다(이전 직후 포함). 따로 돌아 다른 일을 막지 않는다.
                    get<ChosenFontDownload>().start()
                }
            )
        }
        // 처음 쓸 때 연다. 시작 단계에서는 열지 않는다.
        single { get<DatabaseFactory>().create() }
        // 썸네일(research R9). 사진 칸과 기록 이전이 같이 쓴다.
        single {
            val files = get<AppFiles>()
            ThumbnailRepository(
                FileSystem.SYSTEM,
                files.mediaDirectory.toPath(),
                files.thumbnailDirectory.toPath(),
                ThumbnailEncoder(ImageDecoder::writeThumbnail)
            )
        }
        // 이전이 끝난 기기의 후속 작업(FR-011). 일기 DB는 이전이 끝난 뒤에만 꺼낸다.
        single { MigrationFollowUp(get()) { get<DiaryDatabase>() } }
    }
)
