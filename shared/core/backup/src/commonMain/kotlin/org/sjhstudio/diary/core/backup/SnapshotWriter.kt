package org.sjhstudio.diary.core.backup

import androidx.room3.useWriterConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.model.MediaStatus

/**
 * 뜬 스냅샷
 * 숫자와 확인값은 목록 파일에 들어감
 */
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
 * 일기 DB를 한 시점 그대로 한 파일로 뜸(contracts/backup-format.md "스냅샷")
 * 쓰는 중이어도 `VACUUM INTO`는 한 시점만 담고, 다음 백업이 그 뒤의 일기를 담음(FR-012)
 */
class SnapshotWriter(private val db: DiaryDatabase, private val fileSystem: FileSystem) {
    /**
     * 기기 것인 표까지 그대로 둔 사본
     * 되돌리기용
     */
    suspend fun copyInto(target: Path) {
        target.parent?.let(fileSystem::createDirectories)
        fileSystem.delete(target, mustExist = false)
        db.useWriterConnection { transactor ->
            transactor.usePrepared("VACUUM INTO ?") { statement ->
                statement.bindText(1, target.toString())
                statement.step()
            }
        }
    }

    suspend fun write(target: Path): Snapshot {
        copyInto(target)
        val connection = BundledSQLiteDriver().open(target.toString())
        val counted = try {
            // 기기 것인 표는 비움
            // 기준값과 임시 저장본은 다른 기기로 가면 안 됨
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
                mediaMissing = count("SELECT COUNT(*) FROM media WHERE status = '${MediaStatus.MISSING.id}'"),
                mediaPending = count("SELECT COUNT(*) FROM media WHERE status = '${MediaStatus.PENDING.id}'")
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

/**
 * [snapshot]에 담은 일기마다 "이 값까지 밖에 내보냈다"고 적음
 * 다음 합치기가 양쪽에서 고쳤는지, 한쪽만 고쳤는지를 이 값으로 가름
 * 스냅샷을 뜬 뒤에 영구 삭제한 일기는 적을 곳이 없어 뺌
 */
internal suspend fun DiaryDatabase.rememberBases(snapshot: Path) {
    val connection = BundledSQLiteDriver().open(snapshot.toString())
    val bases =
        try {
            connection.prepare("SELECT id, updated_at FROM entry").use { statement ->
                buildList {
                    while (statement.step()) add(SyncBaseEntity(statement.getText(0), statement.getLong(1)))
                }
            }
        } finally {
            connection.close()
        }
    withWriteTransaction {
        val dao = backupDao()
        val here = dao.entryIds().toHashSet()
        dao.setBase(bases.filter { it.entryId in here })
    }
}
