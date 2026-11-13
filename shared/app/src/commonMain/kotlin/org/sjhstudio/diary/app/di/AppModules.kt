package org.sjhstudio.diary.app.di

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
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
import org.sjhstudio.diary.app.StartupWork
import org.sjhstudio.diary.app.backup.BackupSettingsBridge
import org.sjhstudio.diary.app.font.ChosenFontDownload
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.maintenance.DiaryCleanup
import org.sjhstudio.diary.app.navigation.AppLaunch
import org.sjhstudio.diary.app.scaffold.appShellModule
import org.sjhstudio.diary.core.backup.AutoBackupNotifier
import org.sjhstudio.diary.core.backup.BackupAlerts
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.backup.BackupController
import org.sjhstudio.diary.core.backup.BackupGate
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.backup.BundleExporter
import org.sjhstudio.diary.core.backup.LegacyBackupImporter
import org.sjhstudio.diary.core.backup.LegacyPhotoCopier
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.backup.MergeApplier
import org.sjhstudio.diary.core.backup.PhotoFetcher
import org.sjhstudio.diary.core.backup.RestoreCatalog
import org.sjhstudio.diary.core.backup.RestoreController
import org.sjhstudio.diary.core.backup.RestorePlanner
import org.sjhstudio.diary.core.backup.RollbackManager
import org.sjhstudio.diary.core.backup.backupToOffer
import org.sjhstudio.diary.core.backup.offerIfNew
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.DefaultSettingsRepository
import org.sjhstudio.diary.core.data.DefaultWelcomeRepository
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.WelcomeRepository
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
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.datastore.DataStoreFactory
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.createDriveApi
import org.sjhstudio.diary.core.migration.LegacyLockBootstrap
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.LegacySettingsBootstrap
import org.sjhstudio.diary.core.migration.MigrationFollowUp
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.platform.ImageDecoder
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.devicePinKeyStore
import org.sjhstudio.diary.core.ui.FontWarmup
import org.sjhstudio.diary.core.ui.SafetyNet
import org.sjhstudio.diary.feature.backup.backupAlertText
import org.sjhstudio.diary.feature.backup.backupFeatureModule
import org.sjhstudio.diary.feature.backup.backupFolderText
import org.sjhstudio.diary.feature.backup.backupReadme
import org.sjhstudio.diary.feature.calendar.calendarFeatureModule
import org.sjhstudio.diary.feature.home.homeFeatureModule
import org.sjhstudio.diary.feature.insight.insightFeatureModule
import org.sjhstudio.diary.feature.lock.lockFeatureModule
import org.sjhstudio.diary.feature.migration.migrationFeatureModule
import org.sjhstudio.diary.feature.plus.plusFeatureModule
import org.sjhstudio.diary.feature.search.searchFeatureModule
import org.sjhstudio.diary.feature.settings.settingsFeatureModule
import org.sjhstudio.diary.feature.trash.trashFeatureModule
import org.sjhstudio.diary.feature.welcome.welcomeFeatureModule
import org.sjhstudio.diary.feature.write.WriteMediaPath
import org.sjhstudio.diary.feature.write.writeFeatureModule

/**
 * 앱 프로세스와 함께 사는 코루틴 범위
 * DataStore와 시작 작업이 씀
 */
internal val APP_SCOPE = named("appScope")

/**
 * 앱 범위의 일 하나가 실패해도 앱이 꺼지지 않음
 * 예외 메시지에는 일기 내용이 섞일 수 있어 [report]에는 종류만 넘김
 */
internal fun appCoroutineScope(
    context: CoroutineContext = Dispatchers.IO,
    report: (kind: String) -> Unit = { println("앱 범위 작업 실패: $it") }
): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, error -> report(error::class.simpleName.orEmpty()) }
    return CoroutineScope(SupervisorJob() + context + handler)
}

/** 플랫폼마다 다른 것: 파일 자리([AppFiles])와 DB 만들기([DatabaseFactory]) */
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
    welcomeFeatureModule,
    plusFeatureModule,
    appShellModule,
    coreModule(isDebug),
    diaryDataModule,
    backupModule
)

