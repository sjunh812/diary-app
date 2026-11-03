package org.sjhstudio.diary.app.di

import android.content.Context
import android.os.Build
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.AndroidImageSizeReader
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyLockBootstrap
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.LegacySettingsBootstrap
import org.sjhstudio.diary.core.migration.MigrationEvents
import org.sjhstudio.diary.core.migration.MigrationPaths
import org.sjhstudio.diary.core.migration.MigrationRunner
import org.sjhstudio.diary.core.migration.MigrationTelemetry
import org.sjhstudio.diary.core.migration.OriginalExporter
import org.sjhstudio.diary.core.migration.SnapshotLegacyArchive
import org.sjhstudio.diary.core.migration.SupportBundle
import org.sjhstudio.diary.core.migration.SupportFiles
import org.sjhstudio.diary.core.migration.freeBytes
import org.sjhstudio.diary.core.migration.legacyFiles
import org.sjhstudio.diary.core.platform.AndroidNotificationPermission
import org.sjhstudio.diary.core.platform.AndroidReminderScheduler
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.ReminderScheduler

@OptIn(ExperimentalUuidApi::class)
internal actual val platformModule: Module =
    module {
        single { AppFiles(androidContext()) }
        single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
        single<NotificationPermission> { AndroidNotificationPermission(androidContext()) }
        single { DatabaseFactory(androidContext(), get()) }
        single {
            val files = get<AppFiles>()
            MigrationPaths(
                legacy = legacyFiles(androidContext()),
                snapshotDir = requireNotNull(files.legacySnapshotDirectory).toPath(),
                migrationDir = requireNotNull(files.migrationDirectory).toPath(),
                mediaDir = files.mediaDirectory.toPath(),
                databasePath = files.databasePath.toPath()
            )
        }
        single<LegacyArchive> { SnapshotLegacyArchive(FileSystem.SYSTEM, get(), Clock.System, TimeZone.currentSystemDefault()) }
        single { OriginalExporter(FileSystem.SYSTEM, get()) }
        // 고쳐 받기 문의 묶음(FR-010a). 지우는 때는 SupportFiles가 정한다.
        single {
            SupportBundle(FileSystem.SYSTEM, get(), SupportFiles(FileSystem.SYSTEM, get<MigrationPaths>().migrationDir, Clock.System))
        }
        // 이전 단계 알림. 출시 빌드는 사용 통계만, 개발 빌드는 강제 종료 훅이 이 자리를 덮어쓴다(quickstart §3).
        single<MigrationEvents> { MigrationTelemetry(get()) }
        // 1.7.x 잠금과 설정을 이전보다 먼저 잇는다(research R16). 이전을 시작한 뒤에는 하지 않는다.
        single {
            LegacyLockBootstrap(FileSystem.SYSTEM, get<MigrationPaths>().legacy, { get<LegacyMigration>().state().step }, get(), get())
        }
        single { LegacySettingsBootstrap(FileSystem.SYSTEM, get<MigrationPaths>().legacy, { get<LegacyMigration>().state().step }, get()) }
        // 1.7.x 원본이 같은 패키지에 남아 있으면 옮긴다(docs/migration.md).
        single<LegacyMigration> {
            val files = get<AppFiles>()
            val factory = get<DatabaseFactory>()
            MigrationRunner(
                fileSystem = FileSystem.SYSTEM,
                paths = get(),
                clock = Clock.System,
                timeZone = TimeZone.currentSystemDefault(),
                newId = { Uuid.random().toString() },
                freeBytes = ::freeBytes,
                openDatabase = factory::createAt,
                imageSize = AndroidImageSizeReader,
                events = get(),
                versionCode = versionCode(androidContext()),
                makeThumbnail = get<ThumbnailRepository>()::ensure
            )
        }
    }

/** 설치된 앱의 versionCode. 업데이트 뒤 다시 읽기(FR-011)가 버전을 견준다. */
private fun versionCode(context: Context): Int {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        info.versionCode
    }
}
