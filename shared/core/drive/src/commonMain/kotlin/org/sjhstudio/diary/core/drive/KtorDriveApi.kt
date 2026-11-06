package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ChannelWriterContent
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.io.IOException as KotlinxIOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.TokenResult

/**
 * 드라이브 REST v3를 공통 코드에서 직접 부른다(research R3).
 * 본문은 파일에서 스트리밍으로 보내고 받는다.
 */
class KtorDriveApi(
    private val client: HttpClient,
    private val authorizer: DriveAuthorizer,
    private val fileSystem: FileSystem,
    private val backoff: suspend (attempt: Int) -> Unit = { delay((1L shl it) * 1_000 + Random.nextLong(1_000)) }
) : DriveApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun about(): StorageQuota = call { token ->
        val quota = parse(client.get("$API/about?fields=storageQuota") { bearer(token) }).getValue("storageQuota").jsonObject
        StorageQuota(
            limit = quota["limit"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
            usage =
                quota["usage"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0
        )
    }

    override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage = call { token ->
        val page = pageToken?.let { "&pageToken=${it.encodeURLParameter()}" }.orEmpty()
        val fields = "nextPageToken,files($FIELDS)".encodeURLParameter()
        val url = "$API/files?pageSize=1000&spaces=drive&fields=$fields&q=${query.toQ().encodeURLParameter()}$page"
        val body = parse(client.get(url) { bearer(token) })
        DrivePage(body["files"]?.jsonArray.orEmpty().map { it.jsonObject.asFile() }, body["nextPageToken"]?.jsonPrimitive?.contentOrNull)
    }

    override suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>): DriveFile = call { token ->
        val meta = NewFile(name, parentId, FOLDER_MIME, props)
        parse(
            client.post("$API/files?fields=${FIELDS.encodeURLParameter()}") {
                bearer(token)
                setBody(TextContent(meta.toJson(), ContentType.Application.Json))
            }
        ).asFile()
    }

    override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
        val boundary = "diary-backup-boundary"
        // 파일은 다시 해 볼 일이 아니라서 재시도 밖에서 읽는다.
        val bytes = onDevice { fileSystem.read(body) { readByteArray() } }
        val head =
            "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta.toJson()}\r\n" +
                "--$boundary\r\nContent-Type: ${meta.mimeType}\r\n\r\n"
        val tail = "\r\n--$boundary--"
        return call { token ->
            parse(
                client.post("$UPLOAD/files?uploadType=multipart&fields=${FIELDS.encodeURLParameter()}") {
                    bearer(token)
                    header(HttpHeaders.ContentType, "multipart/related; boundary=$boundary")
                    setBody(head.encodeToByteArray() + bytes + tail.encodeToByteArray())
                }
            ).asFile()
        }
    }

    override suspend fun startResumable(meta: NewFile, size: Long): UploadSession = call { token ->
        val response =
            client.post("$UPLOAD/files?uploadType=resumable&fields=${FIELDS.encodeURLParameter()}") {
                bearer(token)
                header("X-Upload-Content-Type", meta.mimeType)
                header("X-Upload-Content-Length", size.toString())
                setBody(TextContent(meta.toJson(), ContentType.Application.Json))
            }
        check(response)
        UploadSession(response.headers[HttpHeaders.Location] ?: throw DriveException(BackupFailure.DRIVE_ERROR, retryable = true), size)
    }

    override suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult = call { token ->
        val end = minOf(from + UPLOAD_CHUNK, session.size)
        val length = end - from
        // 본문을 쓰는 쪽에서 던지면 엔진마다 끝나는 모양이 달라서, 적어 두고 본문을 짧게 끝낸다.
        var unreadable: DriveLocalFileFailure? = null
        val response =
            try {
                client.put(session.uri) {
                    bearer(token)
                    header(HttpHeaders.ContentRange, "bytes $from-${end - 1}/${session.size}")
                    setBody(
                        ChannelWriterContent(
                            body = {
                                try {
                                    onDevice { fileSystem.source(body) }.use { raw ->
                                        raw.buffer().use { source ->
                                            onDevice { source.skip(from) }
                                            val buffer = ByteArray(BUFFER)
                                            var left = length
                                            while (left > 0) {
                                                val read = onDevice { source.read(buffer, 0, minOf(left, BUFFER.toLong()).toInt()) }
                                                if (read < 0) break
                                                writeFully(buffer, 0, read)
                                                left -= read
                                            }
                                        }
                                    }
                                } catch (error: DriveLocalFileFailure) {
                                    unreadable = error
                                }
                            },
                            contentType = ContentType.Application.OctetStream,
                            contentLength = length
                        )
                    )
                }
            } catch (error: Exception) {
                throw unreadable ?: error
            }
        unreadable?.let { throw it }
        chunkResult(response)
    }

    override suspend fun queryResumable(session: UploadSession): ChunkResult = call { token ->
        chunkResult(
            client.put(session.uri) {
                bearer(token)
                header(HttpHeaders.ContentRange, "bytes */${session.size}")
                setBody(ByteArray(0))
            }
        )
    }

    override suspend fun get(fileId: String): DriveFile = call { token ->
        parse(client.get("$API/files/${fileId.encodeURLParameter()}?fields=${FIELDS.encodeURLParameter()}") { bearer(token) }).asFile()
    }

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long = call { token ->
        var total = fromByte
        client.prepareGet("$API/files/${fileId.encodeURLParameter()}?alt=media") {
            bearer(token)
            if (fromByte > 0) header(HttpHeaders.Range, "bytes=$fromByte-")
        }.execute { response ->
            if (response.status.value !in 200..299) throw failure(response)
            val channel = response.bodyAsChannel()
            val sink = onDevice { if (fromByte > 0) fileSystem.appendingSink(to) else fileSystem.sink(to) }
            sink.buffer().use { out ->
                val buffer = ByteArray(BUFFER)
                while (true) {
                    val read = channel.readAvailable(buffer)
                    if (read < 0) break
                    onDevice { out.write(buffer, 0, read) }
                    total += read
                }
                onDevice { out.flush() }
            }
        }
        total
    }

    override suspend fun trash(fileId: String) {
        call { token ->
            check(
                client.patch("$API/files/${fileId.encodeURLParameter()}?fields=id") {
                    bearer(token)
                    setBody(TextContent("""{"trashed":true}""", ContentType.Application.Json))
                }
            )
        }
    }

    /**
     * 토큰을 받아 부른다.
     * 401은 한 번 새로 받아 다시 하고, 잠깐 거절은 1·2·4·8·16초쯤 쉬며 다섯 번까지 다시 한다.
     */
    private suspend fun <T> call(block: suspend (token: String) -> T): T {
        var attempt = 0
        var refreshed = false
        while (true) {
            try {
                return block(token())
            } catch (error: CancellationException) {
                throw error
            } catch (error: DriveLocalFileFailure) {
                throw error
            } catch (_: DriveUnauthorized) {
                if (refreshed) throw DriveException(BackupFailure.AUTH, status = 401)
                refreshed = true
                authorizer.invalidateToken()
            } catch (error: DriveException) {
                if (!error.retryable || attempt >= RETRIES) throw error
                backoff(attempt++)
            } catch (_: IOException) {
                // 사진 수천 장을 올리다 보면 연결이 잠깐 끊긴다.
                // 한 번에 백업 전체를 멈추지 않는다.
                if (attempt >= RETRIES) throw DriveException(BackupFailure.OFFLINE)
                backoff(attempt++)
            } catch (_: KotlinxIOException) {
                if (attempt >= RETRIES) throw DriveException(BackupFailure.OFFLINE)
                backoff(attempt++)
            }
        }
    }

    /**
     * 기기 파일을 만지는 일.
     * 드라이브 호출과 달리 실패해도 다시 하지 않는다.
     */
    private inline fun <T> onDevice(block: () -> T): T = try {
        block()
    } catch (_: IOException) {
        throw DriveLocalFileFailure()
    }

    private suspend fun token(): String = when (val result = authorizer.accessToken()) {
        is TokenResult.Token -> result.value
        TokenResult.NeedsUser -> throw DriveException(BackupFailure.AUTH)
        is TokenResult.Failed -> throw DriveException(result.kind, retryable = result.kind == BackupFailure.DRIVE_ERROR)
    }

    private suspend fun chunkResult(response: HttpResponse): ChunkResult = when (response.status.value) {
        200, 201 -> ChunkResult.Done(json.parseToJsonElement(response.bodyAsText()).jsonObject.asFile())
        308 -> ChunkResult.Progress(response.headers[HttpHeaders.Range]?.substringAfterLast('-')?.toLongOrNull()?.plus(1) ?: 0)
        404, 410 -> ChunkResult.Expired
        else -> throw failure(response)
    }

    private suspend fun check(response: HttpResponse) {
        if (response.status.value !in 200..299) throw failure(response)
    }

    private suspend fun parse(response: HttpResponse): JsonObject {
        check(response)
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun failure(response: HttpResponse): DriveException {
        val reason = runCatching { response.bodyAsBytes().decodeToString() }.getOrNull()?.let(::errorReason)
        return DriveErrorMapper.map(response.status.value, reason)
    }

    private fun errorReason(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("errors")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("reason")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun HttpRequestBuilder.bearer(token: String) = header(HttpHeaders.Authorization, "Bearer $token")

    private fun NewFile.toJson(): String = buildJsonObject {
        put("name", name)
        put("mimeType", mimeType)
        parentId?.let { parent -> putJsonArray("parents") { add(kotlinx.serialization.json.JsonPrimitive(parent)) } }
        if (props.isNotEmpty()) putJsonObject("appProperties") { props.forEach { (key, value) -> put(key, value) } }
    }.toString()

    private fun JsonObject.text(key: String): String? = (this[key] as JsonElement?)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.asFile() = DriveFile(
        id = getValue("id").jsonPrimitive.content,
        name = text("name").orEmpty(),
        size = text("size")?.toLongOrNull(),
        parents = this["parents"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
        sha256 = text("sha256Checksum"),
        md5 = text("md5Checksum"),
        props = this["appProperties"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty(),
        createdAt = text("createdTime")?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0
    )

    companion object {
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val FIELDS = "id,name,size,parents,sha256Checksum,md5Checksum,appProperties,createdTime"
        private const val RETRIES = 5
        private const val BUFFER = 64 * 1024
    }
}
