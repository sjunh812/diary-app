package org.sjhstudio.diary.core.database

import androidx.paging.PagingSource
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 달력과 기분 흐름이 읽는 한 줄.
 * 시간순이라 그날 마지막 줄이 그날의 기분이다.
 */
data class DayMoodRow(val date: String, val mood: String?)

/**
 * 목록 한 줄에 쓰는 값.
 * 쪽마다 긴 본문을 메모리에 올리지 않게 `body_html`은 읽지 않는다.
 * [plainText]는 미리보기 재료다.
 * 찾기가 아니면 앞 [EntryDao.LIST_TEXT_CHARS]자까지만 들어 있다.
 */
data class EntryListRow(
    val id: String,
    val date: String,
    val time: String,
    @ColumnInfo(name = "plain_text") val plainText: String,
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "place_name") val placeName: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean
)

/**
 * 고급 통계에 쓰는 값.
 * 본문은 읽지 않는다.
 */
data class StatsRow(
    val date: String,
    val time: String,
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "place_name") val placeName: String?,
    /** 글자 수만 센다. */
    val length: Int,
    val photos: Int
)

/**
 * 기분별 개수.
 * 기분 없는 일기는 [mood]가 null이다.
 */
data class MoodCountRow(val mood: String?, val count: Int)

@Dao
interface EntryDao {
    /**
     * 목록: 휴지통이 아닌 일기를 최근 날짜부터.
     * `index_entry_list`를 탄다.
     */
    @Query(VISIBLE_ENTRIES)
    fun visibleEntries(): PagingSource<Int, EntryListRow>

    /** 즐겨찾기만 보기. */
    @Query(FAVORITE_ENTRIES)
    fun favoriteEntries(): PagingSource<Int, EntryListRow>

    /**
     * 최근에 쓴 장소.
     * 같은 이름은 한 번만, 나중에 쓴 곳부터.
     */
    @Query(
        "SELECT place_name FROM entry WHERE deleted_at IS NULL AND place_name IS NOT NULL AND place_name != '' " +
            "GROUP BY place_name ORDER BY MAX(date || ' ' || time) DESC LIMIT :limit"
    )
    suspend fun recentPlaces(limit: Int): List<String>

    /** 일기가 있는 달(`yyyy-MM`). */
    @Query("SELECT DISTINCT substr(date, 1, 7) FROM entry WHERE deleted_at IS NULL")
    fun months(): Flow<List<String>>

    /** 즐겨찾기가 있는 달. 즐겨찾기만 볼 때 고를 수 있는 달이다. */
    @Query("SELECT DISTINCT substr(date, 1, 7) FROM entry WHERE deleted_at IS NULL AND is_favorite = 1")
    fun favoriteMonths(): Flow<List<String>>

