package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.BackupFailure

/** 실패 보고를 더해도 연결·토큰 결과는 그대로이고, Swift가 오류 종류를 넘긴 실패만 남음 */
class IosDriveAuthorizerTest {
    private class Bridge(val tokenWith: List<Any?> = emptyList()) : IosDriveBridge {
        var connectResult: List<Any?> = emptyList()

        override fun connect(hint: String?, done: (String?, String?, String?, String?, Long) -> Unit) {
            val r = connectResult
            done(r[0] as String?, r[1] as String?, r[2] as String?, r[3] as String?, r[4] as Long)
        }

        override fun accessToken(done: (String?, String?, String?, String?, String?, Long) -> Unit) {
            val r = tokenWith
            done(r[0] as String?, r[1] as String?, r[2] as String?, r[3] as String?, r[4] as String?, r[5] as Long)
        }

        override fun disconnect(done: () -> Unit) = done()
    }

    private val recorded = mutableListOf<Triple<ErrorArea, String, Long>>()
    private val errors = ErrorReporter { area, domain, code -> recorded += Triple(area, domain, code) }

    private fun authorizer(bridge: Bridge) = IosDriveAuthorizer(errors) { bridge }

    @Test
    fun a_reported_connect_failure_keeps_the_same_result() = runTest {
        val bridge = Bridge().apply { connectResult = listOf(null, null, "failed", "com.google.GIDSignIn", -4L) }

        assertEquals(ConnectResult.Failed(BackupFailure.AUTH), authorizer(bridge).connect(null))
        assertEquals(listOf(Triple(ErrorArea.DRIVE_CONNECT, "com.google.GIDSignIn", -4L)), recorded)
    }

    @Test
    fun unchecking_drive_says_so_instead_of_retry_later() = runTest {
        val bridge = Bridge().apply { connectResult = listOf(null, null, "noDriveAccess", "app.noDriveScope", 0L) }

        assertEquals(ConnectResult.Failed(BackupFailure.NO_DRIVE_ACCESS), authorizer(bridge).connect(null))
        assertEquals(listOf(Triple(ErrorArea.DRIVE_CONNECT, "app.noDriveScope", 0L)), recorded)
    }

    @Test
    fun cancel_offline_and_success_leave_no_report() = runTest {
        val cases =
            listOf(
                listOf(null, null, "cancelled", null, 0L) to ConnectResult.Cancelled,
                listOf(null, null, "offline", null, 0L) to ConnectResult.Failed(BackupFailure.OFFLINE),
                listOf("key", "a@b.c", null, null, 0L) to ConnectResult.Connected(DriveAccount("key", "a@b.c"))
            )
        for ((result, expected) in cases) {
            val bridge = Bridge().apply { connectResult = result }
            assertEquals(expected, authorizer(bridge).connect(null))
        }
        assertTrue(recorded.isEmpty(), recorded.toString())
    }

    @Test
    fun a_reported_token_failure_keeps_the_same_result() = runTest {
        val bridge = Bridge(tokenWith = listOf(null, null, null, "needsUser", "app.noDriveScope", 0L))

        assertEquals(TokenResult.NeedsUser, authorizer(bridge).accessToken())
        assertEquals(listOf(Triple(ErrorArea.DRIVE_TOKEN, "app.noDriveScope", 0L)), recorded)
    }

    @Test
    fun a_token_is_not_reported() = runTest {
        val bridge = Bridge(tokenWith = listOf("token", "key", "a@b.c", null, null, 0L))

        assertTrue(authorizer(bridge).accessToken() is TokenResult.Token)
        assertTrue(recorded.isEmpty())
    }
}
