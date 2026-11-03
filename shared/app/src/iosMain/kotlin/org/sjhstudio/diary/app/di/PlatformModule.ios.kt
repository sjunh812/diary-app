package org.sjhstudio.diary.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.EmptyLegacyArchive
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.IosFontInstaller
import org.sjhstudio.diary.core.platform.IosNotificationPermission
import org.sjhstudio.diary.core.platform.IosReminderScheduler
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.ReminderScheduler

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        // 날마다 되풀이하는 달력 알림 하나(스펙 001 T070, research R15).
        single<ReminderScheduler> { IosReminderScheduler() }
        single<NotificationPermission> { IosNotificationPermission() }
        single<FontInstaller> { IosFontInstaller(get()) }
        single { DatabaseFactory(get()) }
        // iOS에는 옮길 옛 앱이 없다(contracts/migration-flow.md).
        single<LegacyMigration> { NoLegacyMigration }
        single<LegacyArchive> { EmptyLegacyArchive }
    }
