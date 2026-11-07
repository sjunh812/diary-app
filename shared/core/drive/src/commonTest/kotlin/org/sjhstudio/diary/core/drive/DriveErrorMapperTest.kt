package org.sjhstudio.diary.core.drive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.BackupFailure

class DriveErrorMapperTest {
    @Test
    fun token_rejected_is_auth() = assertIs<DriveUnauthorized>(DriveErrorMapper.map(401, null)).let {}

    @Test
    fun quota_is_no_space_and_not_retried() {
        val error = DriveErrorMapper.map(403, "storageQuotaExceeded")
        assertEquals(BackupFailure.NO_SPACE, error.kind)
        assertFalse(error.retryable)
    }

    @Test
    fun rate_limits_and_server_errors_are_retried() {
        for ((status, reason) in listOf(
            403 to "userRateLimitExceeded",
            403 to "rateLimitExceeded",
            429 to null,
            500 to null,
            503 to null
        )) {
            val error = DriveErrorMapper.map(status, reason)
            assertEquals(BackupFailure.DRIVE_ERROR, error.kind)
            assertTrue(error.retryable)
        }
    }

    @Test
    fun other_forbidden_needs_reconnect() = assertEquals(BackupFailure.AUTH, DriveErrorMapper.map(403, "insufficientPermissions").kind)

    @Test
    fun missing_file() = assertIs<DriveNotFound>(DriveErrorMapper.map(404, null)).let {}

    @Test
    fun message_never_carries_drive_text() {
        val error = DriveErrorMapper.map(403, "내 일기 파일 이름")
        assertFalse(error.message.orEmpty().contains("일기"))
    }
}
