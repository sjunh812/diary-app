package org.sjhstudio.diary.core.backup

import okio.Buffer
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use

/** 파일 내용 확인값. 소문자 hex다. 드라이브의 `sha256Checksum`, `md5Checksum`과 그대로 견준다. */
data class FileHashes(val sha256: String, val md5: String, val byteSize: Long)

/** 파일을 흘려 가며 잰다. 사진 하나를 통째로 메모리에 올리지 않는다(SC-005). */
fun FileSystem.hashes(path: Path): FileHashes {
    val sha256 = HashingSink.sha256(blackholeSink())
    val md5 = HashingSink.md5(blackholeSink())
    var size = 0L
    source(path).use { raw ->
        raw.buffer().use { source ->
            val buffer = Buffer()
            while (true) {
                val read = source.read(buffer, CHUNK)
                if (read < 0) break
                size += read
                val copy = buffer.copy()
                sha256.write(buffer, read)
                md5.write(copy, read)
            }
        }
    }
    return FileHashes(sha256.hash.hex(), md5.hash.hex(), size)
}

private const val CHUNK = 64L * 1024
