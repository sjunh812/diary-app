package org.sjhstudio.diary.feature.migration

import org.jetbrains.compose.resources.getPluralString
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.migration_entries_recovered
import org.sjhstudio.diary.feature.migration.resources.migration_photos_found

/** 관문을 지난 뒤 한 번 띄우는 토스트 문구(FR-011, contracts/migration-flow.md) */
suspend fun MigrationNotice.message(): String = when (this) {
    is MigrationNotice.EntriesRecovered -> getPluralString(Res.plurals.migration_entries_recovered, count, count.grouped())
    is MigrationNotice.PhotosFound -> getPluralString(Res.plurals.migration_photos_found, count, count.grouped())
}
