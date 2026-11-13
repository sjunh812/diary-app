package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** [BackupDao.entriesFingerprint]의 SQL. 되돌릴 사본에도 같은 식을 씀 */
const val ENTRIES_FINGERPRINT = "SELECT COUNT(*) || ':' || COALESCE(MAX(updated_at), 0) || ':' || COALESCE(SUM(updated_at), 0) FROM entry"

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_media WHERE account_key = :accountKey")
    suspend fun confirmed(accountKey: String): List<BackupMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun confirm(media: List<BackupMediaEntity>)

    @Query("DELETE FROM backup_media WHERE account_key = :accountKey AND sha256 IN (:hashes)")
    suspend fun forget(accountKey: String, hashes: List<String>)

    /**
     * 아직 올리지 못한 사진 파일 수
     * 기기에 파일이 있는 것만 집계
     */
    @Query(
        "SELECT COUNT(DISTINCT m.sha256) FROM media m WHERE m.status = 'normal' AND m.sha256 IS NOT NULL " +
            "AND m.sha256 NOT IN (SELECT sha256 FROM backup_media WHERE account_key = :accountKey)"
    )
    fun notBackedUp(accountKey: String): Flow<Int>

    /**
     * 올릴 사진
     * 공간이 부족하면 앞에서부터 들어가는 만큼 올리도록 최근 일기의 사진이 먼저(FR-018)
     */
    @Query(
        "SELECT m.sha256 AS sha256, MIN(m.file_name) AS fileName, MAX(m.byte_size) AS byteSize FROM media m " +
            "JOIN entry e ON e.id = m.entry_id WHERE m.status = 'normal' AND m.sha256 IS NOT NULL " +
            "GROUP BY m.sha256 ORDER BY MAX(e.date || e.time) DESC"
    )
    suspend fun localPhotos(): List<LocalPhotoRow>

    @Query("SELECT * FROM entry")
    suspend fun allEntries(): List<EntryEntity>

    /** 본문을 읽지 않고 일기 ID만 읽음 */
    @Query("SELECT id FROM entry")
    suspend fun entryIds(): List<String>

    @Query("SELECT * FROM media")
    suspend fun allMedia(): List<MediaEntity>

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    /**
     * 받은 사진의 상태를 고침
     * 같은 내용이어도 이름이 다르면 다른 파일
     */
    @Query("UPDATE media SET status = :status WHERE sha256 = :sha256 AND file_name = :fileName AND status = 'pending'")
    suspend fun settlePending(sha256: String, fileName: String, status: String)

    /**
     * 기기에 아직 없는 사진 파일
     * 여러 일기가 같이 써도 파일마다 한 줄
     * 목록 파일에도 이 값을 적음
     */
    @Query(
        "SELECT sha256, file_name AS fileName, MAX(byte_size) AS byteSize FROM media " +
            "WHERE status = 'pending' AND sha256 IS NOT NULL GROUP BY sha256, file_name"
    )
    suspend fun pendingFiles(): List<LocalPhotoRow>

    /** [pendingFiles]의 수 */
    @Query(
        "SELECT COUNT(*) FROM (SELECT 1 FROM media WHERE status = 'pending' AND sha256 IS NOT NULL GROUP BY sha256, file_name)"
    )
    fun pendingFileCount(): Flow<Int>

    /** 내용 확인값이 남아 있어 다시 받을 수 있는 찾을 수 없는 사진 */
    @Query(
        "SELECT sha256, file_name AS fileName, MAX(byte_size) AS byteSize FROM media " +
            "WHERE status = 'missing' AND sha256 IS NOT NULL AND file_name IS NOT NULL GROUP BY sha256, file_name"
    )
    suspend fun missingFiles(): List<LocalPhotoRow>

    /** 찾을 수 없던 사진을 다시 쓸 수 있게 됐거나 다시 받을 때 상태를 고침 */
    @Query("UPDATE media SET status = :status WHERE sha256 = :sha256 AND file_name = :fileName AND status = 'missing'")
    suspend fun reviveMissing(sha256: String, fileName: String, status: String)

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
     * 일기가 바뀌었는지 보는 값
     * 가장 늦은 시각만 보면 시계가 앞선 기기의 일기가 하나 섞인 뒤로는 어떤 일기를 고쳐도 값이 그대로라 합계까지 봄
     */
    @Query(ENTRIES_FINGERPRINT)
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

    /**
     * 바꾸기 뒤에 쓰거나 고친 일기 수
     * 되돌리기 안내에 씀
     */
    @Query("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL AND updated_at > :after")
    suspend fun visibleEntriesUpdatedAfter(after: Long): Int

    /**
     * 본문을 읽지 않고 일기 수와 본문 글자 수 계산
     * 묶음 파일 크기를 어림할 때 씀
     */
    @Query(
        "SELECT COUNT(CASE WHEN deleted_at IS NULL THEN 1 END) AS visibleEntries, " +
            "COALESCE(SUM(LENGTH(body_html)), 0) AS bodyCharacters FROM entry"
    )
    suspend fun entrySizes(): EntrySizes

    /**
     * 보이는 일기의 사진 파일 수
     * 백업·복원 화면과 같은 기준이라 같은 사진이 여러 일기에 붙어도 한 장
     */
    @Query(
        "SELECT COUNT(DISTINCT sha256) FROM media WHERE entry_id IN (SELECT id FROM entry WHERE deleted_at IS NULL) " +
            "AND status = 'normal' AND sha256 IS NOT NULL"
    )
    fun visiblePhotos(): Flow<Int>

    @Query("DELETE FROM merge_mark WHERE entry_id = :entryId")
    suspend fun clearMark(entryId: String)
}

/**
 * 기기에 파일이 있는 사진
 * 같은 해시는 한 번만 올림
 */
data class LocalPhotoRow(val sha256: String, val fileName: String?, val byteSize: Long?)

/**
 * 일기 표의 크기
 * [bodyCharacters]는 휴지통의 일기까지 더함
 */
data class EntrySizes(val visibleEntries: Int, val bodyCharacters: Long)
