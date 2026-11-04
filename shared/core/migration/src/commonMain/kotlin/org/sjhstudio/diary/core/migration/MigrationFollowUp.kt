package org.sjhstudio.diary.core.migration

import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 이전이 끝난 기기에서 앱을 열 때 하는 후속 작업(FR-011, contracts/migration-flow.md).
 * 일부만 옮겨진 기기는 앱이 업데이트된 뒤 처음 한 번 못 읽은 일기를 다시 읽고,
 * 찾을 수 없는 사진은 하루 한 번 원래 자리를 다시 본다.
 * 하루가 지난 고쳐 받기 문의 묶음도 이때 지운다(FR-010a).
 * 일기 DB는 이전이 끝난 뒤에만 연다.
 * 이전 전에 열면 빈 DB가 먼저 생겨 옮긴 DB와 부딪힌다.
 */
class MigrationFollowUp(private val migration: LegacyMigration, private val database: () -> DiaryDatabase) {
    suspend fun run(): FollowUpResult {
        migration.cleanUpSupportFiles()
        val step = migration.state().step
        if (step == MigrationStep.DEFERRED) {
            val result = migration.retryDeferred(database(), onlyAfterUpdate = true)
            return FollowUpResult(entriesRecovered = (result as? MigrationResult.Finished)?.report?.entriesMigrated ?: 0)
        }
        if (step != MigrationStep.DONE && step != MigrationStep.PARTIAL) return FollowUpResult()
        val db = database()
        return FollowUpResult(
            entriesRecovered = migration.retryPartial(db) ?: 0,
            photosFound = migration.recheckMissingPhotos(db) ?: 0
        )
    }

    /** 설정에서 미뤄 둔 이전을 다시 시도할 때. */
    suspend fun retryDeferredNow(): MigrationResult = migration.retryDeferred(database())
}

/** 다시 읽어 더한 일기 수와 다시 찾은 사진 수. 0이면 알리지 않는다. */
data class FollowUpResult(val entriesRecovered: Int = 0, val photosFound: Int = 0)
