package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 앱 틀(관문, 설정)이 보는 기록 이전. Android는 [MigrationRunner], iOS는 옮길 것이 없어 [NoLegacyMigration]이다.
 */
interface LegacyMigration {
    val progress: StateFlow<MigrationProgress>

    fun needsMigration(): Boolean

    fun state(): MigrationState

    fun report(): MigrationReport?

    suspend fun run(): MigrationResult

    suspend fun retryPartial(db: DiaryDatabase, currentVersionCode: Int): Int?

    suspend fun recheckMissingPhotos(db: DiaryDatabase): Int?

    /** 문의 메일에 넣는 단계·종류·개수 구간. */
    suspend fun supportSummary(): SupportSummary
}

/** 옛 앱이 없는 플랫폼(iOS)과 새 설치. */
object NoLegacyMigration : LegacyMigration {
    override val progress: StateFlow<MigrationProgress> = MutableStateFlow(MigrationProgress(MigrationStep.NOT_STARTED))

    override fun needsMigration(): Boolean = false

    override fun state(): MigrationState = MigrationState()

    override fun report(): MigrationReport? = null

    override suspend fun run(): MigrationResult = MigrationResult.NoLegacy

    override suspend fun retryPartial(db: DiaryDatabase, currentVersionCode: Int): Int? = null

    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = null

    override suspend fun supportSummary(): SupportSummary = SupportSummary(null, 0, "0", "0", "0")
}
