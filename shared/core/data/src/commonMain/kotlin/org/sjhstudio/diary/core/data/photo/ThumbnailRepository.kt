package org.sjhstudio.diary.core.data.photo

import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/** 원본 사진을 줄여 JPEG로 쓴다. 플랫폼의 `ImageDecoder.writeThumbnail`이 맡는다. */
fun interface ThumbnailEncoder {
    fun encode(source: String, target: String, maxSide: Int, quality: Int): Boolean
}

/**
 * 줄임 사진(스펙 001 research R9, FR-036·FR-037). `thumbs/<sha256>.jpg`, 긴 변 480px, JPEG 80.
 * 원본에서 다시 만들 수 있는 파생 파일이라 백업에 넣지 않고, 캐시가 아닌 백업 제외 폴더에 둔다.
 * 없거나 지워졌으면 처음 찾을 때 만든다. 사진 폴더(`media/`) 밖의 파일은 맡지 않는다.
 */
class ThumbnailRepository(
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val thumbnailDir: Path,
    private val encoder: ThumbnailEncoder,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /** [media]의 줄임 사진 경로. 이름은 원본과 같은 해시다. */
    fun pathFor(media: Path): Path = thumbnailDir / "${media.name.substringBefore('.')}.$EXTENSION"

    /** 줄임 사진을 돌려준다. 없으면 원본에서 만든다. 사진 폴더 밖이거나 만들지 못하면 null. */
    suspend fun thumbnail(media: Path): Path? = withContext(io) {
        if (media.parent != mediaDir) return@withContext null
        val target = pathFor(media)
        if (fileSystem.exists(target)) return@withContext target
        if (create(media, target)) target else null
    }

    /** 이전처럼 미리 만들 때 쓴다. 이미 있거나 만들었으면 true. */
    suspend fun ensure(media: Path): Boolean = thumbnail(media) != null

    private fun create(media: Path, target: Path): Boolean {
        if (!fileSystem.exists(media)) return false
        fileSystem.createDirectories(thumbnailDir)
        // 둘이 같은 사진을 함께 만들어도 반쯤 쓴 파일이 보이지 않게 임시 파일에 쓰고 이름을 바꾼다.
        val temporary = thumbnailDir / "${target.name}.${Random.nextLong().toULong()}.tmp"
        val written = runCatching { encoder.encode(media.toString(), temporary.toString(), MAX_SIDE, QUALITY) }.getOrDefault(false)
        if (!written || !fileSystem.exists(temporary)) {
            fileSystem.delete(temporary, mustExist = false)
            return false
        }
        fileSystem.atomicMove(temporary, target)
        return true
    }

    companion object {
        /** 목록 사진 칸 64dp는 4배 밀도에서 256px, 폰 격자 칸 약 130dp는 3배에서 390px라 480이면 충분하다(R9). */
        const val MAX_SIDE = 480
        const val QUALITY = 80
        private const val EXTENSION = "jpg"
    }
}
