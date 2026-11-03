package org.sjhstudio.diary.core.platform

import android.content.Context
import java.io.File

actual class AppFiles(context: Context) {
    private val filesDir: File = context.filesDir
    private val noBackupDir: File = context.noBackupFilesDir

    actual val databasePath: String = context.getDatabasePath(DATABASE_FILE).absolutePath
    actual val mediaDirectory: String = File(filesDir, "media").absolutePath

    // no_backup은 안드로이드 자동 백업에 들어가지 않는다.
    actual val thumbnailDirectory: String = File(noBackupDir, "thumbs").absolutePath
    actual val fontDirectory: String = File(noBackupDir, "fonts").absolutePath
    actual val dataStoreDirectory: String = File(filesDir, "datastore").absolutePath
    actual val migrationDirectory: String? = File(filesDir, "migration").absolutePath
    actual val legacySnapshotDirectory: String? = File(filesDir, "legacy-snapshot").absolutePath

    actual fun ensureDirectories() {
        listOfNotNull(
            File(databasePath).parent,
            mediaDirectory,
            thumbnailDirectory,
            fontDirectory,
            dataStoreDirectory,
            migrationDirectory,
            legacySnapshotDirectory
        ).forEach { File(it).mkdirs() }
    }

    private companion object {
        const val DATABASE_FILE = "diary.db"
    }
}
