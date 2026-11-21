package org.sjhstudio.diary.app.di

import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.app.IosAds
import org.sjhstudio.diary.app.IosDrive
import org.sjhstudio.diary.app.IosReview
import org.sjhstudio.diary.app.IosTelemetry
import org.sjhstudio.diary.app.IosUpdate
import org.sjhstudio.diary.app.StartupWork
import org.sjhstudio.diary.app.ads.IosAdPlayer
import org.sjhstudio.diary.app.billing.IosBilling
import org.sjhstudio.diary.app.billing.iosBilling
import org.sjhstudio.diary.app.review.IosStoreReview
import org.sjhstudio.diary.app.update.IosStoreUpdates
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.backup.LegacyPhotoCopier
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.AdSwitch
import org.sjhstudio.diary.core.data.NoAdPlayer
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.data.RemoteValues
import org.sjhstudio.diary.core.data.StoreReview
import org.sjhstudio.diary.core.data.StoreUpdates
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
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import platform.Foundation.NSBundle

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        // 광고는 Swift의 구글 광고 다리로(GoogleAdsBridge.swift). 개발 빌드는 늘 시험 광고
        single<AdPlayer>(createdAtStart = true) { IosAds.bridge?.let(::IosAdPlayer) ?: NoAdPlayer }
        single<AdSwitch> { IosAds.switch }
        // 리뷰 창은 Swift 다리로(StoreReviewBridge.swift). 다리 없이 켠 시험 앱은 요청하지 않음
        single<StoreReview> { IosReview.bridge?.let(::IosStoreReview) ?: StoreReview { false } }
        // 새 버전은 App Store 공개 조회, 띄우기는 Swift 다리(AppUpdateBridge.swift, 스펙 009)
        single<RemoteValues> { IosUpdate.remote }
        single<StoreUpdates> { IosStoreUpdates(IosUpdate.bridge, get<BackupAppInfo>().versionName) }
        // 재설치 뒤 구매를 바로 가져오게 앱 시작 때 만듦
        single { iosBilling(appCoroutineScope(Dispatchers.Main.immediate)) }
        single<StartupWork> { StartupWork { get<IosBilling>() } }
        single<PlanRepository> { get<IosBilling>().plans }
        single<PurchaseRepository> { get<IosBilling>().purchases }
        // 날마다 되풀이하는 달력 알림 하나(스펙 001 T070, research R15)
        single<TelemetrySink> { IosTelemetry.sink }
        single<ReminderScheduler> { IosReminderScheduler() }
        single<NotificationPermission> { IosNotificationPermission() }
        single<PlaceFinder> { IosPlaceFinder() }
        single<FontInstaller> { IosFontInstaller(get()) }
        single { DatabaseFactory(get()) }
        single<DriveAuthorizer> { IosDriveAuthorizer(get<Telemetry>()) { IosDrive.bridge } }
        // 시작 때 감시를 켬. 백업·복원이 처음 읽을 때는 실제 연결 값이 들어와 있게
        single<NetworkState>(createdAtStart = true) { IosNetworkState() }
        single<DeviceName> { IosDeviceName() }
        single<BackupScheduler> { NoBackupScheduler }
        single<BackupNotifier> { IosBackupNotifier() }
        single<FreeSpace> { IosFreeSpace() }
        // iOS에는 레거시 사진 경로가 없어 예전 백업의 사진은 찾을 수 없는 사진으로 남음(FR-030)
        single { LegacyPhotoCopier { _, _, _ -> null } }
        single {
            val info = NSBundle.mainBundle.infoDictionary
            BackupAppInfo(
                info?.get("CFBundleShortVersionString") as? String ?: "",
                (info?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0,
                "ios"
            )
        }
        // iOS에는 옮길 옛 앱이 없음(contracts/migration-flow.md)
        single<LegacyMigration> { NoLegacyMigration }
        single<LegacyArchive> { EmptyLegacyArchive }
    }
