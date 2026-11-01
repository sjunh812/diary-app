package org.sjhstudio.diary.app.di

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.migration.AndroidImageSizeReader
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.MigrationPaths
import org.sjhstudio.diary.core.migration.MigrationRunner
import org.sjhstudio.diary.core.migration.MigrationTelemetry
import org.sjhstudio.diary.core.migration.freeBytes
import org.sjhstudio.diary.core.migration.legacyFiles
import org.sjhstudio.diary.core.platform.AppFiles

@OptIn(ExperimentalUuidApi::class)
internal actual val platformModule: Module =
    module {
        single { AppFiles(androidContext()) }
        single { DatabaseFactory(androidContext(), get()) }
        // 1.7.x 원본이 같은 패키지에 남아 있으면 옮긴다(docs/migration.md).
        single<LegacyMigration> {
            val files = get<AppFiles>()
            val factory = get<DatabaseFactory>()
            MigrationRunner(
                fileSystem = FileSystem.SYSTEM,
                paths =
                    MigrationPaths(
                        legacy = legacyFiles(androidContext()),
                        snapshotDir = requireNotNull(files.legacySnapshotDirectory).toPath(),
                        migrationDir = requireNotNull(files.migrationDirectory).toPath(),
                        mediaDir = files.mediaDirectory.toPath(),
                        databasePath = files.databasePath.toPath()
                    ),
                clock = Clock.System,
                timeZone = TimeZone.currentSystemDefault(),
                newId = { Uuid.random().toString() },
                freeBytes = ::freeBytes,
                openDatabase = factory::createAt,
                imageSize = AndroidImageSizeReader,
                events = MigrationTelemetry(get())
            )
        }
    }
