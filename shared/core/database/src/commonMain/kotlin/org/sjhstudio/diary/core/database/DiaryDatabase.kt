package org.sjhstudio.diary.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

/**
 * 일기 DB `diary.db`(research R4)
 * 버전 2에서 백업 표 셋 추가
 * 버전 3에서 임시 저장본에 복원 표시 열 추가
 * 시험 기기에 깔린 DB가 일기를 잃지 않고 넘어오도록 자동 이전을 둠
 * 표를 바꾸면 반드시 버전을 올림
 * 올리지 않으면 앱이 켜지다 죽음
 */
@Database(
    entities = [
        EntryEntity::class,
        MediaEntity::class,
        DraftEntity::class,
        DraftMediaEntity::class,
        BackupMediaEntity::class,
        SyncBaseEntity::class,
        MergeMarkEntity::class
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true
)
@ConstructedBy(DiaryDatabaseConstructor::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    abstract fun mediaDao(): MediaDao

    abstract fun draftDao(): DraftDao

    abstract fun backupDao(): BackupDao
}

// Room 컴파일러가 플랫폼마다 actual을 만듦
@Suppress("KotlinNoActualForExpect")
expect object DiaryDatabaseConstructor : RoomDatabaseConstructor<DiaryDatabase> {
    override fun initialize(): DiaryDatabase
}
