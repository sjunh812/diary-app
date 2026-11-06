package org.sjhstudio.diary.core.data.photo

import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.platform.ImageSize

/**
 * 저장한 사진.
 * 이름은 내용 해시이고 백업 사진 이름과 같다.
 */
data class StoredPhoto(val fileName: String, val sha256: String, val width: Int?, val height: Int?, val byteSize: Long?)

/**
 * 고른 사진을 앱 저장소에 넣는다(research R5, FR-007).
 * 긴 변 2,560px이 넘을 때만 줄이고 방향을 바로잡아 JPEG 85로 쓴 뒤 내용 해시 이름으로 둔다.
 * 쓰는 동안의 파일도 사진 폴더에 둔다.
 * 캐시 폴더는 시스템이 지울 수 있다(헌법 I).
 * 공간이 모자라면 okio.IOException이 그대로 나간다.
 */
class PhotoStore(
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val encoder: ThumbnailEncoder,
    private val measure: (String) -> ImageSize?,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /** 읽을 수 없는 그림이면 null이다. */
    suspend fun store(bytes: ByteArray): StoredPhoto? = withContext(io) {
        fileSystem.createDirectories(mediaDir)
        val token = Random.nextLong().toULong()
        val incoming = mediaDir / ".in-$token"
        val outgoing = mediaDir / ".out-$token"
        try {
            fileSystem.write(incoming) { write(bytes) }
            if (!encoder.encode(incoming.toString(), outgoing.toString(), MAX_SIDE, QUALITY) || !fileSystem.exists(outgoing)) {
                return@withContext null
            }
            val sha256 = sha256Of(outgoing)
            val target = mediaDir / "$sha256.jpg"
            // 같은 사진이면 파일은 하나다.
            if (fileSystem.exists(target)) fileSystem.delete(outgoing) else fileSystem.atomicMove(outgoing, target)
            val size = measure(target.toString())
            StoredPhoto(target.name, sha256, size?.width, size?.height, fileSystem.metadata(target).size)
        } finally {
            fileSystem.delete(incoming, mustExist = false)
            fileSystem.delete(outgoing, mustExist = false)
        }
    }

    private fun sha256Of(file: Path): String {
        val sink = HashingSink.sha256(blackholeSink())
        sink.buffer().use { out -> fileSystem.source(file).use { out.writeAll(it) } }
        return sink.hash.hex()
    }

    private companion object {
        const val MAX_SIDE = 2560
        const val QUALITY = 85
    }
}
