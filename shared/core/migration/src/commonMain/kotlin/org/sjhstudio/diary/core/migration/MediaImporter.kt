package org.sjhstudio.diary.core.migration

import kotlin.time.Clock
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.platform.sha256Hex

/**
 * EXIF 방향을 반영한 사진 가로세로
 * 읽지 못하면 null(Android `BitmapFactory` + `ExifInterface`)
 */
fun interface ImageSizeReader {
    fun read(path: String): Pair<Int, Int>?
}

/**
 * `photos.json` 한 줄: 레거시 경로 → 해시(research R8)
 * 사진 자체는 `media/`에 있음
 */
@Serializable
data class PhotoRecord(val path: String, val sha256: String?, val size: Long?)

/** 사진 조각 하나를 옮긴 결과 */
data class ImportedPhoto(val media: Media, val record: PhotoRecord) {
    val found: Boolean get() = media.status == MediaStatus.NORMAL
}

/**
 * 옛 앱 사진을 `media/<sha256>.<ext>`로 한 번만 복사(contracts/legacy-mapping.md "사진", research R8)
 * 원본(캐시 사진)은 읽기만 함
 * 긴 변이 [PhotoStore.MAX_SIDE]를 넘는 사진은 새로 붙인 사진처럼 줄인 사본을 둠. 백업이 드라이브를 덜 씀
 * 줄이지 못하면 원본 그대로 복사해 사진을 잃지 않음
 * 임시 이름으로 쓰고 해시를 다시 잰 뒤 이름을 바꿈
 * 같은 해시가 이미 있으면 복사하지 않음(같은 파일 두 번, 다시 시도)
 * 이전이 실패해도 `media/`의 사본은 지우지 않음
 */
class MediaImporter(
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val imageSize: ImageSizeReader,
    private val clock: Clock,
    private val newId: () -> String,
    /** 큰 사진을 줄이는 인코더. 없으면 모두 그대로 복사 */
    private val shrink: ThumbnailEncoder? = null
) {
    /** [known]은 앞선 시도가 적어 둔 목록. 원본이 사라졌으면 여기 적힌 사본을 씀 */
    fun import(entryId: String, fragments: List<String>, known: Map<String, PhotoRecord> = emptyMap()): List<ImportedPhoto> =
        fragments.mapIndexed { order, fragment -> importOne(entryId, order, fragment, known[fragment]?.sha256) }

    /**
     * 조각 하나
     * 찾지 못하거나 읽다 실패하면 `missing` 줄
     */
    fun importOne(entryId: String, sortOrder: Int, fragment: String, knownSha256: String? = null): ImportedPhoto {
        val now = clock.now()
        val copied =
            PhotoPaths.candidates(fragment).firstNotNullOfOrNull { candidate -> runCatching { copy(candidate.toPath()) }.getOrNull() }
                ?: knownSha256?.let { sha256 -> runCatching { earlierCopy(sha256, fragment) }.getOrNull() }
        val media =
            Media(
                id = newId(),
                entryId = entryId,
                sortOrder = sortOrder,
                status = if (copied != null) MediaStatus.NORMAL else MediaStatus.MISSING,
                fileName = copied?.fileName,
                sha256 = copied?.sha256,
                width = copied?.size?.first,
                height = copied?.size?.second,
                byteSize = copied?.bytes,
                legacyPath = fragment,
                createdAt = now
            )
        return ImportedPhoto(media, PhotoRecord(path = fragment, sha256 = copied?.sha256, size = copied?.bytes))
    }

    /** 다시 잰 해시가 파일 이름과 같은지(검증 단계) */
    fun verify(fileName: String): Boolean {
        val path = mediaDir / fileName
        return fileSystem.exists(path) && sha256(path) == fileName.substringBefore('.')
    }

    private class Copied(val fileName: String, val sha256: String, val bytes: Long, val size: Pair<Int, Int>?)

    /**
     * 앞선 시도가 옮겨 둔 사본. 캐시의 원본은 그사이 OS가 지울 수 있음(헌법 I)
     * 이름에 든 해시와 내용이 맞을 때만 씀
     */
    private fun earlierCopy(sha256: String, fragment: String): Copied? {
        val name = PhotoPaths.extensions(fragment).map { PhotoPaths.fileName(sha256, it) }.firstOrNull(::verify) ?: return null
        val target = mediaDir / name
        return Copied(name, sha256, fileSystem.metadata(target).size ?: 0, imageSize.read(target.toString()))
    }

    private fun copy(source: Path): Copied? {
        val meta = fileSystem.metadataOrNull(source) ?: return null
        if (!meta.isRegularFile) return null
        fileSystem.createDirectories(mediaDir)
        // 줄이다 예외가 나도 사진을 잃지 않게 원본 복사로 넘어감
        runCatching { shrunk(source) }.getOrNull()?.let { return it }
        val head = fileSystem.read(source) { readByteArray(minOf(HEAD_BYTES, meta.size ?: HEAD_BYTES)) }
        val temp = mediaDir / ".import-${newId()}.tmp"
        val hashing = HashingSink.sha256(fileSystem.sink(temp))
        try {
            hashing.buffer().use { sink -> fileSystem.source(source).use { sink.writeAll(it) } }
            val sha = hashing.hash.hex()
            val name = PhotoPaths.fileName(sha, PhotoPaths.extension(source.toString(), head))
            val target = mediaDir / name
            if (fileSystem.exists(target) && sha256(target) == sha) {
                fileSystem.delete(temp)
            } else {
                check(sha256(temp) == sha) { "복사본 해시가 다르다" }
                fileSystem.atomicMove(temp, target)
            }
            return Copied(name, sha, fileSystem.metadata(target).size ?: 0, imageSize.read(target.toString()))
        } finally {
            if (fileSystem.exists(temp)) fileSystem.delete(temp)
        }
    }

    /** 큰 사진이면 줄인 JPEG 사본. 작거나 줄이지 못하면 null(그대로 복사) */
    private fun shrunk(source: Path): Copied? {
        val encoder = shrink?.takeIf { isLarge(source) } ?: return null
        val temp = mediaDir / ".import-${newId()}.jpg"
        try {
            val written = encoder.encode(source.toString(), temp.toString(), PhotoStore.MAX_SIDE, PhotoStore.QUALITY)
            val bytes = fileSystem.metadataOrNull(temp)?.size ?: 0
            val size = if (written && bytes > 0) imageSize.read(temp.toString()) else null
            return size?.let { keep(temp, bytes, it) }
        } finally {
            if (fileSystem.exists(temp)) fileSystem.delete(temp)
        }
    }

    private fun isLarge(source: Path): Boolean =
        imageSize.read(source.toString())?.let { (w, h) -> maxOf(w, h) > PhotoStore.MAX_SIDE } == true

    /** 다 쓴 사본을 해시 이름으로 둠. 같은 사진이 이미 있으면 그것을 씀 */
    private fun keep(temp: Path, bytes: Long, size: Pair<Int, Int>): Copied {
        val sha = sha256(temp)
        val name = PhotoPaths.fileName(sha, "jpg")
        val target = mediaDir / name
        if (fileSystem.exists(target) && sha256(target) == sha) fileSystem.delete(temp) else fileSystem.atomicMove(temp, target)
        return Copied(name, sha, fileSystem.metadata(target).size ?: bytes, size)
    }

    private fun sha256(path: Path): String = fileSystem.sha256Hex(path)

    private companion object {
        const val HEAD_BYTES = 16L
    }
}
