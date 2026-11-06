package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_media WHERE account_key = :accountKey")
    suspend fun confirmed(accountKey: String): List<BackupMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun confirm(media: List<BackupMediaEntity>)

    @Query("DELETE FROM backup_media WHERE account_key = :accountKey AND sha256 IN (:hashes)")
    suspend fun forget(accountKey: String, hashes: List<String>)

    /** 아직 올리지 못한 사진 파일 수. 기기에 파일이 있는 것만 센다. */
    @Query(
        "SELECT COUNT(DISTINCT m.sha256) FROM media m WHERE m.status = 'normal' AND m.sha256 IS NOT NULL " +
            "AND m.sha256 NOT IN (SELECT sha256 FROM backup_media WHERE account_key = :accountKey)"
    )
    fun notBackedUp(accountKey: String): Flow<Int>

    /** 올릴 사진. 공간이 모자라면 앞에서부터 들어가는 만큼 올리도록 최근 일기의 사진이 먼저다(FR-018). */
    @Query(
        "SELECT m.sha256 AS sha256, MIN(m.file_name) AS fileName, MAX(m.byte_size) AS byteSize FROM media m " +
            "JOIN entry e ON e.id = m.entry_id WHERE m.status = 'normal' AND m.sha256 IS NOT NULL " +
            "GROUP BY m.sha256 ORDER BY MAX(e.date || e.time) DESC"
    )
    suspend fun localPhotos(): List<LocalPhotoRow>

    @Query("SELECT * FROM entry")
    suspend fun allEntries(): List<EntryEntity>

    /** 본문을 읽지 않고 일기 ID만 읽는다. */
    @Query("SELECT id FROM entry")
    suspend fun entryIds(): List<String>

    @Query("SELECT * FROM media")
    suspend fun allMedia(): List<MediaEntity>

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    /** 받은 사진의 상태를 고친다. 같은 내용이어도 이름이 다르면 다른 파일이다. */
    @Query("UPDATE media SET status = :status WHERE sha256 = :sha256 AND file_name = :fileName AND status = 'pending'")
    suspend fun settlePending(sha256: String, fileName: String, status: String)

    /** 기기에 아직 없는 사진 파일. 여러 일기가 같이 써도 파일마다 한 줄이다. 목록 파일에도 이 값을 적는다. */
    @Query(
        "SELECT sha256, file_name AS fileName, MAX(byte_size) AS byteSize FROM media " +
            "WHERE status = 'pending' AND sha256 IS NOT NULL GROUP BY sha256, file_name"
    )
    suspend fun pendingFiles(): List<LocalPhotoRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBase(rows: List<SyncBaseEntity>)

    @Query("SELECT * FROM sync_base")
    suspend fun bases(): List<SyncBaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(marks: List<MergeMarkEntity>)

    @Query("SELECT * FROM merge_mark")
    fun marks(): Flow<List<MergeMarkEntity>>

    @Query("SELECT pair_entry_id FROM merge_mark WHERE entry_id = :entryId LIMIT 1")
    fun markedPair(entryId: String): Flow<String?>

    @Query("SELECT entry_id FROM merge_mark")
    suspend fun markedIds(): List<String>

    /**
     * 일기가 바뀌었는지 보는 값.
     * 가장 늦은 시각만 보면 시계가 앞선 기기의 일기가 하나 섞인 뒤로는 어떤 일기를 고쳐도 값이 그대로라 합계까지 본다.
     */
    @Query("SELECT COUNT(*) || ':' || COALESCE(MAX(updated_at), 0) || ':' || COALESCE(SUM(updated_at), 0) FROM entry")
    suspend fun entriesFingerprint(): String

    @Query("SELECT COUNT(*) FROM entry")
    suspend fun entryRows(): Int

    @Query("SELECT COUNT(*) FROM media")
    suspend fun mediaRows(): Int

    @Query("DELETE FROM media")
    suspend fun clearMedia()

    @Query("DELETE FROM entry")
    suspend fun clearEntries()

    @Query("DELETE FROM sync_base")
    suspend fun clearBases()

    @Query("DELETE FROM merge_mark")
    suspend fun clearMarks()

    @Query("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL")
    fun visibleEntries(): Flow<Int>

    @Query("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL")
    suspend fun visibleEntryCount(): Int

    /** 바꾸기 뒤에 쓰거나 고친 일기 수. 되돌리기 안내에 쓴다. */
    @Query("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL AND updated_at > :after")
    suspend fun visibleEntriesUpdatedAfter(after: Long): Int

    /** 본문을 읽지 않고 일기 수와 본문 글자 수를 잰다. 묶음 파일 크기를 어림할 때 쓴다. */
    @Query(
        "SELECT COUNT(CASE WHEN deleted_at IS NULL THEN 1 END) AS visibleEntries, " +
            "COALESCE(SUM(LENGTH(body_html)), 0) AS bodyCharacters FROM entry"
    )
    suspend fun entrySizes(): EntrySizes

    @Query("SELECT COUNT(*) FROM media WHERE entry_id IN (SELECT id FROM entry WHERE deleted_at IS NULL) AND status = 'normal'")
    fun visiblePhotos(): Flow<Int>

    @Query("DELETE FROM merge_mark WHERE entry_id = :entryId")
    suspend fun clearMark(entryId: String)
}

/** 기기에 파일이 있는 사진. 같은 해시는 한 번만 올린다. */
data class LocalPhotoRow(val sha256: String, val fileName: String?, val byteSize: Long?)

/** 일기 표의 크기. [bodyCharacters]는 휴지통의 일기까지 더한다. */
data class EntrySizes(val visibleEntries: Int, val bodyCharacters: Long)
