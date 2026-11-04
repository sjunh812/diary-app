package org.sjhstudio.diary.core.backup

import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase

/** 뜬 스냅샷. 숫자와 확인값은 목록 파일에 들어간다. */
data class Snapshot(
    val path: Path,
    val hashes: FileHashes,
    val entries: Int,
    val entriesInTrash: Int,
    val mediaRows: Int,
    val mediaMissing: Int,
    val mediaPending: Int
)

/**
 * 일기 DB를 한 시점 그대로 한 파일로 뜬다(contracts/backup-format.md "스냅샷").
 * 쓰는 중이어도 `VACUUM INTO`는 한 시점만 담고, 다음 백업이 그 뒤의 일기를 담는다(FR-012).
 */
class SnapshotWriter(private val db: DiaryDatabase, private val fileSystem: FileSystem) {
    suspend fun write(target: Path): Snapshot {
        target.parent?.let(fileSystem::createDirectories)
        fileSystem.delete(target, mustExist = false)
        db.useWriterConnection { transactor ->
            transactor.usePrepared("VACUUM INTO ?") { statement ->
                statement.bindText(1, target.toString())
                statement.step()
            }
        }
        val connection = BundledSQLiteDriver().open(target.toString())
        val counted = try {
            // 기기 것인 표는 비운다. 기준값과 임시 저장본은 다른 기기로 가면 안 된다.
            DEVICE_TABLES.forEach { connection.execSQL("DELETE FROM $it") }
            connection.execSQL("VACUUM")
            fun count(sql: String): Int = connection.prepare(sql).use { statement ->
                statement.step()
                statement.getInt(0)
            }
            Snapshot(
                path = target,
                hashes = FileHashes("", "", 0),
                entries = count("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL"),
                entriesInTrash = count("SELECT COUNT(*) FROM entry WHERE deleted_at IS NOT NULL"),
                mediaRows = count("SELECT COUNT(*) FROM media"),
                mediaMissing = count("SELECT COUNT(*) FROM media WHERE status = 'missing'"),
                mediaPending = count("SELECT COUNT(*) FROM media WHERE status = 'pending'")
            )
        } finally {
            connection.close()
        }
        return counted.copy(hashes = fileSystem.hashes(target))
    }

    companion object {
        val DEVICE_TABLES = listOf("backup_media", "sync_base", "merge_mark", "draft", "draft_media")
    }
}
