package org.sjhstudio.diary.core.database

import androidx.room3.ConstructedBy
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

/**
 * 일기 DB `diary.db`(research R4).
 * R1 출시 때 버전 1이다.
 * 스펙 002(임시 저장본)와 003(백업 표)은 출시 전에 같은 버전 1에 표를 더하고 스키마 JSON만 다시 만든다.
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
    version = 1,
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

// Room 컴파일러가 플랫폼마다 actual을 만든다.
@Suppress("KotlinNoActualForExpect")
expect object DiaryDatabaseConstructor : RoomDatabaseConstructor<DiaryDatabase> {
    override fun initialize(): DiaryDatabase
}
