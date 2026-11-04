package org.sjhstudio.diary.core.database

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** 달력과 기분 흐름이 읽는 한 줄. 시간순이라 그날 마지막 줄이 그날의 기분이다. */
data class DayMoodRow(val date: String, val mood: String?)

/** 기분별 개수. 기분 없는 일기는 [mood]가 null이다. */
data class MoodCountRow(val mood: String?, val count: Int)

@Dao
interface EntryDao {
    /** 목록: 휴지통이 아닌 일기를 최근 날짜부터. `index_entry_list`를 탄다. */
    @Query(VISIBLE_ENTRIES)
    fun visibleEntries(): PagingSource<Int, EntryEntity>

    /** 즐겨찾기만 보기. */
    @Query(FAVORITE_ENTRIES)
    fun favoriteEntries(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun get(id: String): EntryEntity?

    /** 상세: 휴지통으로 옮긴 일기는 돌려주지 않는다. */
    @Query("SELECT * FROM entry WHERE id = :id AND deleted_at IS NULL")
    fun observeVisible(id: String): Flow<EntryEntity?>

    /** 그날 일기를 위에서부터 최근 시각 순으로. */
    @Query("SELECT * FROM entry WHERE deleted_at IS NULL AND date = :date ORDER BY time DESC, created_at DESC")
    fun onDay(date: String): Flow<List<EntryEntity>>

    /** 달력: 시간순으로 읽어 그날 마지막 일기를 뒤에 둔다(FR-022). */
    @Query(
        "SELECT date, mood FROM entry WHERE deleted_at IS NULL AND date BETWEEN :from AND :to " +
            "ORDER BY date, time, created_at"
    )
    fun dayMoods(from: String, to: String): Flow<List<DayMoodRow>>

    @Query(
        "SELECT mood, COUNT(*) AS count FROM entry WHERE deleted_at IS NULL " +
            "AND (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to) GROUP BY mood"
    )
    fun moodCounts(from: String?, to: String?): Flow<List<MoodCountRow>>

    /**
     * 찾기(FR-025, FR-026). [like]는 이스케이프한 `%글자%`이고 빈 글자면 조건에서 빠진다.
     * 목록 조건은 빈 목록이 `IN ()`이 되지 않게 [anyMood], [anyWeather]로 끈다.
     */
    @Query(
        "SELECT * FROM entry WHERE deleted_at IS NULL " +
            "AND (:like = '' OR plain_text LIKE :like ESCAPE '\\' OR place_name LIKE :like ESCAPE '\\') " +
            "AND (:anyMood OR mood IN (:moods)) " +
            "AND (:anyWeather OR weather IN (:weathers)) " +
            "AND (:favoritesOnly = 0 OR is_favorite = 1) " +
            "AND (:hasPhoto = 0 OR EXISTS (SELECT 1 FROM media WHERE media.entry_id = entry.id)) " +
            "AND (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to) " +
            "ORDER BY date DESC, time DESC, created_at DESC"
    )
    fun search(
        like: String,
        anyMood: Boolean,
        moods: List<String>,
        anyWeather: Boolean,
        weathers: List<String>,
        favoritesOnly: Boolean,
        hasPhoto: Boolean,
        from: String?,
        to: String?
    ): Flow<List<EntryEntity>>

    @Query("UPDATE entry SET is_favorite = :favorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    /** 휴지통 넣기와 꺼내기. 합치기 복원이 고친 쪽을 가르도록 `updated_at`도 바꾼다. */
    @Query("UPDATE entry SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?, now: Long)

    /** 휴지통: 지운 날짜순. */
    @Query("SELECT * FROM entry WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun trashed(): Flow<List<EntryEntity>>

    @Query("SELECT id FROM entry WHERE deleted_at IS NOT NULL")
    suspend fun trashedIds(): List<String>

    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL AND id IN (:ids)")
    suspend fun deleteTrashed(ids: List<String>)

    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL")
    suspend fun deleteAllTrashed()

    /** 30일 지난 휴지통 일기. 몇 편을 지웠는지 돌려준다. */
    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeBefore(before: Long): Int

    /** 기록 이전 검증과 다시 읽기에 쓴다. */
    @Query("SELECT * FROM entry WHERE legacy_id = :legacyId")
    suspend fun getByLegacyId(legacyId: Int): EntryEntity?

    @Query("SELECT legacy_id FROM entry WHERE legacy_id IS NOT NULL")
    suspend fun legacyIds(): List<Int>

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

    /** 고친 줄 수를 돌려준다. 없는 일기면 0이다. */
    @Update
    suspend fun update(entry: EntryEntity): Int

    /** 휴지통에서 영구 삭제. 사진 줄도 함께 지워진다. */
    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deletePermanently(id: String)

    companion object {
        const val VISIBLE_ENTRIES =
            "SELECT * FROM entry WHERE deleted_at IS NULL ORDER BY date DESC, time DESC, created_at DESC"
        const val FAVORITE_ENTRIES =
            "SELECT * FROM entry WHERE deleted_at IS NULL AND is_favorite = 1 ORDER BY date DESC, time DESC, created_at DESC"
    }
}
