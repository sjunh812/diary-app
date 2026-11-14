package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer

class ResumableUploadTest {
    private val fs = FakeFileSystem()
    private val path = "/photo.jpg".toPath()
    private val auth = FakeDriveAuthorizer()
    private val fileJson =
        """{"id":"f1","name":"a.jpg","size":"10","parents":["p"],"sha256Checksum":"abc","md5Checksum":"def",""" +
            """"createdTime":"2026-11-01T00:00:00Z"}"""

    private var backoffs = 0

    private fun api(handler: suspend (Int) -> Pair<HttpStatusCode, Map<String, String>>): Pair<KtorDriveApi, MutableList<String>> {
        var count = 0
        val ranges = mutableListOf<String>()
        val client =
            HttpClient(MockEngine) {
                expectSuccess = false
                followRedirects = false
                engine {
                    addHandler { request ->
                        request.headers[HttpHeaders.ContentRange]?.let(ranges::add)
                        // 실제 엔진처럼 본문을 끝까지 읽음
                        // 파일을 읽지 못하면 여기서 터짐
                        request.body.toByteArray()
                        val (status, headers) = handler(count++)
                        val body = if (status.value in 200..299) fileJson else """{"error":{"errors":[{"reason":"backendError"}]}}"""
                        respond(body, status, headersOf(*headers.map { it.key to listOf(it.value) }.toTypedArray()))
                    }
                }
            }
        return KtorDriveApi(client, auth, fs, backoff = { backoffs++ }) to ranges
    }

    @Test
    fun chunk_is_8mib_and_308_reports_confirmed_bytes() = runTest {
        fs.write(path) { write(ByteArray((UPLOAD_CHUNK + 100).toInt())) }
        val (drive, ranges) = api { HttpStatusCode.PermanentRedirect to mapOf(HttpHeaders.Range to "bytes=0-${UPLOAD_CHUNK - 1}") }

        val result = drive.uploadChunk(UploadSession("https://upload/1", UPLOAD_CHUNK + 100), 0, path)

        assertEquals(ChunkResult.Progress(UPLOAD_CHUNK), result)
        assertEquals("bytes 0-${UPLOAD_CHUNK - 1}/${UPLOAD_CHUNK + 100}", ranges.single())
    }

    @Test
    fun last_chunk_returns_file() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { HttpStatusCode.OK to emptyMap() }

        val done = assertIs<ChunkResult.Done>(drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path))

        assertEquals("abc", done.file.sha256)
        assertEquals(10, done.file.size)
    }

    @Test
    fun session_gone_is_expired() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { HttpStatusCode.NotFound to emptyMap() }

        assertEquals(ChunkResult.Expired, drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path))
    }

    @Test
    fun server_errors_retry_five_times_then_fail() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        var tries = 0
        val (drive, _) = api {
            tries++
            HttpStatusCode.ServiceUnavailable to emptyMap()
        }

        val error = assertFailsWith<DriveException> { drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path) }

        assertEquals(BackupFailure.DRIVE_ERROR, error.kind)
        assertEquals(6, tries)
    }

    @Test
    fun expired_token_is_refreshed_once() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { if (it == 0) HttpStatusCode.Unauthorized to emptyMap() else HttpStatusCode.OK to emptyMap() }

        assertIs<ChunkResult.Done>(drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path))
        assertEquals(2, auth.tokensIssued)
        assertEquals(1, auth.invalidated)
    }

    @Test
    fun second_rejection_needs_reconnect() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { HttpStatusCode.Unauthorized to emptyMap() }

        assertEquals(
            BackupFailure.AUTH,
            assertFailsWith<DriveException> {
                drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path)
            }.kind
        )
    }

    @Test
    fun a_dropped_connection_is_retried_instead_of_stopping_the_backup() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) =
            api { count ->
                if (count < 2) throw okio.IOException("끊김")
                HttpStatusCode.OK to emptyMap()
            }

        assertIs<ChunkResult.Done>(drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path))
    }

    @Test
    fun a_connection_that_stays_down_reports_offline() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { throw okio.IOException("끊김") }

        val failure = assertFailsWith<DriveException> { drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path) }

        assertEquals(BackupFailure.OFFLINE, failure.kind)
    }

    private var calls = 0

    /** 드라이브가 [reason]으로 거절하는 클라이언트 */
    private fun refusing(status: HttpStatusCode, reason: String): KtorDriveApi {
        val client =
            HttpClient(MockEngine) {
                expectSuccess = false
                engine {
                    addHandler {
                        calls++
                        respond("""{"error":{"errors":[{"reason":"$reason"}]}}""", status)
                    }
                }
            }
        return KtorDriveApi(client, auth, fs, backoff = { backoffs++ })
    }

    @Test
    fun a_full_drive_stops_at_once_during_a_chunk() = runTest {
        fs.write(path) { write(ByteArray(10)) }

        val error =
            assertFailsWith<DriveException> {
                refusing(HttpStatusCode.Forbidden, "storageQuotaExceeded").uploadChunk(UploadSession("https://upload/1", 10), 0, path)
            }

        assertEquals(BackupFailure.NO_SPACE, error.kind)
        assertEquals(0, backoffs)
    }

    @Test
    fun taken_back_permission_stops_at_once() = runTest {
        val error = assertFailsWith<DriveException> { refusing(HttpStatusCode.Forbidden, "insufficientPermissions").about() }

        assertEquals(BackupFailure.AUTH, error.kind)
        assertEquals(1, calls)
    }

    @Test
    fun a_lost_file_is_not_retried() = runTest {
        assertFailsWith<DriveNotFound> { refusing(HttpStatusCode.NotFound, "notFound").download("f1", path) }

        assertEquals(0, backoffs)
    }

    @Test
    fun a_failed_download_leaves_no_partial_file_to_mistake_for_a_photo() = runTest {
        assertFailsWith<DriveException> { refusing(HttpStatusCode.InternalServerError, "backendError").download("f1", path) }

        assertEquals(false, fs.exists(path))
    }

    @Test
    fun no_token_needs_the_user_without_calling_drive() = runTest {
        auth.account = null

        val error = assertFailsWith<DriveException> { refusing(HttpStatusCode.OK, "").about() }

        assertEquals(BackupFailure.AUTH, error.kind)
        assertEquals(0, calls)
    }

    @Test
    fun a_file_that_cannot_be_read_is_not_called_offline_or_retried() = runTest {
        val (drive, _) = api { HttpStatusCode.OK to emptyMap() }

        assertFailsWith<IOException> { drive.uploadSmall(NewFile("a.jpg", null, "image/jpeg"), "/none.jpg".toPath()) }

        assertEquals(0, backoffs)
    }

    @Test
    fun a_chunk_that_cannot_be_read_is_not_called_offline_or_retried() = runTest {
        val (drive, _) = api { HttpStatusCode.OK to emptyMap() }

        assertFailsWith<IOException> { drive.uploadChunk(UploadSession("https://upload/1", 10), 0, "/none.jpg".toPath()) }

        assertEquals(0, backoffs)
    }

    @Test
    fun a_download_that_cannot_be_written_is_not_called_offline_or_retried() = runTest {
        val (drive, _) = api { HttpStatusCode.OK to emptyMap() }

        assertFailsWith<IOException> { drive.download("f1", "/no/such/folder/a.jpg".toPath()) }

        assertEquals(0, backoffs)
    }
}
