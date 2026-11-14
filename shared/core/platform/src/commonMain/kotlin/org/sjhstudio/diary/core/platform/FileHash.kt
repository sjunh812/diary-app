package org.sjhstudio.diary.core.platform

import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use

/** 파일을 스트림으로 읽어 잰 SHA-256(소문자 hex). 큰 사진도 메모리에 다 올리지 않음 */
fun FileSystem.sha256Hex(path: Path): String {
    val sink = HashingSink.sha256(blackholeSink())
    sink.buffer().use { out -> source(path).use { out.writeAll(it) } }
    return sink.hash.hex()
}
