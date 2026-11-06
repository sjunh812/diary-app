package org.sjhstudio.diary.core.drive

import okio.Path

/**
 * 드라이브 저장 공간.
 * [limit]이 null이면 한도가 없는 계정이다.
 */
data class StorageQuota(val limit: Long?, val usage: Long) {
    val free: Long? get() = limit?.let { (it - usage).coerceAtLeast(0) }
}

data class DriveFile(
    val id: String,
    val name: String,
    val size: Long?,
    val parents: List<String>,
    val sha256: String?,
    val md5: String?,
    val props: Map<String, String>,
    val createdAt: Long
)

/**
 * 파일 찾기 조건.
 * 휴지통의 파일은 늘 뺀다.
 */
data class DriveQuery(val name: String? = null, val parentId: String? = null, val props: Map<String, String> = emptyMap()) {
    fun toQ(): String = buildList {
        add("trashed = false")
        name?.let { add("name = '${it.escaped()}'") }
        parentId?.let { add("'${it.escaped()}' in parents") }
        props.forEach { (key, value) -> add("appProperties has { key='${key.escaped()}' and value='${value.escaped()}' }") }
    }.joinToString(" and ")

    private fun String.escaped() = replace("\\", "\\\\").replace("'", "\\'")
}

data class DrivePage(val files: List<DriveFile>, val nextPageToken: String?)

data class NewFile(val name: String, val parentId: String?, val mimeType: String, val props: Map<String, String> = emptyMap())

/**
 * 이어 올리기 세션.
 * [uri]는 드라이브가 정한 일회용 주소로 일주일 동안 유효하다.
 */
data class UploadSession(val uri: String, val size: Long)

sealed interface ChunkResult {
    data class Progress(val confirmedBytes: Long) : ChunkResult

    data class Done(val file: DriveFile) : ChunkResult

    /**
     * 세션이 없어졌다.
     * 처음부터 다시 올린다.
     */
    data object Expired : ChunkResult
}

/**
 * 드라이브 호출 경계(contracts/drive-gateway.md).
 * 파일은 경로로 받아 흘려 보낸다.
 */
interface DriveApi {
    suspend fun about(): StorageQuota

    suspend fun list(query: DriveQuery, pageToken: String? = null): DrivePage

    suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>): DriveFile

    suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile

    suspend fun startResumable(meta: NewFile, size: Long): UploadSession

    /** [from]부터 조각 하나를 보낸다. */
    suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult

    /** 끊긴 뒤 어디까지 갔는지 묻는다. */
    suspend fun queryResumable(session: UploadSession): ChunkResult

    suspend fun get(fileId: String): DriveFile

    /** [fromByte]부터 받아 [to]에 이어 쓰고 파일 크기를 돌려준다. */
    suspend fun download(fileId: String, to: Path, fromByte: Long = 0): Long

    suspend fun trash(fileId: String)
}

/** 모든 쪽을 모아 읽는다. */
suspend fun DriveApi.listAll(query: DriveQuery): List<DriveFile> {
    val out = ArrayList<DriveFile>()
    var token: String? = null
    do {
        val page = list(query, token)
        out += page.files
        token = page.nextPageToken
    } while (token != null)
    return out
}

/** 5MB 이하는 한 번에, 그보다 크면 이어 올리기로 올린다. */
const val SMALL_UPLOAD_LIMIT: Long = 5L * 1024 * 1024

/**
 * 이어 올리기 조각.
 * 256KiB의 배수여야 한다.
 */
const val UPLOAD_CHUNK: Long = 8L * 1024 * 1024
