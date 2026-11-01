package org.sjhstudio.diary.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.EmptyLegacyArchive
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.NoReminderScheduler
import org.sjhstudio.diary.core.platform.ReminderScheduler

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        // iOS 알림 예약은 알림 설정 화면과 함께 붙인다(스펙 001 T070).
        single<ReminderScheduler> { NoReminderScheduler }
        single { DatabaseFactory(get()) }
        // iOS에는 옮길 옛 앱이 없다(contracts/migration-flow.md).
        single<LegacyMigration> { NoLegacyMigration }
        single<LegacyArchive> { EmptyLegacyArchive }
    }
