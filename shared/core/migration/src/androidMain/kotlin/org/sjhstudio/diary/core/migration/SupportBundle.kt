package org.sjhstudio.diary.core.migration

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.sink
import okio.use

/**
 * 고쳐 받기 문의에 붙이는 zip(FR-010a, contracts/migration-flow.md "고쳐 받기 문의").
 * 예전 앱 일기 파일 사본(`note.db`, `-wal`, `-shm`)만 담는다. 사진과 설정 파일은 담지 않는다.
 * 설정(`pref.xml`)에는 옛 백업 계정 이메일과 드라이브 파일 ID가 있을 수 있어서다(헌법 II, 2026-11-04 사용자 결정).
 * 사용자가 "첨부해서 문의"를 고를 때만 만든다. 지우는 때는 [SupportFiles]가 정한다.
 */
class SupportBundle(private val fileSystem: FileSystem, private val paths: MigrationPaths, private val files: SupportFiles) {
    /** [fileName]으로 묶음을 새로 만들고 경로를 돌려준다. 담을 사본이 없으면 실패한다. */
    fun create(fileName: String): Path {
        val parts = PARTS.map { paths.snapshotDir / it }.filter(fileSystem::exists)
        check(parts.any { it.name == "note.db" }) { "일기 파일 사본이 없음" }
        val target = files.prepare() / fileName
        fileSystem.sink(target).buffer().outputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                for (part in parts) {
                    zip.putNextEntry(ZipEntry("legacy-snapshot/${part.name}"))
                    val sink = zip.sink().buffer()
                    fileSystem.source(part).use { sink.writeAll(it) }
                    sink.emit()
                    zip.closeEntry()
                }
            }
        }
        return target
    }

    private companion object {
        val PARTS = listOf("note.db", "note.db-wal", "note.db-shm")
    }
}
