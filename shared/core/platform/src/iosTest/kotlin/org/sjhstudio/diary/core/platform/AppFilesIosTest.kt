package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AppFilesIosTest {
    private val files = AppFiles()

    @Test
    fun everything_lives_in_application_support_not_caches() {
        listOf(files.databasePath, files.mediaDirectory, files.thumbnailDirectory, files.dataStoreDirectory).forEach { path ->
            assertTrue(path.contains("/Library/Application Support/"), path)
            assertFalse(path.contains("/Caches/"), path)
        }
        assertNull(files.migrationDirectory)
        assertNull(files.legacySnapshotDirectory)
    }

    @Test
    fun ensureDirectories_creates_folders_and_excludes_thumbnails_from_backup() {
        files.ensureDirectories()

        val manager = NSFileManager.defaultManager
        listOf(files.mediaDirectory, files.thumbnailDirectory, files.dataStoreDirectory).forEach { path ->
            assertTrue(manager.fileExistsAtPath(path), path)
        }
        assertEquals(true, isExcludedFromBackup(files.thumbnailDirectory))
        assertEquals(false, isExcludedFromBackup(files.mediaDirectory))
    }

    private fun isExcludedFromBackup(path: String): Boolean? = memScoped {
        val value = alloc<ObjCObjectVar<Any?>>()
        NSURL.fileURLWithPath(path, isDirectory = true).getResourceValue(value.ptr, forKey = NSURLIsExcludedFromBackupKey, error = null)
        (value.value as? NSNumber)?.boolValue
    }
}
