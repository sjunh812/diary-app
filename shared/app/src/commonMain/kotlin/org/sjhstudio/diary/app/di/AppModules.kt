package org.sjhstudio.diary.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.font.ChosenFontDownload
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.maintenance.DiaryCleanup
import org.sjhstudio.diary.core.backup.BackupAlerts
import org.sjhstudio.diary.core.backup.BackupController
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.backup.LegacyBackupImporter
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.backup.MergeApplier
import org.sjhstudio.diary.core.backup.PhotoFetcher
import org.sjhstudio.diary.core.backup.RestoreCatalog
import org.sjhstudio.diary.core.backup.RestoreController
import org.sjhstudio.diary.core.backup.RestorePlanner
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.DefaultSettingsRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.DefaultDraftRepository
import org.sjhstudio.diary.core.data.diary.DefaultEntryRepository
import org.sjhstudio.diary.core.data.diary.DefaultStatsRepository
import org.sjhstudio.diary.core.data.diary.DefaultTrashRepository
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.data.diary.MediaRepository
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.data.photo.DefaultMediaRepository
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.data.reminder.ReminderRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.datastore.DataStoreFactory
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.createDriveApi
import org.sjhstudio.diary.core.migration.LegacyLockBootstrap
import org.sjhstudio.diary.core.migration.LegacySettingsBootstrap
import org.sjhstudio.diary.core.migration.MigrationFollowUp
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.ImageDecoder
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.core.platform.devicePinKeyStore
import org.sjhstudio.diary.feature.backup.backupAlertText
import org.sjhstudio.diary.feature.backup.backupFeatureModule
import org.sjhstudio.diary.feature.backup.backupReadme
import org.sjhstudio.diary.feature.calendar.calendarFeatureModule
import org.sjhstudio.diary.feature.home.homeFeatureModule
import org.sjhstudio.diary.feature.insight.insightFeatureModule
import org.sjhstudio.diary.feature.lock.lockFeatureModule
import org.sjhstudio.diary.feature.migration.migrationFeatureModule
import org.sjhstudio.diary.feature.search.searchFeatureModule
import org.sjhstudio.diary.feature.settings.settingsFeatureModule
import org.sjhstudio.diary.feature.trash.trashFeatureModule
import org.sjhstudio.diary.feature.write.WriteMediaPath
import org.sjhstudio.diary.feature.write.writeFeatureModule

/** 앱 프로세스와 함께 사는 코루틴 범위. DataStore와 시작 작업이 쓴다. */
internal val APP_SCOPE = named("appScope")

/** 플랫폼마다 다른 것: 파일 자리([AppFiles])와 DB 만들기([DatabaseFactory]). */
internal expect val platformModule: Module

