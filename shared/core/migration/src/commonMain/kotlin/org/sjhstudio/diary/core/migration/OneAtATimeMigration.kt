package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjhstudio.diary.core.database.DiaryDatabase

/**
 * 이전과 다시 옮기기를 한 번에 하나만 돌린다.
 * 다시 시도를 연달아 누르거나 설정의 다시 옮기기와 겹치면 같은 DB와 상태 파일을 함께 써서 깨진다.
 * 이전 단계는 [inner]가 그대로 맡고, 여기서는 순서만 지킨다.
 */
class OneAtATimeMigration(private val inner: LegacyMigration) : LegacyMigration by inner {
    private val lock = Mutex()

    override suspend fun run(): MigrationResult = lock.withLock { inner.run() }

    override suspend fun retryPartial(db: DiaryDatabase): Int? = lock.withLock { inner.retryPartial(db) }

    override suspend fun retryDeferred(db: DiaryDatabase, onlyAfterUpdate: Boolean): MigrationResult =
        lock.withLock { inner.retryDeferred(db, onlyAfterUpdate) }

    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = lock.withLock { inner.recheckMissingPhotos(db) }
}
