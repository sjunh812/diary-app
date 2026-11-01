package org.sjhstudio.diary.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * `media` 표. 열 이름, 타입, 널은 data-model.md `media` 표 그대로다.
 * 일기를 영구 삭제하면 사진 줄도 함께 지운다. 파일은 사진 정리(스펙 002)가 따로 지운다.
 */
@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entry_id", "sort_order"], unique = true, name = "index_media_entry_order"),
        Index(value = ["sha256"], name = "index_media_sha256")
    ]
)
data class MediaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val status: String,
    @ColumnInfo(name = "file_name") val fileName: String?,
    val sha256: String?,
    val width: Int?,
    val height: Int?,
    @ColumnInfo(name = "byte_size") val byteSize: Long?,
    @ColumnInfo(name = "legacy_path") val legacyPath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
