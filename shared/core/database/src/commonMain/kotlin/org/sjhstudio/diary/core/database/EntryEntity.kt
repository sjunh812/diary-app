package org.sjhstudio.diary.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * `entry` 표
 * 열 이름, 타입, 널은 data-model.md `entry` 표 그대로
 * 날짜와 시각은 글자(`yyyy-MM-dd`, `HH:mm`), 시각은 epoch 밀리초로 둠
 * 모델과의 변환은 Converters.kt에 있음
 */
@Entity(
    tableName = "entry",
    indices = [
        // 목록·달력: 휴지통이 아닌 일기를 최근 날짜부터
        Index(
            value = ["deleted_at", "date", "time", "created_at"],
            orders = [Index.Order.ASC, Index.Order.DESC, Index.Order.DESC, Index.Order.DESC],
            name = "index_entry_list"
        ),
        Index(value = ["deleted_at", "is_favorite"], name = "index_entry_favorite"),
        // 레거시 `_id`는 한 번만 옮김
        // 새로 쓴 일기는 null이라 여럿이어도 됨
        Index(value = ["legacy_id"], unique = true, name = "index_entry_legacy_id")
    ]
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    @ColumnInfo(name = "body_html") val bodyHtml: String,
    @ColumnInfo(name = "plain_text") val plainText: String,
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "place_name") val placeName: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "legacy_id") val legacyId: Int?,
    @ColumnInfo(name = "legacy_create_date") val legacyCreateDate: String?,
    @ColumnInfo(name = "legacy_location") val legacyLocation: String?
)
