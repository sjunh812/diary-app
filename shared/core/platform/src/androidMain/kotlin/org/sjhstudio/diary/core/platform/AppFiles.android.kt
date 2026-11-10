package org.sjhstudio.diary.core.platform

import android.content.Context
import java.io.File

// 시스템 폴더를 물으면 폴더를 만들려고 디스크를 건드려, 화면 스레드에서 만들어도 막지 않게 경로로만 셈
actual class AppFiles(context: Context) {
    private val data = File(context.applicationInfo.dataDir)
    private val filesDir: File = File(data, "files")
    private val noBackupDir: File = File(data, "no_backup")

    actual val databasePath: String = File(File(data, "databases"), DATABASE_FILE).absolutePath
    actual val mediaDirectory: String = File(filesDir, "media").absolutePath

    // no_backup은 안드로이드 자동 백업에 들어가지 않음
    actual val thumbnailDirectory: String = File(noBackupDir, "thumbs").absolutePath
    actual val fontDirectory: String = File(noBackupDir, "fonts").absolutePath
    actual val dataStoreDirectory: String = File(filesDir, "datastore").absolutePath
    actual val backupDirectory: String = File(filesDir, "backup").absolutePath
    actual val migrationDirectory: String? = File(filesDir, "migration").absolutePath
    actual val legacySnapshotDirectory: String? = File(filesDir, "legacy-snapshot").absolutePath

    actual fun ensureDirectories() {
        listOfNotNull(
            File(databasePath).parent,
            mediaDirectory,
            thumbnailDirectory,
            fontDirectory,
            dataStoreDirectory,
            backupDirectory,
            migrationDirectory,
            legacySnapshotDirectory
        ).forEach { File(it).mkdirs() }
    }

    private companion object {
        const val DATABASE_FILE = "diary.db"
    }
}
