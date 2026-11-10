package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjhstudio.diary.core.database.DiaryDatabase

/**
 * 이전과 다시 옮기기를 한 번에 하나만 돌림.
 * 다시 시도를 연달아 누르거나 설정의 다시 옮기기와 겹치면 같은 DB와 상태 파일을 함께 써서 깨짐.
 * 이전 단계는 [inner]가 그대로 맡고, 여기서는 순서만 지킴.
 */
class OneAtATimeMigration(private val inner: LegacyMigration) : LegacyMigration by inner {
    private val lock = Mutex()

    override suspend fun run(): MigrationResult = lock.withLock { inner.run() }

    override suspend fun retryPartial(db: DiaryDatabase): Int? = lock.withLock { inner.retryPartial(db) }

    override suspend fun retryDeferred(db: DiaryDatabase, onlyAfterUpdate: Boolean): MigrationResult =
        lock.withLock { inner.retryDeferred(db, onlyAfterUpdate) }

    /** 실패 화면의 새 일기부터 쓰기와 다시 시도를 겹쳐 눌러도 상태 파일을 함께 쓰지 않음 */
    override suspend fun defer() = lock.withLock { inner.defer() }

    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = lock.withLock { inner.recheckMissingPhotos(db) }
}
