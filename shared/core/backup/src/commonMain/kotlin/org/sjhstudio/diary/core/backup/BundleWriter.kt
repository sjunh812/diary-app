package org.sjhstudio.diary.core.backup

import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/** 묶음 파일의 한 항목. [name]은 zip 안의 이름이다. */
class BundleItem(val name: String, val path: Path)

/**
 * 압축 없는(STORE) zip을 흘려 쓴다(research R18). 사진은 이미 줄어 있어 압축 이득이 작다.
 * 저장 위치가 흐름뿐이어도 되도록 뒤로 돌아가 쓰지 않는다. 크기와 CRC를 머리에 쓰려고 파일을 먼저 한 번 읽는다.
 * [zip64From]은 시험에서 큰 파일 없이 ZIP64를 보려고 낮춘다.
 */
class BundleWriter(private val fileSystem: FileSystem, private val zip64From: Long = UINT_MAX, private val zip64Count: Int = USHORT_MAX) {
    private class Written(val name: ByteArray, val crc: Int, val size: Long, val offset: Long)

    fun write(sink: BufferedSink, items: List<BundleItem>, onItem: (done: Int) -> Unit = {}) {
        val written = ArrayList<Written>(items.size)
        var offset = 0L
        items.forEachIndexed { index, item ->
            val (crc, size) = measure(item.path)
            val name = item.name.encodeToByteArray()
            offset += local(sink, name, crc, size)
            copy(item.path, sink)
            written += Written(name, crc, size, offset - localSize(name, size))
            offset += size
            onItem(index + 1)
        }
        val directoryAt = offset
        var directorySize = 0L
        for (entry in written) directorySize += central(sink, entry)
        end(sink, written.size, directorySize, directoryAt)
        sink.flush()
    }

