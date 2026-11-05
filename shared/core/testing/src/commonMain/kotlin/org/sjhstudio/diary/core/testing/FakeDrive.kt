package org.sjhstudio.diary.core.testing

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.use
import org.sjhstudio.diary.core.drive.ChunkResult
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveErrorMapper
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.DriveUnauthorized
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.StorageQuota
import org.sjhstudio.diary.core.drive.UPLOAD_CHUNK
import org.sjhstudio.diary.core.drive.UploadSession
import org.sjhstudio.diary.core.model.BackupFailure

/** 다음 드라이브 호출에 끼워 넣는 실패. */
enum class DriveFault { OFFLINE, UNAUTHORIZED, RATE_LIMIT, SERVER_ERROR, NO_SPACE }

/**
 * 메모리 드라이브(contracts/drive-gateway.md FakeDrive).
 * 실제 드라이브처럼 SHA-256·MD5를 계산하고 `appProperties` 질의와 쪽 넘김을 흉내 낸다.
 */
class FakeDrive(private val fileSystem: FileSystem, var limit: Long? = null, private val pageSize: Int = 1000) : DriveApi {
    private class Stored(var meta: DriveFile, val bytes: ByteArray, var trashed: Boolean = false)

    private class Session(val meta: NewFile, val size: Long, var bytes: ByteArray = ByteArray(0), var expired: Boolean = false)

    private val files = LinkedHashMap<String, Stored>()
    private val sessions = HashMap<String, Session>()
    private var nextId = 1
    private var clock = 1L

    /** 호출마다 앞에서 하나씩 꺼내 실패시킨다. */
    val faults = ArrayDeque<DriveFault>()

    /** 이름이 맞는 파일은 드라이브가 다른 확인값을 돌려준다. */
    val corruptNames = HashSet<String>()

    /** 이어 올리기 조각을 이만큼 받은 뒤 끊는다. */
    var dropAfterChunks: Int? = null

    /** 드라이브가 `sha256Checksum`을 주지 않는 경우. */
    var omitSha256 = false

    /** 파일을 받을 때마다 부른다. 백업이 도는 도중에 일어나는 일을 흉내 낸다. */
    var onUpload: (suspend (NewFile) -> Unit)? = null

    var calls = 0
        private set

    val visible: List<DriveFile> get() = files.values.filter { !it.trashed }.map { it.meta }
    val trashedNames: List<String> get() = files.values.filter { it.trashed }.map { it.meta.name }

    fun bytes(fileId: String): ByteArray = files.getValue(fileId).bytes

    fun usage(): Long = files.values.sumOf { it.bytes.size.toLong() }

    fun expireSessions() = sessions.values.forEach { it.expired = true }

    /** 사용자가 드라이브에서 지운 경우. */
    fun deleteByUser(fileId: String) {
        files.getValue(fileId).trashed = true
    }

    override suspend fun about(): StorageQuota {
        gate()
        return StorageQuota(limit, usage())
    }

    override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage {
        gate()
        val matched =
            files.values.filter { !it.trashed }.map { it.meta }.filter { file ->
                (query.name == null || file.name == query.name) &&
                    (query.parentId == null || query.parentId in file.parents) &&
                    query.props.all { (key, value) -> file.props[key] == value }
            }
        val from = pageToken?.toInt() ?: 0
        val to = minOf(from + pageSize, matched.size)
        return DrivePage(matched.subList(from, to), to.takeIf { it < matched.size }?.toString())
    }

    override suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>): DriveFile {
        gate()
        return store(NewFile(name, parentId, "folder", props), ByteArray(0))
    }

    override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile {
        gate()
        onUpload?.invoke(meta)
        return store(meta, fileSystem.read(body) { readByteArray() })
    }

    override suspend fun startResumable(meta: NewFile, size: Long): UploadSession {
        gate()
        val uri = "session-${nextId++}"
        sessions[uri] = Session(meta, size)
        return UploadSession(uri, size)
    }

    override suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult {
        gate()
        val state = sessions.getValue(session.uri)
        if (state.expired) return ChunkResult.Expired
        dropAfterChunks?.let {
            if (it <= 0) {
                dropAfterChunks = null
                throw DriveException(BackupFailure.OFFLINE)
            }
            dropAfterChunks = it - 1
        }
        val all = fileSystem.read(body) { readByteArray() }
        val end = minOf(from + UPLOAD_CHUNK, session.size).toInt()
        state.bytes = state.bytes.copyOf(from.toInt()) + all.copyOfRange(from.toInt(), end)
        if (end.toLong() < session.size) return ChunkResult.Progress(end.toLong())
        sessions.remove(session.uri)
        return ChunkResult.Done(store(state.meta, state.bytes))
    }

    override suspend fun queryResumable(session: UploadSession): ChunkResult {
        gate()
        val state = sessions[session.uri] ?: return ChunkResult.Expired
        return if (state.expired) ChunkResult.Expired else ChunkResult.Progress(state.bytes.size.toLong())
    }

    override suspend fun get(fileId: String): DriveFile {
        gate()
        val file = files[fileId]?.takeUnless { it.trashed } ?: throw DriveNotFound()
        return file.meta
    }

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        gate()
        val file = files[fileId]?.takeUnless { it.trashed } ?: throw DriveNotFound()
        val rest = file.bytes.copyOfRange(fromByte.toInt(), file.bytes.size)
        if (fromByte >
            0
        ) {
            fileSystem.appendingSink(to).use { it.write(okio.Buffer().write(rest), rest.size.toLong()) }
        } else {
            fileSystem.write(to) { write(rest) }
        }
        return file.bytes.size.toLong()
    }

    override suspend fun trash(fileId: String) {
        gate()
        (files[fileId] ?: throw DriveNotFound()).trashed = true
    }

    private fun gate() {
        calls++
        when (faults.removeFirstOrNull()) {
            null -> Unit
            DriveFault.OFFLINE -> throw DriveException(BackupFailure.OFFLINE)
            DriveFault.UNAUTHORIZED -> throw DriveUnauthorized()
            DriveFault.RATE_LIMIT -> throw DriveErrorMapper.map(403, "userRateLimitExceeded")
            DriveFault.SERVER_ERROR -> throw DriveErrorMapper.map(503, null)
            DriveFault.NO_SPACE -> throw DriveErrorMapper.map(403, "storageQuotaExceeded")
        }
    }

    private fun store(meta: NewFile, bytes: ByteArray): DriveFile {
        limit?.let { if (usage() + bytes.size > it) throw DriveErrorMapper.map(403, "storageQuotaExceeded") }
        val id = "file-${nextId++}"
        val content = bytes.toByteString()
        val corrupt = meta.name in corruptNames
        val file =
            DriveFile(
                id = id,
                name = meta.name,
                size = bytes.size.toLong(),
                parents = listOfNotNull(meta.parentId),
                sha256 = if (omitSha256) {
                    null
                } else if (corrupt) {
                    "0".repeat(64)
                } else {
                    content.sha256().hex()
                },
                md5 = if (corrupt) "0".repeat(32) else content.md5().hex(),
                props = meta.props,
                createdAt = clock++
            )
        files[id] = Stored(file, bytes)
        return file
    }
}
