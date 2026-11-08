package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.MigrationResult
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.ui.launchSafely

/** 미뤄 둔 이전의 다시 시도 상태. */
enum class MigrationRetry { Idle, Running, Failed }

/**
 * 기록 이전 결과(M5) 화면 상태.
 * 결과 파일이 없으면 [report]가 null이다.
 * [deferred]면 예전 일기를 아직 옮기지 못한 기기라 다시 시도를 보인다.
 */
data class MigrationReportUiState(
    val report: MigrationReport? = null,
    val migratedOn: LocalDate? = null,
    val deferred: Boolean = false,
    val retry: MigrationRetry = MigrationRetry.Idle
)

/**
 * 설정 > 기록 이전 결과(FR-010, FR-011).
 * 이전 직후 결과 화면과 같은 숫자를 보인다.
 * 일부만 옮겨진 기기는 앱이 업데이트된 뒤 못 읽은 일기를 한 번 더 읽어서 다시 시도 버튼이 없다.
 * 새 일기부터 쓰기로 넘어간 기기만 여기서 다시 시도한다.
 */
class MigrationReportViewModel(
    private val migration: LegacyMigration,
    private val timeZone: TimeZone,
    /**
     * 결과 파일을 읽는 곳.
     * 시험에서는 시험 디스패처로 바꾼다.
     */
    private val background: CoroutineDispatcher = Dispatchers.Default,
    /** 미뤄 둔 이전을 다시 한다(`MigrationFollowUp.retryDeferredNow`). */
    private val retryDeferred: suspend () -> MigrationResult = { MigrationResult.NoLegacy }
) : ViewModel() {
    private val _state = MutableStateFlow(MigrationReportUiState())
    val state: StateFlow<MigrationReportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launchSafely { load() }
    }

    fun retry() {
        if (_state.value.retry == MigrationRetry.Running) return
        _state.value = _state.value.copy(retry = MigrationRetry.Running)
        viewModelScope.launchSafely {
            // 화면을 떠나 취소된 것은 실패가 아니다.
            val result =
                runCatching { withContext(background) { retryDeferred() } }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
            if (result is MigrationResult.Finished) {
                load()
            } else {
                _state.value = _state.value.copy(retry = MigrationRetry.Failed)
            }
        }
    }

    private suspend fun load() {
        val (report, deferred) = withContext(background) { migration.report() to (migration.state().step == MigrationStep.DEFERRED) }
        _state.value = MigrationReportUiState(report, report?.finishedAt?.toLocalDateTime(timeZone)?.date, deferred)
    }
}

/**
 * 설정에 "기록 이전 결과" 줄을 둘지.
 * 일부만 옮겨졌거나 옮기기를 미룬 기기에서만 둔다.
 * 다 옮긴 기기에는 줄이 없다.
 */
fun showsMigrationReport(migration: LegacyMigration): Boolean =
    migration.state().step.let { it == MigrationStep.PARTIAL || it == MigrationStep.DEFERRED }
