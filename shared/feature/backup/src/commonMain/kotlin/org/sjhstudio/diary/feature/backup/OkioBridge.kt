package org.sjhstudio.diary.feature.backup

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout

/**
 * FileKit은 kotlinx-io, 백업 코드는 Okio를 쓴다.
 * 파일을 통째로 메모리에 올리지 않고 스트리밍으로 보낸다.
 */
internal class OkioSink(private val raw: RawSink) : Sink {
    override fun write(source: Buffer, byteCount: Long) {
        val chunk = kotlinx.io.Buffer()
        var left = byteCount
        while (left > 0) {
            val size = minOf(left, CHUNK)
            chunk.write(source.readByteArray(size))
            raw.write(chunk, size)
            left -= size
        }
    }

    override fun flush() = raw.flush()

    override fun timeout() = Timeout.NONE

    override fun close() = raw.close()
}

internal class OkioSource(private val raw: RawSource) : Source {
    override fun read(sink: Buffer, byteCount: Long): Long {
        val chunk = kotlinx.io.Buffer()
        val read = raw.readAtMostTo(chunk, minOf(byteCount, CHUNK))
        if (read > 0) sink.write(chunk.readByteArray())
        return read
    }

    override fun timeout() = Timeout.NONE

    override fun close() = raw.close()
}

private const val CHUNK = 64 * 1024L