    /**
     * [date]보다 나중 날짜의 일기 수.
     * 그 달 첫 줄의 목록 위치가 된다.
     */
    @Query(
        "SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL AND date > :date " +
            "AND (:favoritesOnly = 0 OR is_favorite = 1)"
    )
    suspend fun countAfter(date: String, favoritesOnly: Boolean): Int

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
        "SELECT date, time, mood, weather, place_name, LENGTH(plain_text) AS length, " +
            "(SELECT COUNT(*) FROM media WHERE media.entry_id = entry.id) AS photos FROM entry WHERE deleted_at IS NULL " +
            "AND (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to)"
    )
    fun statsRows(from: String?, to: String?): Flow<List<StatsRow>>

    @Query(
        "SELECT mood, COUNT(*) AS count FROM entry WHERE deleted_at IS NULL " +
            "AND (:from IS NULL OR date >= :from) AND (:to IS NULL OR date <= :to) GROUP BY mood"
    )
    fun moodCounts(from: String?, to: String?): Flow<List<MoodCountRow>>

    /**
     * 찾기(FR-025, FR-026).
     * [like]는 이스케이프한 `%글자%`이고 빈 글자면 조건에서 빠진다.
     * 목록 조건은 빈 목록이 `IN ()`이 되지 않게 [anyMood], [anyWeather]로 끈다.
     * 찾은 글자가 뒤에 있어도 미리보기에 보여야 해서 `plain_text`는 다 읽는다.
     */
    @Query(
        "SELECT id, date, time, plain_text, mood, weather, place_name, is_favorite FROM entry WHERE deleted_at IS NULL " +
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
    ): PagingSource<Int, EntryListRow>

    @Query("UPDATE entry SET is_favorite = :favorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    /**
     * 휴지통 넣기와 꺼내기.
     * 합치기 복원이 고친 쪽을 가르도록 `updated_at`도 바꾼다.
     */
    @Query("UPDATE entry SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?, now: Long)

    /** 휴지통: 지운 날짜순. */
    @Query("SELECT * FROM entry WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun trashed(): Flow<List<EntryEntity>>

    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL AND id IN (:ids)")
    suspend fun deleteTrashed(ids: List<String>)

    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL")
    suspend fun deleteAllTrashed()

    /**
     * 30일 지난 휴지통 일기.
     * 몇 편을 지웠는지 돌려준다.
     */
    @Query("DELETE FROM entry WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeBefore(before: Long): Int

    /** 기록 이전 검증과 다시 읽기에 쓴다. */
    @Query("SELECT * FROM entry WHERE legacy_id = :legacyId")
    suspend fun getByLegacyId(legacyId: Int): EntryEntity?

    /**
     * 옛 기록에서 온 일기를 옛 일기와 견주는 데 쓰는 값.
     * 지운 일기까지 읽어 휴지통으로 보낸 일기가 다시 들어오지 않게 한다.
     */
    @Query(
        "SELECT legacy_id AS legacyId, legacy_create_date AS legacyCreateDate, plain_text AS plainText FROM entry " +
            "WHERE legacy_id IS NOT NULL OR legacy_create_date IS NOT NULL"
    )
    suspend fun legacyRows(): List<LegacyEntryRow>

    /**
     * 옛 번호만 비운다.
     * 일기 내용과 수정 시각은 그대로다.
     */
    @Query("UPDATE entry SET legacy_id = NULL WHERE legacy_id IN (:legacyIds)")
    suspend fun clearLegacyIds(legacyIds: List<Int>)

    /**
     * 휴지통까지 센다.
     * 개발 빌드의 목록 견본과 업그레이드 시험이 쓴다.
     */
    @Query("SELECT COUNT(*) FROM entry")
    suspend fun countAll(): Int

    /**
     * 한 번에 넣는다.
     * 같은 ID나 같은 레거시 ID가 있으면 실패한다.
     */
    @Insert
    suspend fun insertAll(entries: List<EntryEntity>)

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    /**
     * 고친 줄 수를 돌려준다.
     * 없는 일기면 0이다.
     */
    @Update
    suspend fun update(entry: EntryEntity): Int

    /**
     * 일기를 읽어 고쳐 쓴다.
     * [change]가 null을 주면 쓰지 않는다.
     * 한 트랜잭션이라 사이에 다른 쓰기가 끼어들어 앞의 고침을 덮지 않는다.
     */
    @Transaction
    suspend fun edit(id: String, change: (EntryEntity) -> EntryEntity?) {
        val entry = get(id) ?: return
        change(entry)?.let { update(it) }
    }

    /**
     * 휴지통에서 영구 삭제.
     * 사진 줄도 함께 지워진다.
     */
    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deletePermanently(id: String)

    companion object {
        /**
         * 목록 미리보기가 다듬기 전에 쓰는 앞부분.
         * 미리보기 240자를 채우고 남는 양이다.
         */
        const val LIST_TEXT_CHARS = 520

        private const val LIST_COLUMNS =
            "id, date, time, substr(plain_text, 1, $LIST_TEXT_CHARS) AS plain_text, mood, weather, place_name, is_favorite"

        const val VISIBLE_ENTRIES =
            "SELECT $LIST_COLUMNS FROM entry WHERE deleted_at IS NULL ORDER BY date DESC, time DESC, created_at DESC"
        const val FAVORITE_ENTRIES =
            "SELECT $LIST_COLUMNS FROM entry WHERE deleted_at IS NULL AND is_favorite = 1 " +
                "ORDER BY date DESC, time DESC, created_at DESC"
    }
}

/** 옛 일기를 옮겼는지 보는 데 쓰는 값. */
data class LegacyEntryRow(val legacyId: Int?, val legacyCreateDate: String?, val plainText: String)
