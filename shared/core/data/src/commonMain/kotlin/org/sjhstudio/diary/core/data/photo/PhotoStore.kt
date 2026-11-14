package org.sjhstudio.diary.core.data.photo

import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.use
import org.sjhstudio.diary.core.model.PickedImage
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
    /**
     * 읽을 수 없는 그림이면 null
     * 원본은 조각으로 받아 바로 파일에 써서 통째로 메모리에 올리지 않음
     */
    suspend fun store(image: PickedImage): StoredPhoto? = withContext(io) {
        fileSystem.createDirectories(mediaDir)
        val token = Random.nextLong().toULong()
        val incoming = mediaDir / ".in-$token"
        val outgoing = mediaDir / ".out-$token"
        try {
            if (!receive(image, incoming)) return@withContext null
            if (!shrink(incoming, outgoing)) return@withContext null
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

    /**
     * 원본을 [incoming]에 받음. 원본을 읽다 실패하면 거짓(읽을 수 없는 그림)
     * 받아 쓰다 실패한 것은 공간 문제라 예외를 그대로 올림
     */
    private suspend fun receive(image: PickedImage, incoming: Path): Boolean {
        // 쓰는 중에 난 실패만 공간 문제. 그 밖은 원본을 읽다 난 실패
        var writing = false
        val chunkBuffer = Buffer()
        // 파일을 열지 못한 것도 쓰기 실패라 그대로 올림
        val target = fileSystem.sink(incoming)
        return try {
            // 조각마다 바로 내려 써서 쓰기 실패가 그 자리에서 드러남
            target.use { sink ->
                image.copy { chunk, length ->
                    chunkBuffer.write(chunk, 0, length)
                    writing = true
                    sink.write(chunkBuffer, chunkBuffer.size)
                    writing = false
                }
            }
            (fileSystem.metadata(incoming).size ?: 0) > 0
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            if (writing) throw error
            false
        } catch (_: Exception) {
            false
        }
    }

    // 아주 큰 그림을 줄이다 메모리가 모자라면 앱을 끄지 않고 읽을 수 없는 그림으로 봄
    private fun shrink(incoming: Path, outgoing: Path): Boolean = try {
        encoder.encode(incoming.toString(), outgoing.toString(), MAX_SIDE, QUALITY) && fileSystem.exists(outgoing)
    } catch (_: OutOfMemoryError) {
        false
    }

    companion object {
        /** 일기 사진을 보관하는 긴 변. 옛 앱에서 옮긴 사진도 같음 */
        const val MAX_SIDE = 2560
        const val QUALITY = 85
    }
}
