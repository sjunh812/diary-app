package org.sjhstudio.diary.core.database

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface EntryDao {
    /** 목록: 휴지통이 아닌 일기를 최근 날짜부터. `index_entry_list`를 탄다. */
    @Query(VISIBLE_ENTRIES)
    fun visibleEntries(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun get(id: String): EntryEntity?

    @Query("SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL")
    suspend fun countVisible(): Int

    /** 휴지통까지 센다. 이전 검증(`entry` 수 + 못 읽은 줄 = 레거시 줄 수)에 쓴다. */
    @Query("SELECT COUNT(*) FROM entry")
    suspend fun countAll(): Int

    /** 한 번에 넣는다. 같은 ID나 같은 레거시 ID가 있으면 실패한다. */
    @Insert
    suspend fun insertAll(entries: List<EntryEntity>)

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    /** 휴지통에서 영구 삭제. 사진 줄도 함께 지워진다. */
    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deletePermanently(id: String)

    companion object {
        const val VISIBLE_ENTRIES =
            "SELECT * FROM entry WHERE deleted_at IS NULL ORDER BY date DESC, time DESC, created_at DESC"
    }
}
