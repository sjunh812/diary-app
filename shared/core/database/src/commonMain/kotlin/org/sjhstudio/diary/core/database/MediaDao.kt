package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** 상태별 사진 수 */
data class MediaStatusCount(val status: String, val count: Int)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE entry_id = :entryId ORDER BY sort_order")
    suspend fun forEntry(entryId: String): List<MediaEntity>

    /**
     * 상세가 봄
     * 복원이 사진을 받으면 일기 표는 그대로라 사진 줄을 따로 살핌
     */
    @Query("SELECT * FROM media WHERE entry_id = :entryId ORDER BY sort_order")
    fun observeForEntry(entryId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE entry_id IN (:entryIds) ORDER BY entry_id, sort_order")
    suspend fun forEntries(entryIds: List<String>): List<MediaEntity>

    /**
     * 한 번에 넣음
     * 한 일기 안에서 같은 순서가 있으면 실패
     */
    @Insert
    suspend fun insertAll(media: List<MediaEntity>)

    @Query("DELETE FROM media WHERE entry_id = :entryId")
    suspend fun deleteForEntry(entryId: String)

    /** 사진 정리가 남길 파일 이름(휴지통 일기 포함) */
    @Query("SELECT DISTINCT file_name FROM media WHERE file_name IS NOT NULL")
    suspend fun fileNames(): List<String>

    @Query("SELECT COUNT(*) FROM media")
    suspend fun count(): Int

    /**
     * 기다리거나 찾지 못한 사진 수
     * 이 값이 바뀌면 보이는 사진이 달라짐
     * 새 일기에 넣은 사진은 일기 표가 알리니 세지 않음
     */
    @Query("SELECT status, COUNT(*) AS count FROM media WHERE status != 'normal' GROUP BY status ORDER BY status")
    fun observeUnsettled(): Flow<List<MediaStatusCount>>

    /** 찾을 수 없는 사진 가운데 원래 경로를 아는 줄(하루 한 번 다시 찾기) */
    @Query("SELECT * FROM media WHERE status = 'missing' AND legacy_path IS NOT NULL ORDER BY entry_id, sort_order")
    suspend fun missingWithLegacyPath(): List<MediaEntity>

    @Update
    suspend fun update(media: MediaEntity)
}
