package org.sjhstudio.diary.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * 임시 저장본(스펙 002 data-model.md Draft). 새 일기는 키 `new`, 고치는 일기는 일기 ID를 키로 하나씩 둔다.
 * 백업에 싣지 않는다(`docs/backup.md`).
 */
@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey val key: String,
    val date: String,
    val time: String,
    @ColumnInfo(name = "body_html") val bodyHtml: String,
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "place_name") val placeName: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "weather_auto_filled") val weatherAutoFilled: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** 임시 저장본의 사진. 저장하면 `media`로 옮기고 지운다. 옮겨 온 일기처럼 같은 사진이 두 줄일 수 있어 순서를 키로 쓴다. */
@Entity(
    tableName = "draft_media",
    primaryKeys = ["draft_key", "sort_order"],
    foreignKeys = [
        ForeignKey(
            entity = DraftEntity::class,
            parentColumns = ["key"],
            childColumns = ["draft_key"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["file_name"], name = "index_draft_media_file_name")]
)
data class DraftMediaEntity(
    @ColumnInfo(name = "draft_key") val draftKey: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    val sha256: String,
    val width: Int?,
    val height: Int?,
    @ColumnInfo(name = "byte_size") val byteSize: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)