/** 시작, 설정, 잠금, 알림, 통계, 광고 */
private fun coreModule(isDebug: Boolean) = module {
    single(APP_SCOPE) { appCoroutineScope() }
    single { DataStoreFactory(get<AppFiles>().dataStoreDirectory, get(APP_SCOPE)) }
    single { SettingsStore(get<DataStoreFactory>().settings()) }
    single { LockStore(get<DataStoreFactory>().lock()) }
    single<SettingsRepository> { DefaultSettingsRepository(get()) }
    // 잠금 확인값은 기기 키로 만듦(research R11)
    single<LockRepository> { DefaultLockRepository(get(), devicePinKeyStore()) }
    single { LockController(get(), get(APP_SCOPE)) }
    // 매일 알림(FR-022)
    // 앱을 열 때와 알림 수신기가 다시 예약
    single { ReminderRepository(get(), get()) }
    single { ChosenFontDownload(get(), get(), get(APP_SCOPE)) }
    single { AppLaunch() }
    // Firebase 설정 파일이 생기면 플랫폼 구현으로 바꿈(스펙 001 T028)
    // 앱을 열 때 만들어 화면 모델 안전망이 받은 예외를 처음부터 보고함
    single(createdAtStart = true) { Telemetry(get(), strict = isDebug).also { telemetry -> SafetyNet.report = telemetry::caught } }
    single {
        // 옛 앱이 없는 iOS에는 잠금·설정 잇기 없음
        val legacyLock = getOrNull<LegacyLockBootstrap>()
        val legacySettings = getOrNull<LegacySettingsBootstrap>()
        Startup(
            get<AppFiles>()::ensureDirectories,
            get(),
            get(),
            get(APP_SCOPE),
            bootstrapLegacyLock = { legacyLock?.run() },
            bootstrapLegacySettings = { legacySettings?.run() },
            platformWork = { getOrNull<StartupWork>()?.run() },
            knowFont = { getOrNull<FontInstaller>()?.installedPath(it.font.id) },
            warmFont = { getOrNull<FontWarmup>()?.warm(it.font) },
            afterReady = {
                get<ReminderRepository>().reschedule()
                // 고른 글꼴이 받는 글꼴인데 없으면 받기 시작(이전 직후 포함)
                // 따로 돌아 다른 일을 막지 않음
                get<ChosenFontDownload>().start()
                // 글꼴 설정 화면이 받아 둔 글꼴을 처음부터 맞게 그리게 파일 확인을 미리 해 둠
                getOrNull<FontInstaller>()?.let { installer -> DiaryFont.entries.forEach { installer.installedPath(it.id) } }
                get<AdRepository>().warmUp()
            }
        )
    }
    single<WelcomeRepository> {
        DefaultWelcomeRepository(WelcomeStore(get<DataStoreFactory>().welcome())) { get<LegacyMigration>().report()?.entriesMigrated }
    }
    single<ContextTips> { get<WelcomeRepository>() }
    single { AdStore(get<DataStoreFactory>().ads()) }
    single { AdRepository(get(), get(), get(), get()) }
}

/** 일기 DB와 일기·사진·임시 저장·휴지통 저장소. DB는 이전 관문을 지난 뒤 처음 쓸 때 열림 */
private val diaryDataModule =
    module {
        // 처음 쓸 때 열기
        // 시작 단계에서는 열지 않음
        single { get<DatabaseFactory>().create() }
        // 썸네일(research R9)
        // 사진 칸과 기록 이전이 같이 씀
        single {
            val files = get<AppFiles>()
            ThumbnailRepository(
                FileSystem.SYSTEM,
                files.mediaDirectory.toPath(),
                files.thumbnailDirectory.toPath(),
                ThumbnailEncoder(ImageDecoder::writeThumbnail)
            )
        }
        // 일기 저장소(스펙 002)
        // 일기 DB는 이전 관문을 지난 뒤 화면이 처음 쓸 때 열림
        single {
            val files = get<AppFiles>()
            PhotoStore(FileSystem.SYSTEM, files.mediaDirectory.toPath(), ThumbnailEncoder(ImageDecoder::writeThumbnail), ImageDecoder::size)
        }
        // 개발 빌드가 요금제를 바꿔 끼울 수 있게 부를 때마다 찾음
        single<DraftRepository> {
            val koin = getKoin()
            DefaultDraftRepository(get(), get(), photoLimit = { koin.get<PlanRepository>().awaitConfirmed().photoLimit })
        }
        single<StatsRepository> { DefaultStatsRepository(get()) }
        single<TrashRepository> { DefaultTrashRepository(get(), get<AppFiles>().mediaDirectory.toPath()) }
        single<MediaRepository> {
            val files = get<AppFiles>()
            DefaultMediaRepository(
                get(),
                FileSystem.SYSTEM,
                files.mediaDirectory.toPath(),
                files.thumbnailDirectory.toPath(),
                // 되돌릴 수 있는 동안에는 바꾸기 전 일기가 가리키던 사진을 지우지 않음
                keepEverything = { get<BackupStore>().current().rollbackUntil != null }
            )
        }
        single { DiaryCleanup(get(), get(), keepPhotos = { get<LegacyMigration>().keepsPhotoCopies() }) }
        single<EntryRepository> { DefaultEntryRepository(get(), get<AppFiles>().mediaDirectory.toPath()) }
        single { WriteMediaPath { name -> (get<AppFiles>().mediaDirectory.toPath() / name).toString() } }
        // 이전이 끝난 기기의 후속 작업(FR-011)
        // 일기 DB는 이전이 끝난 뒤에만 꺼냄
        single { MigrationFollowUp(get()) { get<DiaryDatabase>() } }
    }

