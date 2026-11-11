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
import org.sjhstudio.diary.core.platform.sha256Hex

/**
 * 저장한 사진
 * 이름은 내용 해시이고 백업 사진 이름과 같음
 */
data class StoredPhoto(val fileName: String, val sha256: String, val width: Int?, val height: Int?, val byteSize: Long?)

/**
 * 고른 사진을 앱 저장소에 넣음(research R5, FR-007)
 * 긴 변 2,560px이 넘을 때만 줄이고 방향을 바로잡아 JPEG 85로 쓴 뒤 내용 해시 이름으로 둠
 * 쓰는 동안의 파일도 사진 폴더에 둠
 * 캐시 폴더는 시스템이 지울 수 있음(헌법 I)
 * 공간이 부족하면 okio.IOException이 그대로 나감
 */
class PhotoStore(
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val encoder: ThumbnailEncoder,
    private val measure: (String) -> ImageSize?,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /** 읽을 수 없는 그림이면 null */
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
            val sha256 = fileSystem.sha256Hex(outgoing)
            val target = mediaDir / "$sha256.jpg"
            // 같은 사진이면 파일은 하나
            // 이미 있어도 새로 쓴 것으로 바꿔 고친 시각을 지금으로 둠
            // 그대로 두면 하루 지난 안 쓰는 사진을 지우는 정리가 방금 다시 고른 사진을 지울 수 있음
            fileSystem.atomicMove(outgoing, target)
            val size = measure(target.toString())
            StoredPhoto(target.name, sha256, size?.width, size?.height, fileSystem.metadata(target).size)
        } finally {
            fileSystem.delete(incoming, mustExist = false)
            fileSystem.delete(outgoing, mustExist = false)
        }
    }

    companion object {
        /** 일기 사진을 보관하는 긴 변. 옛 앱에서 옮긴 사진도 같음 */
        const val MAX_SIDE = 2560
        const val QUALITY = 85
    }
}
