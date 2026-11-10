package org.sjhstudio.diary.core.migration

import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.platform.sha256Hex

/**
 * 원본 파일 하나의 크기와 SHA-256(`snapshot.json`).
 * 이전 전후로 원본이 같은지 시험에서 견줌.
 */
@Serializable
data class SnapshotFile(val name: String, val size: Long, val sha256: String)

@Serializable
data class SnapshotManifest(val createdAtMillis: Long, val files: List<SnapshotFile>)

/**
 * 원본을 `legacy-snapshot/`에 바이트 그대로 복사(contracts/migration-flow.md `Snapshotting`, research R5).
 * 원본에는 SQLite 연결을 열지 않음.
 * 연결을 열었다 닫으면 WAL이 본 파일에 합쳐져 원본이 바뀌기 때문.
 * 설정 사본에서는 비밀번호 줄을 뺌.
 */
class LegacySnapshotter(private val fileSystem: FileSystem, private val clock: Clock) {
    /** 같은 원본으로 만든 사본이 이미 있으면 다시 복사하지 않음(다시 시도) */
    fun snapshot(legacy: LegacyFiles, snapshotDir: Path): SnapshotManifest {
        fileSystem.createDirectories(snapshotDir)
        val manifestPath = snapshotDir / MANIFEST
        val originals = listOf(
            legacy.noteDb,
            legacy.noteDbWal,
            legacy.noteDbShm,
            legacy.prefXml,
            legacy.pref2Xml
        ).filter(fileSystem::exists)
        readManifest(manifestPath)?.let { old ->
            if (sameAsOriginals(old, originals) && old.files.all { fileSystem.exists(snapshotDir / it.name) }) return old
        }
        val files =
            originals.map { source ->
                val target = snapshotDir / source.name
                if (source == legacy.prefXml) {
                    val text = fileSystem.read(source) { readUtf8() }
                    writeAtomically(target) { writeUtf8(LegacyPrefs.withoutPassword(text)) }
                    SnapshotFile(source.name, fileSystem.metadata(source).size ?: 0, sha256(source))
                } else {
                    copyAndHash(source, target)
                }
            }
        val manifest = SnapshotManifest(clock.now().toEpochMilliseconds(), files)
        writeAtomically(manifestPath) { writeUtf8(JSON.encodeToString(SnapshotManifest.serializer(), manifest)) }
        return manifest
    }

    fun readManifest(path: Path): SnapshotManifest? = if (!fileSystem.exists(path)) {
        null
    } else {
        runCatching { JSON.decodeFromString(SnapshotManifest.serializer(), fileSystem.read(path) { readUtf8() }) }.getOrNull()
    }

    /** 원본 파일의 SHA-256. 원본은 읽기만 함 */
    fun sha256(path: Path): String = fileSystem.sha256Hex(path)

    private fun sameAsOriginals(manifest: SnapshotManifest, originals: List<Path>): Boolean {
        if (manifest.files.map { it.name }.toSet() != originals.map { it.name }.toSet()) return false
        return originals.all { path ->
            val recorded = manifest.files.first { it.name == path.name }
            recorded.size == (fileSystem.metadata(path).size ?: -1) && recorded.sha256 == sha256(path)
        }
    }

    /** 스트림으로 복사하며 원본 해시를 재고, 사본 해시가 같은지 다시 봄 */
    private fun copyAndHash(source: Path, target: Path): SnapshotFile {
        val temp = target.parent!! / "${target.name}.tmp"
        val hashing = HashingSink.sha256(fileSystem.sink(temp))
        hashing.buffer().use { sink -> fileSystem.source(source).use { sink.writeAll(it) } }
        val hash = hashing.hash.hex()
        check(sha256(temp) == hash) { "사본 해시가 원본과 다르다: ${source.name}" }
        fileSystem.atomicMove(temp, target)
        return SnapshotFile(source.name, fileSystem.metadata(target).size ?: 0, hash)
    }

    private fun writeAtomically(target: Path, write: okio.BufferedSink.() -> Unit) {
        val temp = target.parent!! / "${target.name}.tmp"
        fileSystem.write(temp) { write() }
        fileSystem.atomicMove(temp, target)
    }

    companion object {
        const val MANIFEST = "snapshot.json"
        internal val JSON = Json { ignoreUnknownKeys = true }
    }
}
