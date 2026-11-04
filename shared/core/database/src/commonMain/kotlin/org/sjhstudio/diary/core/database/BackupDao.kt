package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
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

    @Query("SELECT DISTINCT sha256, file_name AS fileName, byte_size AS byteSize FROM media WHERE status = 'normal' AND sha256 IS NOT NULL")
    suspend fun localPhotos(): List<LocalPhotoRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBase(rows: List<SyncBaseEntity>)

    @Query("SELECT * FROM sync_base")
    suspend fun bases(): List<SyncBaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(marks: List<MergeMarkEntity>)

    @Query("SELECT * FROM merge_mark")
    fun marks(): Flow<List<MergeMarkEntity>>

    @Query("DELETE FROM merge_mark WHERE entry_id = :entryId")
    suspend fun clearMark(entryId: String)
}

/** 기기에 파일이 있는 사진. 같은 해시는 한 번만 올린다. */
data class LocalPhotoRow(val sha256: String, val fileName: String?, val byteSize: Long?)
