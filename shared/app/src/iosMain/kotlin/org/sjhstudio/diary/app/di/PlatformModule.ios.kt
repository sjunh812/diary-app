package org.sjhstudio.diary.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.app.IosDrive
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.EmptyLegacyArchive
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.IosDeviceName
import org.sjhstudio.diary.core.platform.IosDriveAuthorizer
import org.sjhstudio.diary.core.platform.IosFontInstaller
import org.sjhstudio.diary.core.platform.IosNetworkState
import org.sjhstudio.diary.core.platform.IosNotificationPermission
import org.sjhstudio.diary.core.platform.IosReminderScheduler
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.NoBackupScheduler
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.ReminderScheduler
import platform.Foundation.NSBundle

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        // 날마다 되풀이하는 달력 알림 하나(스펙 001 T070, research R15).
        single<ReminderScheduler> { IosReminderScheduler() }
        single<NotificationPermission> { IosNotificationPermission() }
        single<FontInstaller> { IosFontInstaller(get()) }
        single { DatabaseFactory(get()) }
        single<DriveAuthorizer> { IosDriveAuthorizer { IosDrive.bridge } }
        single<NetworkState> { IosNetworkState() }
        single<DeviceName> { IosDeviceName() }
        single<BackupScheduler> { NoBackupScheduler }
        single {
            val info = NSBundle.mainBundle.infoDictionary
            BackupAppInfo(
                info?.get("CFBundleShortVersionString") as? String ?: "",
                (info?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0,
                "ios"
            )
        }
        // iOS에는 옮길 옛 앱이 없다(contracts/migration-flow.md).
        single<LegacyMigration> { NoLegacyMigration }
        single<LegacyArchive> { EmptyLegacyArchive }
    }
