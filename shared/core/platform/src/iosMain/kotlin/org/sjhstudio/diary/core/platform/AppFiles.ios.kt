package org.sjhstudio.diary.core.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AppFiles {
    // Application Support는 사용자에게 보이지 않고 시스템이 비우지 않는 앱 전용 자리
    private val root: String =
        requireNotNull(
            (
                NSFileManager.defaultManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
                    .firstOrNull() as? NSURL
                )?.path
        ) { "Application Support 폴더를 찾지 못했다" }

    actual val databasePath: String = "$root/diary.db"
    actual val mediaDirectory: String = "$root/media"
    actual val thumbnailDirectory: String = "$root/thumbs"
    actual val fontDirectory: String = "$root/fonts"
    actual val dataStoreDirectory: String = "$root/datastore"
    actual val backupDirectory: String = "$root/backup"
    actual val migrationDirectory: String? = null
    actual val legacySnapshotDirectory: String? = null

    actual fun ensureDirectories() {
        val manager = NSFileManager.defaultManager
        listOf(root, mediaDirectory, thumbnailDirectory, fontDirectory, dataStoreDirectory, backupDirectory).forEach { path ->
            manager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        }
        // 썸네일과 받은 글꼴은 다시 만들거나 받을 수 있어 iCloud·기기 백업에서 뺌
        // 복원 전 사본과 묶음 작업 파일도 잠깐 쓰는 것이라 뺌
        for (path in listOf(thumbnailDirectory, fontDirectory, backupDirectory)) {
            NSURL.fileURLWithPath(path, isDirectory = true)
                .setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
        }
    }
}
