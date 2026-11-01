package org.sjhstudio.diary.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 플랫폼마다 DB 파일 자리를 받아 [DiaryDatabase]를 연다. */
expect class DatabaseFactory {
    fun create(): DiaryDatabase
}

/**
 * Android와 iOS가 같은 SQLite(FTS5, `VACUUM INTO` 포함)를 쓰도록 번들 드라이버로 연다.
 * 쿼리는 IO 디스패처에서 돈다.
 * 시험도 이 설정을 그대로 쓴다.
 */
internal fun RoomDatabase.Builder<DiaryDatabase>.configured(): RoomDatabase.Builder<DiaryDatabase> = setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
