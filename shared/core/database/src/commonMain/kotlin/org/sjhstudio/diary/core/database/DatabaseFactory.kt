package org.sjhstudio.diary.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 플랫폼마다 DB 파일 자리를 받아 [DiaryDatabase]를 엶 */
expect class DatabaseFactory {
    fun create(): DiaryDatabase

    /**
     * 기록 이전이 쓰는 임시 DB(`migration/diary-new.db`)를 엶
     * 저널은 TRUNCATE라 닫은 뒤 파일 하나만 남아 이름 바꾸기 한 번으로 옮길 수 있음(research R7)
     */
    fun createAt(path: String): DiaryDatabase
}

/**
 * Android와 iOS가 같은 SQLite(FTS5, `VACUUM INTO` 포함)를 쓰도록 번들 드라이버로 엶
 * 쿼리는 IO 디스패처에서 실행
 * 시험도 이 설정을 그대로 씀
 */
fun RoomDatabase.Builder<DiaryDatabase>.configured(): RoomDatabase.Builder<DiaryDatabase> = setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
