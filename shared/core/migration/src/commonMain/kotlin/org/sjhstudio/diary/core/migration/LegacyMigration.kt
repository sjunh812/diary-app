package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 앱 틀(관문, 설정)이 보는 기록 이전.
 * Android는 [MigrationRunner], iOS는 옮길 것이 없어 [NoLegacyMigration]이다.
 */
interface LegacyMigration {
    val progress: StateFlow<MigrationProgress>

    fun needsMigration(): Boolean

    fun state(): MigrationState

    fun report(): MigrationReport?

    suspend fun run(): MigrationResult

    /**
     * 일부만 옮겨진 기기에서 앱이 업데이트된 뒤 한 번 못 읽은 일기를 다시 읽는다.
     * 더한 일기 수, 때가 아니면 null(FR-011).
     */
    suspend fun retryPartial(db: DiaryDatabase): Int?

    /**
     * 실패 화면의 새 일기부터 쓰기.
     * 앱을 쓰게 하고 예전 일기는 [retryDeferred]로 나중에 옮긴다.
     */
    fun defer() = Unit

    /**
     * 미뤄 둔 이전을 다시 한다.
     * 지금 일기 DB를 버리지 않고 예전 일기를 더한다.
     * 실패해도 앱은 그대로 쓴다.
     * [onlyAfterUpdate]면 앱이 업데이트된 뒤 처음 한 번만 한다.
     */
    suspend fun retryDeferred(db: DiaryDatabase, onlyAfterUpdate: Boolean = false): MigrationResult = MigrationResult.NoLegacy

    /**
     * 찾을 수 없는 사진을 하루 한 번 원래 자리에서 다시 찾는다.
     * 찾은 장 수, 때가 아니면 null(FR-011).
     */
    suspend fun recheckMissingPhotos(db: DiaryDatabase): Int?

    /** 문의 메일에 넣는 단계·종류·개수 구간. */
    suspend fun supportSummary(): SupportSummary

    /**
     * 하루가 지난 고쳐 받기 문의 묶음을 지운다(FR-010a).
     * 지웠으면 true.
     */
    suspend fun cleanUpSupportFiles(): Boolean = false
}

/** 옛 앱이 없는 플랫폼(iOS)과 새 설치. */
object NoLegacyMigration : LegacyMigration {
    override val progress: StateFlow<MigrationProgress> = MutableStateFlow(MigrationProgress(MigrationStep.NOT_STARTED))

    override fun needsMigration(): Boolean = false

    override fun state(): MigrationState = MigrationState()

    override fun report(): MigrationReport? = null

    override suspend fun run(): MigrationResult = MigrationResult.NoLegacy

    override suspend fun retryPartial(db: DiaryDatabase): Int? = null

    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = null

    override suspend fun supportSummary(): SupportSummary = SupportSummary(null, 0, "0", "0", "0")
}
