package org.sjhstudio.diary.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.model.MediaStatus

/**
 * 고칠 수 있는 사진 줄.
 * 찾을 수 없는 사진은 고칠 수 없으니 임시 저장본에 싣지 않는다.
 */
val MediaEntity.isEditable: Boolean get() = status == MediaStatus.NORMAL.id && fileName != null && sha256 != null

/**
 * 수정 화면을 열어 읽은 줄.
 * [resumed]면 전에 남겨 둔 임시 저장본을 이어 연 것이다.
 */
data class EditDraftRows(val draft: DraftEntity, val media: List<DraftMediaEntity>, val resumed: Boolean) {
    override fun toString(): String = "EditDraftRows"
}

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

    /**
     * 가장 최근의 새 일기 임시 저장본.
     * 일기 ID는 UUID라 `new`로 시작하지 않는다.
     */
    @Query("SELECT * FROM draft WHERE `key` LIKE 'new%' ORDER BY updated_at DESC LIMIT 1")
    suspend fun latestNew(): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(draft: DraftEntity): Long

    @Update
    suspend fun update(draft: DraftEntity)

    /**
     * 덮어쓰기(REPLACE)는 줄을 지워 사진 줄까지 함께 지워지므로 넣어 보고 있으면 고친다.
     * 복원이 남긴 표시는 화면이 모르는 값이라 자동 저장이 지우지 않게 잇는다.
     */
    @Transaction
    suspend fun upsert(draft: DraftEntity) {
        if (insertIfAbsent(draft) == -1L) update(draft.copy(entryChanged = get(draft.key)?.entryChanged ?: draft.entryChanged))
    }

    /** 복원이 이 일기를 바꿨다고 적는다. */
    @Query("UPDATE draft SET entry_changed = 1 WHERE `key` = :entryId")
    suspend fun markEntryChanged(entryId: String)

    /** 복원이 일기를 통째로 바꾸면 고치던 저장본은 모두 옛 일기를 보고 있다. */
    @Query("UPDATE draft SET entry_changed = 1 WHERE `key` NOT LIKE 'new%'")
    suspend fun markAllEntriesChanged()

    /**
     * 수정 화면을 연다.
     * 임시 저장본이 있으면 그대로 돌려주고, 없으면 일기에서 만든다.
     * 일기가 없으면 null이다.
     * 한 트랜잭션이라 사진 줄이 빠진 임시 저장본이 남지 않는다.
     * 그런 저장본을 저장하면 사진이 사라진다(헌법 I).
     */
    @Transaction
    suspend fun openEdit(entryId: String, now: Long, entries: EntryDao, mediaDao: MediaDao): EditDraftRows? {
        get(entryId)?.let { return EditDraftRows(it, media(entryId), resumed = true) }
        val entry = entries.get(entryId)?.takeIf { it.deletedAt == null } ?: return null
        val draft =
            DraftEntity(
                key = entryId,
                date = entry.date,
                time = entry.time,
                bodyHtml = entry.bodyHtml,
                mood = entry.mood,
                weather = entry.weather,
                placeName = entry.placeName,
                isFavorite = entry.isFavorite,
                weatherAutoFilled = false,
                updatedAt = now
            )
        upsert(draft)
        // 찾을 수 없는 사진은 저장할 때 그대로 둔다.
        val rows =
            mediaDao.forEntry(entryId).filter { it.isEditable }.mapIndexed { index, row ->
                DraftMediaEntity(entryId, row.fileName!!, row.sha256!!, row.width, row.height, row.byteSize, index)
            }
        if (rows.isNotEmpty()) insertMedia(rows)
        return EditDraftRows(draft, rows, resumed = false)
    }

    @Insert
    suspend fun insertMedia(media: List<DraftMediaEntity>)

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM draft_media WHERE draft_key = :key")
    suspend fun nextMediaOrder(key: String): Int

    /**
     * 사진 한 장을 끝에 붙인다.
     * 순번은 읽은 값을 믿지 않고 여기서 다시 센다.
     */
    @Transaction
    suspend fun appendMedia(row: DraftMediaEntity) {
        insertMedia(listOf(row.copy(sortOrder = nextMediaOrder(row.draftKey))))
    }

    @Query("DELETE FROM draft_media WHERE draft_key = :key")
    suspend fun clearMedia(key: String)

    @Query("DELETE FROM draft WHERE `key` = :key")
    suspend fun delete(key: String)

    /**
     * 휴지통에서 완전히 지울 일기의 임시 저장본.
     * 사진 줄은 함께 지워지고, 파일은 사진 정리가 지운다.
     * 복원이 일기를 없앤 저장본은 새 일기로 살릴 글이라 여기서 지우지 않는다.
     */
    @Query("DELETE FROM draft WHERE `key` IN (SELECT id FROM entry WHERE deleted_at IS NOT NULL AND id IN (:ids))")
    suspend fun deleteTrashed(ids: List<String>)

    @Query("DELETE FROM draft WHERE `key` IN (SELECT id FROM entry WHERE deleted_at IS NOT NULL)")
    suspend fun deleteAllTrashed()

    @Query("DELETE FROM draft WHERE `key` IN (SELECT id FROM entry WHERE deleted_at IS NOT NULL AND deleted_at < :before)")
    suspend fun purgeBefore(before: Long)

    @Query("SELECT DISTINCT file_name FROM draft_media")
    suspend fun fileNames(): List<String>

    /**
     * 사진을 빼거나 순서를 바꾼다.
     * 순서 번호를 다시 매기므로 통째로 바꾼다.
     */
    @Transaction
    suspend fun replaceMedia(key: String, media: List<DraftMediaEntity>) {
        clearMedia(key)
        insertMedia(media)
    }

    /**
     * [index]번째 사진을 뺀다.
     * 그 사이 사진이 줄어 없는 자리면 그대로 둔다.
     */
    @Transaction
    suspend fun removeMediaAt(key: String, index: Int): DraftMediaEntity? {
        val rows = media(key)
        val removed = rows.getOrNull(index) ?: return null
        replaceMedia(key, rows.filterIndexed { i, _ -> i != index }.mapIndexed { i, row -> row.copy(sortOrder = i) })
        return removed
    }

    /**
     * 뺀 사진을 원래 자리에 다시 넣는다(되돌리기).
     * 그 사이 사진이 줄었으면 가능한 가장 가까운 자리에 둔다.
     */
    @Transaction
    suspend fun insertMediaAt(key: String, row: DraftMediaEntity, index: Int) {
        val rows = media(key).toMutableList()
        rows.add(index.coerceIn(0, rows.size), row.copy(draftKey = key))
        replaceMedia(key, rows.mapIndexed { i, media -> media.copy(sortOrder = i) })
    }

    /** [order]가 지금 사진을 빠짐없이 나열하지 않으면 그 사이 사진이 바뀐 것이라 그대로 둔다. */
    @Transaction
    suspend fun reorderMedia(key: String, order: List<Int>) {
        val rows = media(key)
        if (order.sorted() != rows.indices.toList()) return
        replaceMedia(key, order.mapIndexed { i, from -> rows[from].copy(sortOrder = i) })
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
