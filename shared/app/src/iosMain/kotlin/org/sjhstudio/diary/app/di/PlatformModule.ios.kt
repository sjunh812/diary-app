package org.sjhstudio.diary.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.app.IosDrive
import org.sjhstudio.diary.app.IosTelemetry
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.backup.LegacyPhotoCopier
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.AdSwitch
import org.sjhstudio.diary.core.data.FreePlanRepository
import org.sjhstudio.diary.core.data.NoAdPlayer
import org.sjhstudio.diary.core.data.NoPurchases
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.EmptyLegacyArchive
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.platform.IosBackupNotifier
import org.sjhstudio.diary.core.platform.IosDeviceName
import org.sjhstudio.diary.core.platform.IosDriveAuthorizer
import org.sjhstudio.diary.core.platform.IosFontInstaller
import org.sjhstudio.diary.core.platform.IosFreeSpace
import org.sjhstudio.diary.core.platform.IosNetworkState
import org.sjhstudio.diary.core.platform.IosNotificationPermission
import org.sjhstudio.diary.core.platform.IosPlaceFinder
import org.sjhstudio.diary.core.platform.IosReminderScheduler
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.NoBackupScheduler
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.PlaceFinder
import org.sjhstudio.diary.core.platform.ReminderScheduler
import org.sjhstudio.diary.core.platform.TelemetrySink
import platform.Foundation.NSBundle

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        single<AdPlayer> { NoAdPlayer }
        // iOS는 R2에서 구매와 광고를 붙인다.
        single<AdSwitch> { AdSwitch { true } }
        single<PlanRepository> { FreePlanRepository() }
        single<PurchaseRepository> { NoPurchases() }
        // 날마다 되풀이하는 달력 알림 하나(스펙 001 T070, research R15).
        single<TelemetrySink> { IosTelemetry.sink }
        single<ReminderScheduler> { IosReminderScheduler() }
        single<NotificationPermission> { IosNotificationPermission() }
        single<PlaceFinder> { IosPlaceFinder() }
        single<FontInstaller> { IosFontInstaller(get()) }
        single { DatabaseFactory(get()) }
        single<DriveAuthorizer> { IosDriveAuthorizer { IosDrive.bridge } }
        single<NetworkState> { IosNetworkState() }
        single<DeviceName> { IosDeviceName() }
        single<BackupScheduler> { NoBackupScheduler }
        single<BackupNotifier> { IosBackupNotifier() }
        single<FreeSpace> { IosFreeSpace() }
        // iOS에는 레거시 사진 경로가 없어 예전 백업의 사진은 찾을 수 없는 사진으로 남는다(FR-030).
        single { LegacyPhotoCopier { _, _, _ -> null } }
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
