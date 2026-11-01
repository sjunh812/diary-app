package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE entry_id = :entryId ORDER BY sort_order")
    suspend fun forEntry(entryId: String): List<MediaEntity>

    @Query("SELECT * FROM media WHERE entry_id IN (:entryIds) ORDER BY entry_id, sort_order")
    suspend fun forEntries(entryIds: List<String>): List<MediaEntity>

    /** 한 번에 넣는다. 한 일기 안에서 같은 순서가 있으면 실패한다. */
    @Insert
    suspend fun insertAll(media: List<MediaEntity>)

    @Query("SELECT COUNT(*) FROM media")
    suspend fun count(): Int

    /** 찾을 수 없는 사진 가운데 원래 경로를 아는 줄(하루 한 번 다시 찾기). */
    @Query("SELECT * FROM media WHERE status = 'missing' AND legacy_path IS NOT NULL ORDER BY entry_id, sort_order")
    suspend fun missingWithLegacyPath(): List<MediaEntity>

    @Update
    suspend fun update(media: MediaEntity)
}
