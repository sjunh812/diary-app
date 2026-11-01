package org.sjhstudio.diary.feature.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.MigrationResult
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep

/** 관문이 보는 이전 화면 상태(contracts/migration-flow.md "화면 상태"). */
sealed interface MigrationUiState {
    /** 원본 확인 중. 바탕색만 보인다(보통 1초 안). */
    data object Checking : MigrationUiState

    /** 사본을 만드는 중(M0). 끝을 모르는 진행 표시. */
    data object Preparing : MigrationUiState

    /** 옮기고 검증하는 중(M1). 일기 내용과 사진은 없다. */
    data class Copying(val entriesDone: Int, val entriesTotal: Int, val photosDone: Int, val photosTotal: Int) : MigrationUiState

    /** 끝난 직후 한 번(M2, M2-b). */
    data class Result(val report: MigrationReport) : MigrationUiState

    /** 실패(M3). 앱 틀로 가지 않는다. */
    data class Failed(val kind: MigrationFailureKind, val shortfallBytes: Long?) : MigrationUiState

    /** 옮길 것이 없거나 끝났다. 앱 틀로 간다. */
    data object Passed : MigrationUiState
}

class MigrationViewModel(private val migration: LegacyMigration) : ViewModel() {
    private val _state = MutableStateFlow<MigrationUiState>(MigrationUiState.Checking)
    val state: StateFlow<MigrationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val needed = withContext(Dispatchers.Default) { migration.needsMigration() }
            if (needed) start() else _state.value = MigrationUiState.Passed
        }
    }

    /** 실패 화면의 다시 시도·다시 확인. */
    fun retry() = start()

    /** 결과 화면의 시작하기. 결과는 한 번만 보인다. */
    fun acknowledgeResult() {
        _state.value = MigrationUiState.Passed
    }

    private fun start() {
        _state.value = MigrationUiState.Preparing
        viewModelScope.launch {
            // 진행 값은 0.25초마다 옮겨 담는다(contracts/migration-flow.md).
            val ticker =
                launch {
                    while (isActive) {
                        val p = migration.progress.value
                        if (p.step == MigrationStep.COPYING || p.step == MigrationStep.VERIFYING || p.step == MigrationStep.COMMITTING) {
                            _state.value = MigrationUiState.Copying(p.entriesDone, p.entriesTotal, p.photosDone, p.photosTotal)
                        }
                        delay(PROGRESS_MILLIS)
                    }
                }
            val result = migration.run()
            ticker.cancel()
            _state.value =
                when (result) {
                    is MigrationResult.Finished -> if (result.showResult) {
                        MigrationUiState.Result(
                            result.report
                        )
                    } else {
                        MigrationUiState.Passed
                    }

                    is MigrationResult.Failed -> MigrationUiState.Failed(result.kind, result.neededBytes)

                    is MigrationResult.AlreadyFinished, MigrationResult.NoLegacy -> MigrationUiState.Passed
                }
        }
    }

    private companion object {
        const val PROGRESS_MILLIS = 250L
    }
}
