package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer

class ResumableUploadTest {
    private val fs = FakeFileSystem()
    private val path = "/photo.jpg".toPath()
    private val auth = FakeDriveAuthorizer()
    private val fileJson = """{"id":"f1","name":"a.jpg","size":"10","parents":["p"],"sha256Checksum":"abc","md5Checksum":"def","createdTime":"2026-11-01T00:00:00Z"}"""

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
                        val (status, headers) = handler(count++)
                        val body = if (status.value in 200..299) fileJson else """{"error":{"errors":[{"reason":"backendError"}]}}"""
                        respond(body, status, headersOf(*headers.map { it.key to listOf(it.value) }.toTypedArray()))
                    }
                }
            }
        return KtorDriveApi(client, auth, fs, backoff = {}) to ranges
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
        val (drive, _) = api { tries++; HttpStatusCode.ServiceUnavailable to emptyMap() }

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
    }

    @Test
    fun second_rejection_needs_reconnect() = runTest {
        fs.write(path) { write(ByteArray(10)) }
        val (drive, _) = api { HttpStatusCode.Unauthorized to emptyMap() }

        assertEquals(BackupFailure.AUTH, assertFailsWith<DriveException> { drive.uploadChunk(UploadSession("https://upload/1", 10), 0, path) }.kind)
    }
}
