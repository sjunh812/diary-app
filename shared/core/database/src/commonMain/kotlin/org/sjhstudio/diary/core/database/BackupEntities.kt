package org.sjhstudio.diary.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/**
 * 드라이브에 올라가 확인까지 끝난 사진
 * 드라이브가 원본이고 이 표는 거울
 * 스냅샷에서는 비움
 */
@Entity(tableName = "backup_media", primaryKeys = ["account_key", "sha256"])
data class BackupMediaEntity(
    @ColumnInfo(name = "account_key") val accountKey: String,
    val sha256: String,
    @ColumnInfo(name = "drive_file_id") val driveFileId: String,
    @ColumnInfo(name = "byte_size") val byteSize: Long,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long
)

/**
 * 마지막으로 백업이나 복원으로 맞춰진 때의 `updated_at`
 * 합치기에서 어느 쪽이 고쳐졌는지 구분
 * 스냅샷에서는 비움
 */
@Entity(
    tableName = "sync_base",
    foreignKeys = [
        ForeignKey(entity = EntryEntity::class, parentColumns = ["id"], childColumns = ["entry_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class SyncBaseEntity(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "base_updated_at") val baseUpdatedAt: Long
)

/**
 * 합치기 결과로 알려야 하는 일기("두 가지로 남겼어요")
 * 스냅샷에서는 비움
 */
@Entity(
    tableName = "merge_mark",
    foreignKeys = [
        ForeignKey(entity = EntryEntity::class, parentColumns = ["id"], childColumns = ["entry_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MergeMarkEntity(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: String,
    val kind: String,
    @ColumnInfo(name = "pair_entry_id") val pairEntryId: String?,
    val source: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
