package org.sjhstudio.diary.core.backup

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
fun FileSystem.gzip(from: Path, to: Path) {
    delete(to, mustExist = false)
    source(from).use { source ->
        sink(to).gzip().buffer().use { it.writeAll(source) }
    }
}

/**
 * 끝의 CRC와 길이가 맞지 않거나 덜 받은 파일이면 IOException
 * 반쯤 풀린 파일은 남기지 않음
 */
fun FileSystem.gunzip(from: Path, to: Path) {
    delete(to, mustExist = false)
    try {
        source(from).gzip().buffer().use { source ->
            sink(to).buffer().use { it.writeAll(source) }
        }
    } catch (e: IOException) {
        delete(to, mustExist = false)
        throw e
    }
}