internal fun appModules(isDebug: Boolean): List<Module> = listOf(
    platformModule,
    lockFeatureModule,
    migrationFeatureModule,
    settingsFeatureModule,
    writeFeatureModule,
    homeFeatureModule,
    calendarFeatureModule,
    searchFeatureModule,
    insightFeatureModule,
    trashFeatureModule,
    backupFeatureModule,
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
        // 일기 저장소(스펙 002). 일기 DB는 이전 관문을 지난 뒤 화면이 처음 쓸 때 열린다.
        single {
            val files = get<AppFiles>()
            PhotoStore(FileSystem.SYSTEM, files.mediaDirectory.toPath(), ThumbnailEncoder(ImageDecoder::writeThumbnail), ImageDecoder::size)
        }
        // 백업(스펙 003). 드라이브와 일기 DB는 처음 쓸 때 연다.
        single { BackupStore(get<DataStoreFactory>().backup()) }
        single<DriveApi> { createDriveApi(get(), FileSystem.SYSTEM) }
        single {
            val files = get<AppFiles>()
            BackupRunner(
                db = get(),
                drive = get(),
                authorizer = get(),
                store = get(),
                network = get(),
                deviceName = get(),
                fileSystem = FileSystem.SYSTEM,
                mediaDir = files.mediaDirectory.toPath(),
                workDir = files.backupDirectory.toPath(),
                app = get(),
                readme = { backupReadme() },
                settings = {
                    val settings = get<SettingsRepository>().settings.first()
                    ManifestSettings(
                        font = settings.font.id,
                        bodyTextSize = settings.bodyTextSize.id,
                        themeMode = settings.themeMode.id,
                        reminderEnabled = settings.reminderEnabled,
                        reminderMinuteOfDay = settings.reminderMinuteOfDay,
                        listStyle = settings.list.listStyle.id,
                        favoritesOnly = settings.list.favoritesOnly,
                        askLocationAgain = settings.askLocationAgain,
                        lockEnabled = get<LockRepository>().settings.first().enabled
                    )
                },
                alerts = BackupAlerts(get()) { backupAlertText(it) }
            )
        }
        single<BackupRepository> {
            BackupController(get(), lazy { get<DiaryDatabase>() }, lazy { get<BackupRunner>() }, get(), get(), get(), get(APP_SCOPE))
        }
        single<RestoreRepository> {
            val files = get<AppFiles>()
            val work = files.backupDirectory.toPath()
            val media = files.mediaDirectory.toPath()
            val system = FileSystem.SYSTEM
            val applier = lazy { MergeApplier(get(), { system.exists(media / it) }) }
            val legacy = lazy { LegacyBackupImporter(get(), get(), get(), system, work) }
            RestoreController(
                catalog =
                    lazy {
                        RestoreCatalog(get(), system, work) {
                            legacy.value.find(get<SettingsRepository>().settings.first().legacyBackup?.fileId)
                        }
                    },
                planner = lazy { RestorePlanner(get(), applier.value, get(), system, work) },
                applier = applier,
                fetcher = lazy { PhotoFetcher(get(), get(), system, media, work) },
                legacy = legacy,
                db = lazy { get<DiaryDatabase>() },
                store = get(),
                network = get(),
                fileSystem = system,
                applySettings = { applyBackupSettings(get(), it) },
                scope = get(APP_SCOPE)
            )
        }
        single<DraftRepository> { DefaultDraftRepository(get(), get()) }
        single<StatsRepository> { DefaultStatsRepository(get()) }
        single<TrashRepository> { DefaultTrashRepository(get(), get<AppFiles>().mediaDirectory.toPath()) }
        single<MediaRepository> {
            val files = get<AppFiles>()
            DefaultMediaRepository(get(), FileSystem.SYSTEM, files.mediaDirectory.toPath(), files.thumbnailDirectory.toPath())
        }
        single { DiaryCleanup(get(), get()) }
        single<EntryRepository> { DefaultEntryRepository(get(), get<AppFiles>().mediaDirectory.toPath()) }
        single { WriteMediaPath { name -> (get<AppFiles>().mediaDirectory.toPath() / name).toString() } }
        // 이전이 끝난 기기의 후속 작업(FR-011). 일기 DB는 이전이 끝난 뒤에만 꺼낸다.
        single { MigrationFollowUp(get()) { get<DiaryDatabase>() } }
    }
)

/** 잠금과 백업 설정은 바꾸지 않는다(contracts/merge-rules.md "설정"). 알림은 값이 바뀌면 다시 예약된다. */
private suspend fun applyBackupSettings(repository: SettingsRepository, backup: ManifestSettings) {
    DiaryFont.fromId(backup.font)?.let { repository.setFont(it) }
    BodyTextSize.fromId(backup.bodyTextSize)?.let { repository.setBodyTextSize(it) }
    ThemeMode.fromId(backup.themeMode)?.let { repository.setThemeMode(it) }
    backup.listStyle?.let { repository.setListStyle(ListStyle.fromId(it)) }
    backup.favoritesOnly?.let { repository.setFavoritesOnly(it) }
    backup.askLocationAgain?.let { repository.setAskLocationAgain(it) }
    val minute = backup.reminderMinuteOfDay
    val enabled = backup.reminderEnabled
    if (enabled != null && minute != null) repository.setReminder(enabled, minute)
}
