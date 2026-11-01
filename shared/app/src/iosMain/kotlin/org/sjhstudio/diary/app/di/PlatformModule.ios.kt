package org.sjhstudio.diary.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.EmptyLegacyArchive
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.platform.AppFiles

internal actual val platformModule: Module =
    module {
        single { AppFiles() }
        single { DatabaseFactory(get()) }
        // iOS에는 옮길 옛 앱이 없다(contracts/migration-flow.md).
        single<LegacyMigration> { NoLegacyMigration }
        single<LegacyArchive> { EmptyLegacyArchive }
    }
