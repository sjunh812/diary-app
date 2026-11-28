package org.sjhstudio.diary.core.backup

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.gzip
import okio.use

/**
 * 드라이브에 올리는 스냅샷은 gzip으로 줄임(형식 3)
 * 일기 글은 잘 줄어 올리는 양과 드라이브에 쌓이는 양이 함께 줄어듦
 */
internal fun FileSystem.gzip(from: Path, to: Path) {
    delete(to, mustExist = false)
    source(from).use { source ->
        sink(to).gzip().buffer().use { it.writeAll(source) }
    }
}

/**
 * 끝의 CRC와 길이가 맞지 않거나 덜 받은 파일이면 [CorruptPack]
 * 쓰다 난 오류(저장 공간 부족 같은)는 그대로 올려 깨진 백업과 가름
 * 반쯤 풀린 파일은 남기지 않음
 */
internal fun FileSystem.gunzip(from: Path, to: Path) {
    delete(to, mustExist = false)
    try {
        source(from).gzip().buffer().use { source ->
            sink(to).buffer().use { out -> copyUnpacked(source, out) }
        }
    } catch (e: IOException) {
        delete(to, mustExist = false)
        throw e
    }
}

/** 읽다 난 오류만 [CorruptPack]으로 바꿈 */
private fun copyUnpacked(source: BufferedSource, out: BufferedSink) {
    val chunk = Buffer()
    while (true) {
        val read =
            try {
                source.read(chunk, CHUNK_BYTES)
            } catch (e: IOException) {
                throw CorruptPack(e)
            }
        if (read == -1L) return
        out.write(chunk, read)
    }
}

/** 받은 줄인 파일이 풀리지 않음. 확인값은 맞았으니 백업 자체가 깨진 것 */
internal class CorruptPack(cause: IOException) : IOException(cause.message, cause)

private const val CHUNK_BYTES = 64L * 1024
