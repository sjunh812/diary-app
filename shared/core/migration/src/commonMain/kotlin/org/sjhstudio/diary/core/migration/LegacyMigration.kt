package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 앱 틀(관문, 설정)이 보는 기록 이전
 * Android는 [MigrationRunner], iOS는 옮길 것이 없어 [NoLegacyMigration]
 * 모두 파일을 읽어 suspend.
 * 부르는 쪽이 화면 스레드에서 막히지 않게 구현이 입출력 스레드로 옮김
 */
@Suppress("TooManyFunctions") // 앱 틀이 보는 이전의 입구 하나. 나누면 같은 상태 파일을 여러 곳이 읽음
interface LegacyMigration {
    val progress: StateFlow<MigrationProgress>

    suspend fun needsMigration(): Boolean

    suspend fun state(): MigrationState

    suspend fun report(): MigrationReport?

    /**
     * 예전 일기 보기가 읽을 사본이 있는가
     * 사본을 만들기 전에 실패했으면 없음
     */
    suspend fun hasLegacyCopy(): Boolean = false

    suspend fun run(): MigrationResult

    /**
     * 일부만 옮겨진 기기에서 앱 업데이트 뒤 한 번 못 읽은 일기를 다시 읽음
     * 더한 일기 수, 때가 아니면 null(FR-011)
     */
    suspend fun retryPartial(db: DiaryDatabase): Int?

    /**
     * 실패 화면의 새 일기부터 쓰기
     * 앱을 쓰게 하고 예전 일기는 [retryDeferred]로 나중에 옮김
     */
    suspend fun defer() = Unit

    /**
     * 미뤄 둔 이전을 다시 함
     * 지금 일기 DB를 버리지 않고 예전 일기를 더함
     * 실패해도 앱은 그대로 씀
     * [onlyAfterUpdate]면 앱 업데이트 뒤 처음 한 번만 함
     */
    suspend fun retryDeferred(db: DiaryDatabase, onlyAfterUpdate: Boolean = false): MigrationResult = MigrationResult.NoLegacy

    /**
     * 찾을 수 없는 사진을 하루 한 번 원래 자리에서 다시 찾음
     * 찾은 장 수, 때가 아니면 null(FR-011)
     */
    suspend fun recheckMissingPhotos(db: DiaryDatabase): Int?

    /** 문의 메일에 넣는 단계·종류·개수 구간 */
    suspend fun supportSummary(): SupportSummary

    /**
     * 하루 지난 복구 문의 묶음을 지움(FR-010a)
     * 지웠으면 true.
     */
    suspend fun cleanUpSupportFiles(): Boolean = false

    /**
     * 아직 옮기지 못한 일기가 있어 옮겨 둔 사진 사본을 다시 쓸 수 있는가
     * 사본은 일기 줄이 없어 쓰지 않는 사진처럼 보이고, 캐시의 원본은 그사이 OS가 지울 수 있음(헌법 I)
     */
    suspend fun keepsPhotoCopies(): Boolean = false
}

/** 옛 앱이 없는 플랫폼(iOS)과 새 설치 */
object NoLegacyMigration : LegacyMigration {
    override val progress: StateFlow<MigrationProgress> = MutableStateFlow(MigrationProgress(MigrationStep.NOT_STARTED))

    override suspend fun needsMigration(): Boolean = false

    override suspend fun state(): MigrationState = MigrationState()

    override suspend fun report(): MigrationReport? = null

    override suspend fun run(): MigrationResult = MigrationResult.NoLegacy

    override suspend fun retryPartial(db: DiaryDatabase): Int? = null

    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = null

    override suspend fun supportSummary(): SupportSummary = SupportSummary(null, 0, "0", "0", "0")
}
