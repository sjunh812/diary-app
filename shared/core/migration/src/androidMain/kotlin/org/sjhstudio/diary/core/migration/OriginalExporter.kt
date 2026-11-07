package org.sjhstudio.diary.core.migration

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.sink
import okio.use

/**
 * 원본 파일 내보내기(FR-009, contracts/migration-flow.md "원본 내보내기").
 * 사용자가 저장 창에서 고른 곳에 zip을 쓴다.
 * 담는 것: `legacy-snapshot/`의 파일 전부(설정은 비밀번호 줄을 뺀 사본),
 * `media/`의 이전 사진, `README.txt`.
 * 사본을 만들기 전에 실패한 기기는 사본에 없는 파일을 원본에서 읽어 같은 자리에 담는다.
 * 원본은 읽기만 하고 SQLite 연결을 열지 않는다.
 * 담을 파일이 하나도 없으면 안내문만 든 묶음을 내보냈다고 하지 않게 실패한다.
 */
class OriginalExporter(private val fileSystem: FileSystem, private val paths: MigrationPaths) {
    fun write(out: OutputStream, readme: String) {
        val copies = copies()
        val originals = originals(copies.map { it.name }.toSet())
        check(copies.isNotEmpty() || originals.isNotEmpty()) { "담을 파일이 없다" }
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(readme.encodeToByteArray())
            zip.closeEntry()
            for (file in copies) add(zip, "$FOLDER/${file.name}", file)
            for (file in originals) addOriginal(zip, file)
            for (name in legacyPhotoFiles()) add(zip, "media/$name", paths.mediaDir / name)
        }
    }

    private fun copies(): List<Path> = if (fileSystem.exists(paths.snapshotDir)) {
        fileSystem.list(paths.snapshotDir).filter { fileSystem.metadataOrNull(it)?.isRegularFile == true && !it.name.endsWith(".tmp") }
    } else {
        emptyList()
    }

    private fun originals(copied: Set<String>): List<Path> = with(paths.legacy) {
        listOf(noteDb, noteDbWal, noteDbShm, prefXml, pref2Xml).filter { it.name !in copied && fileSystem.exists(it) }
    }

    /** 사본처럼 설정에서 비밀번호 줄을 뺀다. */
    private fun addOriginal(zip: ZipOutputStream, file: Path) {
        if (file != paths.legacy.prefXml) {
            add(zip, "$FOLDER/${file.name}", file)
            return
        }
        zip.putNextEntry(ZipEntry("$FOLDER/${file.name}"))
        zip.write(LegacyPrefs.withoutPassword(fileSystem.read(file) { readUtf8() }).encodeToByteArray())
        zip.closeEntry()
    }

    /**
     * `photos.json`에 적힌 이전 사진만 담는다.
     * 새로 붙인 사진은 이전과 상관없다.
     */
    private fun legacyPhotoFiles(): List<String> {
        if (!fileSystem.exists(paths.photosJson) || !fileSystem.exists(paths.mediaDir)) return emptyList()
        val hashes =
            LegacySnapshotter.JSON
                .decodeFromString(ListSerializer(PhotoRecord.serializer()), fileSystem.read(paths.photosJson) { readUtf8() })
                .mapNotNull { it.sha256 }
                .toSet()
        return fileSystem.list(paths.mediaDir).map { it.name }.filter { it.substringBefore('.') in hashes }.sorted()
    }

    private fun add(zip: ZipOutputStream, name: String, file: Path) {
        zip.putNextEntry(ZipEntry(name))
        val sink = zip.sink().buffer()
        fileSystem.source(file).use { sink.writeAll(it) }
        sink.emit()
        zip.closeEntry()
    }

    private companion object {
        const val FOLDER = "legacy-snapshot"
    }
}
