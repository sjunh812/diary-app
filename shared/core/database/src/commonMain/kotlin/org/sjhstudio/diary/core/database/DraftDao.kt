package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft WHERE `key` = :key")
    fun observe(key: String): Flow<DraftEntity?>

    @Query("SELECT * FROM draft_media WHERE draft_key = :key ORDER BY sort_order")
    fun observeMedia(key: String): Flow<List<DraftMediaEntity>>

    @Query("SELECT * FROM draft WHERE `key` = :key")
    suspend fun get(key: String): DraftEntity?

    @Query("SELECT * FROM draft_media WHERE draft_key = :key ORDER BY sort_order")
    suspend fun media(key: String): List<DraftMediaEntity>

    /** 가장 최근의 새 일기 임시 저장본. 일기 ID는 UUID라 `new`로 시작하지 않는다. */
    @Query("SELECT * FROM draft WHERE `key` LIKE 'new%' ORDER BY updated_at DESC LIMIT 1")
    suspend fun latestNew(): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(draft: DraftEntity): Long

    @Update
    suspend fun update(draft: DraftEntity)

    /** 덮어쓰기(REPLACE)는 줄을 지워 사진 줄까지 함께 지워지므로 넣어 보고 있으면 고친다. */
    @Transaction
    suspend fun upsert(draft: DraftEntity) {
        if (insertIfAbsent(draft) == -1L) update(draft)
    }

    @Insert
    suspend fun insertMedia(media: List<DraftMediaEntity>)

    @Query("DELETE FROM draft_media WHERE draft_key = :key")
    suspend fun clearMedia(key: String)

    @Query("DELETE FROM draft WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT DISTINCT file_name FROM draft_media")
    suspend fun fileNames(): List<String>

    /** 사진을 빼거나 순서를 바꾼다. 순서 번호를 다시 매기므로 통째로 바꾼다. */
    @Transaction
    suspend fun replaceMedia(key: String, media: List<DraftMediaEntity>) {
        clearMedia(key)
        insertMedia(media)
    }

    /**
     * 저장: 일기와 사진을 쓰고 임시 저장본을 지운다(FR-002, R6).
     * 한 트랜잭션이라 실패하면 임시 저장본이 그대로 남는다.
     */
    @Transaction
    suspend fun commit(key: String, entry: EntryEntity, media: List<MediaEntity>, entries: EntryDao, mediaDao: MediaDao) {
        if (entries.update(entry) == 0) entries.insertAll(listOf(entry))
        mediaDao.deleteForEntry(entry.id)
        if (media.isNotEmpty()) mediaDao.insertAll(media)
        delete(key)
    }
}
