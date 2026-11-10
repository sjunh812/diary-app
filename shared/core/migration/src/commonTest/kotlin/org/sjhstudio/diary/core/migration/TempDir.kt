package org.sjhstudio.diary.core.migration

import kotlin.random.Random
import okio.FileSystem
import okio.Path

/**
 * 시험마다 새 임시 폴더.
 * 끝나면 지움.
 */
class TempDir : AutoCloseable {
    val fs: FileSystem = FileSystem.SYSTEM
    val root: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "diary-migration-${Random.nextLong().toULong()}"

    init {
        fs.createDirectories(root)
    }

    fun write(relative: String, bytes: ByteArray): Path {
        val path = root / relative
        fs.createDirectories(path.parent!!)
        fs.write(path) { write(bytes) }
        return path
    }

    fun write(relative: String, text: String): Path = write(relative, text.encodeToByteArray())

    override fun close() {
        fs.deleteRecursively(root)
    }
}