/** 백업·복원(스펙 003). 드라이브와 일기 DB는 처음 쓸 때 열림 */
private val backupModule =
    module {
        // 백업에 담는 설정과 복원할 때 적용할 설정(contracts/merge-rules.md "설정")
        single {
            BackupSettingsBridge(
                settings = get(),
                lockEnabled = { get<LockRepository>().settings.first().enabled },
                reschedule = { get<ReminderRepository>().reschedule() }
            )
        }
        // 백업(스펙 003)
        // 드라이브와 일기 DB는 처음 쓸 때 열기
        single { BackupStore(get<DataStoreFactory>().backup()) }
        single<DriveApi> { createDriveApi(get(), FileSystem.SYSTEM) }
        single { BackupGate() }
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
                settings = { get<BackupSettingsBridge>().current() },
                alerts = get(),
                gate = get(),
                dataReady = { !get<LegacyMigration>().needsMigration() }
            )
        }
        single { BackupAlerts(AutoBackupNotifier(get(), get()), { backupFolderText() }) { backupAlertText(it) } }
        single<BackupRepository> { get<BackupController>() }
        single {
            BackupController(
                store = get(),
                db = lazy { get<DiaryDatabase>() },
                runner = lazy { get<BackupRunner>() },
                authorizer = get(),
                scheduler = get(),
                notifier = get(),
                scope = get(APP_SCOPE),
                migrated = { get<LegacyMigration>().report() != null },
                legacyAccount = { get<SettingsRepository>().settings.first().legacyBackup?.account },
                findBackup = {
                    val restore = get<RestoreRepository>()
                    val latest = backupToOffer(restore.backups(), get<LegacyMigration>().report() != null)
                    // 권하지 않으면 확인하려고 받은 사본을 지움
                    offerIfNew(latest) { restore.prepare(it) }.also { if (it == null && latest != null) restore.dismiss() }
                },
                alerts = get()
            )
        }
        single<RestoreRepository> {
            val files = get<AppFiles>()
            val work = files.backupDirectory.toPath()
            val media = files.mediaDirectory.toPath()
            val system = FileSystem.SYSTEM
            // DB만 늦게 열기
            // 나머지는 여기서 꺼내 빠진 부품이 앱을 켤 때 바로 드러나게 함
            val drive = get<DriveApi>()
            val copier = get<LegacyPhotoCopier>()
            val freeSpace = get<FreeSpace>()
            val settings = get<SettingsRepository>()
            val store = get<BackupStore>()
            val deviceName = get<DeviceName>()
            val appInfo = get<BackupAppInfo>()
            val database = lazy { get<DiaryDatabase>() }
            val applier = lazy { MergeApplier(database.value, { system.exists(media / it) }) }
            val legacy = lazy { LegacyBackupImporter(database.value, drive, copier, system, work) }
            val bridge = get<BackupSettingsBridge>()
            val applyBackup: suspend (ManifestSettings) -> Unit = bridge::apply
            RestoreController(
                catalog =
                    lazy {
                        RestoreCatalog(get(), system, work) {
                            legacy.value.find(settings.settings.first().legacyBackup?.fileId)
                        }
                    },
                planner = lazy { RestorePlanner(drive, applier.value, freeSpace, system, work, store::deviceId) },
                applier = applier,
                fetcher = lazy { PhotoFetcher(database.value, drive, system, media, work, freeSpace) },
                legacy = legacy,
                exporter =
                    lazy {
                        BundleExporter(
                            db = database.value,
                            store = store,
                            deviceName = deviceName,
                            fileSystem = system,
                            mediaDir = media,
                            workDir = work,
                            app = appInfo,
                            readme = { backupReadme() },
                            settings = { bridge.current() }
                        )
                    },
                rollbackManager =
                    lazy {
                        RollbackManager(
                            db = database.value,
                            applier = applier.value,
                            store = store,
                            freeSpace = freeSpace,
                            fileSystem = system,
                            rootDir = work,
                            currentSettings = { bridge.current() },
                            applySettings = applyBackup
                        )
                    },
                scheduler = get(),
                freeSpace = freeSpace,
                db = database,
                store = store,
                network = get(),
                fileSystem = system,
                workDir = work,
                applySettings = applyBackup,
                scope = get(APP_SCOPE),
                gate = get()
            )
        }
    }
