package org.sjhstudio.diary.core.migration

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem
import okio.buffer
import okio.sink
import okio.use

/**
 * 원본 파일 내보내기(FR-009, contracts/migration-flow.md "원본 내보내기").
 * 사용자가 저장 창에서 고른 곳에 zip을 쓴다. 담는 것: `legacy-snapshot/`의 파일 전부(설정은 비밀번호 줄을 뺀 사본),
 * `media/`의 이전 사진, `README.txt`. 원본에는 손대지 않는다.
 */
class OriginalExporter(private val fileSystem: FileSystem, private val paths: MigrationPaths) {
    fun write(out: OutputStream, readme: String) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(readme.encodeToByteArray())
            zip.closeEntry()
            if (fileSystem.exists(paths.snapshotDir)) {
                for (file in fileSystem.list(paths.snapshotDir).filter {
                    fileSystem.metadataOrNull(it)?.isRegularFile == true &&
                        !it.name.endsWith(".tmp")
                }) {
                    add(zip, "legacy-snapshot/${file.name}", file)
                }
            }
            for (name in legacyPhotoFiles()) add(zip, "media/$name", paths.mediaDir / name)
        }
    }

    /** `photos.json`에 적힌 이전 사진만 담는다. 새로 붙인 사진은 이전과 상관없다. */
    private fun legacyPhotoFiles(): List<String> {
        if (!fileSystem.exists(paths.photosJson) || !fileSystem.exists(paths.mediaDir)) return emptyList()
        val hashes =
            LegacySnapshotter.JSON
                .decodeFromString(ListSerializer(PhotoRecord.serializer()), fileSystem.read(paths.photosJson) { readUtf8() })
                .mapNotNull { it.sha256 }
                .toSet()
        return fileSystem.list(paths.mediaDir).map { it.name }.filter { it.substringBefore('.') in hashes }.sorted()
    }

    private fun add(zip: ZipOutputStream, name: String, file: okio.Path) {
        zip.putNextEntry(ZipEntry(name))
        val sink = zip.sink().buffer()
        fileSystem.source(file).use { sink.writeAll(it) }
        sink.emit()
        zip.closeEntry()
    }
}
