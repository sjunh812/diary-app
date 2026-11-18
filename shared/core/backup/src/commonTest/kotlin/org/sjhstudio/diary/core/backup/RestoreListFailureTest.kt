package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.sjhstudio.diary.core.data.BackupList
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveErrorMapper
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.ErrorArea
import org.sjhstudio.diary.core.platform.ErrorReporter
import org.sjhstudio.diary.core.testing.FakeDrive

/** 백업 목록을 읽지 못하면 까닭을 화면까지 넘기고, 고칠 수 있는 실패만 오류 보고에 남김 */
class RestoreListFailureTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var phone: TestPhone
    private val reported = mutableListOf<Triple<ErrorArea, String, Long>>()
    private val errors = ErrorReporter { area, domain, code -> reported += Triple(area, domain, code) }

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-list-${Random.nextLong()}"
        drive = FakeDrive(fs)
        phone = TestPhone(dir, drive)
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        fs.deleteRecursively(dir)
    }

    private fun failingWith(error: Exception) = object : DriveApi by drive {
        override suspend fun list(query: DriveQuery, pageToken: String?) = throw error
    }

    @Test
    fun a_refused_permission_says_account_and_is_reported() = runTest {
        val controller = phone.controller(
            backgroundScope,
            api = failingWith(DriveErrorMapper.map(403, "insufficientPermissions")),
            errors = errors
        )

        assertEquals(BackupList.Failed(BackupFailure.AUTH), controller.backups())
        assertEquals(listOf(Triple(ErrorArea.RESTORE_LIST, "DriveException.AUTH", 403L)), reported)
    }

    @Test
    fun being_offline_is_not_reported() = runTest {
        val controller = phone.controller(backgroundScope, api = failingWith(DriveException(BackupFailure.OFFLINE)), errors = errors)

        assertEquals(BackupList.Failed(BackupFailure.OFFLINE), controller.backups())
        assertTrue(reported.isEmpty())
    }

    @Test
    fun an_unexpected_error_is_unknown_without_its_message() = runTest {
        val controller = phone.controller(backgroundScope, api = failingWith(IllegalStateException("user@example.com 일기")), errors = errors)

        assertEquals(BackupList.Failed(BackupFailure.UNKNOWN), controller.backups())
        assertEquals("IllegalStateException.UNKNOWN", reported.single().second)
    }
}