    private fun measure(path: Path): Pair<Int, Long> {
        var crc = -1
        var size = 0L
        val buffer = ByteArray(CHUNK)
        fileSystem.source(path).buffer().use { source ->
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                for (i in 0 until read) crc = CRC_TABLE[(crc xor buffer[i].toInt()) and 0xFF] xor (crc ushr 8)
                size += read
            }
        }
        return crc.inv() to size
    }

    private fun copy(path: Path, sink: BufferedSink) {
        fileSystem.source(path).use { sink.writeAll(it) }
    }

    private fun isLarge(value: Long) = value >= zip64From

    private fun localExtra(size: Long) = if (isLarge(size)) ZIP64_LOCAL_EXTRA else 0

    private fun localSize(name: ByteArray, size: Long) = LOCAL_HEADER + name.size + localExtra(size).toLong()

    private fun local(sink: BufferedSink, name: ByteArray, crc: Int, size: Long): Long {
        val large = isLarge(size)
        sink.writeIntLe(LOCAL_SIGNATURE)
        sink.writeShortLe(VERSION)
        sink.writeShortLe(UTF8_FLAG)
        sink.writeShortLe(STORED)
        sink.writeShortLe(0)
        sink.writeShortLe(DOS_DATE)
        sink.writeIntLe(crc)
        sink.writeIntLe(if (large) -1 else size.toInt())
        sink.writeIntLe(if (large) -1 else size.toInt())
        sink.writeShortLe(name.size)
        sink.writeShortLe(localExtra(size))
        sink.write(name)
        if (large) {
            sink.writeShortLe(ZIP64_TAG)
            sink.writeShortLe(ZIP64_LOCAL_EXTRA - EXTRA_HEADER)
            sink.writeLongLe(size)
            sink.writeLongLe(size)
        }
        return localSize(name, size)
    }

    private fun central(sink: BufferedSink, entry: Written): Long {
        val largeSize = isLarge(entry.size)
        val largeOffset = isLarge(entry.offset)
        val extra = Buffer()
        if (largeSize) {
            extra.writeLongLe(entry.size)
            extra.writeLongLe(entry.size)
        }
        if (largeOffset) extra.writeLongLe(entry.offset)
        val extraSize = if (extra.size > 0) (extra.size + EXTRA_HEADER).toInt() else 0
        sink.writeIntLe(CENTRAL_SIGNATURE)
        sink.writeShortLe(VERSION)
        sink.writeShortLe(VERSION)
        sink.writeShortLe(UTF8_FLAG)
        sink.writeShortLe(STORED)
        sink.writeShortLe(0)
        sink.writeShortLe(DOS_DATE)
        sink.writeIntLe(entry.crc)
        sink.writeIntLe(if (largeSize) -1 else entry.size.toInt())
        sink.writeIntLe(if (largeSize) -1 else entry.size.toInt())
        sink.writeShortLe(entry.name.size)
        sink.writeShortLe(extraSize)
        sink.writeShortLe(0)
        sink.writeShortLe(0)
        sink.writeShortLe(0)
        sink.writeIntLe(0)
        sink.writeIntLe(if (largeOffset) -1 else entry.offset.toInt())
        sink.write(entry.name)
        if (extraSize > 0) {
            sink.writeShortLe(ZIP64_TAG)
            sink.writeShortLe(extra.size.toInt())
            sink.writeAll(extra)
        }
        return CENTRAL_HEADER + entry.name.size + extraSize.toLong()
    }

    private fun end(sink: BufferedSink, count: Int, directorySize: Long, directoryAt: Long) {
        val large = count >= zip64Count || isLarge(directoryAt) || isLarge(directorySize)
        if (large) {
            sink.writeIntLe(ZIP64_END_SIGNATURE)
            sink.writeLongLe(ZIP64_END_BODY)
            sink.writeShortLe(VERSION)
            sink.writeShortLe(VERSION)
            sink.writeIntLe(0)
            sink.writeIntLe(0)
            sink.writeLongLe(count.toLong())
            sink.writeLongLe(count.toLong())
            sink.writeLongLe(directorySize)
            sink.writeLongLe(directoryAt)
            sink.writeIntLe(ZIP64_LOCATOR_SIGNATURE)
            sink.writeIntLe(0)
            sink.writeLongLe(directoryAt + directorySize)
            sink.writeIntLe(1)
        }
        sink.writeIntLe(END_SIGNATURE)
        sink.writeShortLe(0)
        sink.writeShortLe(0)
        sink.writeShortLe(if (large) USHORT_MAX else count)
        sink.writeShortLe(if (large) USHORT_MAX else count)
        sink.writeIntLe(if (large) -1 else directorySize.toInt())
        sink.writeIntLe(if (large) -1 else directoryAt.toInt())
        sink.writeShortLe(0)
    }

    private companion object {
        const val UINT_MAX = 0xFFFFFFFFL
        const val USHORT_MAX = 0xFFFF
        const val CHUNK = 64 * 1024
        const val LOCAL_SIGNATURE = 0x04034b50
        const val CENTRAL_SIGNATURE = 0x02014b50
        const val END_SIGNATURE = 0x06054b50
        const val ZIP64_END_SIGNATURE = 0x06064b50
        const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
        const val ZIP64_END_BODY = 44L
        const val ZIP64_TAG = 1
        const val EXTRA_HEADER = 4
        const val ZIP64_LOCAL_EXTRA = EXTRA_HEADER + 16
        const val LOCAL_HEADER = 30L
        const val CENTRAL_HEADER = 46L
        const val VERSION = 45
        const val UTF8_FLAG = 0x0800
        const val STORED = 0

        // zip은 1980-01-01보다 앞을 쓰지 못한다. 시각은 쓰지 않는다.
        const val DOS_DATE = 0x21

        val CRC_TABLE = IntArray(256) { index ->
            var value = index
            repeat(8) { value = if (value and 1 != 0) (value ushr 1) xor -306674912 else value ushr 1 }
            value
        }
    }
}
