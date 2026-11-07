package org.sjhstudio.diary.core.migration

import okio.FileSystem
import okio.Path

/**
 * 새 DB로 옮긴 옛 일기 번호.
 * 다시 읽을 때 표에 없는 일기가 못 옮긴 일기인지, 옮긴 뒤 삭제한 일기인지 가른다.
 * 파일이 없거나 깨졌으면 빈 값이다.
 */
internal class MovedNumbers(private val fileSystem: FileSystem, private val directory: Path) {
    private val file = directory / "moved-numbers.txt"

    fun read(): Set<Int> = runCatching {
        if (!fileSystem.exists(file)) return@runCatching emptySet()
        fileSystem.read(file) { readUtf8() }.split(',').mapNotNullTo(HashSet()) { it.toIntOrNull() }
    }.getOrDefault(emptySet())

    /** 지운 일기의 번호가 빠지지 않게 있던 값에 더한다. */
    fun add(numbers: Collection<Int>) {
        val all = read() + numbers
        runCatching {
            val temp = directory / "moved-numbers.tmp"
            fileSystem.createDirectories(directory)
            fileSystem.write(temp) { writeUtf8(all.joinToString(",")) }
            fileSystem.atomicMove(temp, file)
        }
    }
}
