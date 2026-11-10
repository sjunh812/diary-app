package org.sjhstudio.diary.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
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
    .setQueryCoroutineContext(RoomQueryContext)

/**
 * Room 안쪽 작업(변경 감지 등)의 예외로 앱이 꺼지지 않게 받음.
 * 쿼리 실패는 부른 쪽에도 그대로 가서 저장소가 결과로 돌려줌
 */
internal val RoomQueryContext: CoroutineContext =
    Dispatchers.IO + CoroutineExceptionHandler { _, error -> println("DB 작업 실패: ${error::class.simpleName}") }
