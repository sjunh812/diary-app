package org.sjhstudio.diary.feature.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.migration.FollowUpResult
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.MigrationResult
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

/** 관문이 보는 이전 화면 상태(contracts/migration-flow.md "화면 상태") */
sealed interface MigrationUiState {
    /**
     * 원본 확인 중
     * 바탕색만 보임(보통 1초 안)
     */
    data object Checking : MigrationUiState

    /**
     * 사본을 만드는 중(M0)
     * 끝을 모르는 진행 표시
     */
    data object Preparing : MigrationUiState

    /**
     * 옮기고 검증하는 중(M1)
     * 일기 내용과 사진은 없음
     */
    data class Copying(val entriesDone: Int, val entriesTotal: Int, val photosDone: Int, val photosTotal: Int) : MigrationUiState

    /** 끝난 직후 한 번(M2, M2-b) */
    data class Result(val report: MigrationReport) : MigrationUiState

    /**
     * 실패(M3)
     * 공간 부족 말고는 새 일기부터 쓰기로 앱 틀로 갈 수 있음
     * [hasCopy]: 예전 일기 보기가 읽을 사본이 있는지
     */
    data class Failed(val kind: MigrationFailureKind, val shortfallBytes: Long?, val hasCopy: Boolean = false) : MigrationUiState

    /**
     * 옮길 것이 없거나 끝남
     * 앱 틀로 감
     */
    data object Passed : MigrationUiState
}

/**
 * 관문을 지난 뒤 한 번 알리는 일(FR-011)
 * 앱 틀이 토스트로 띄움
 */
sealed interface MigrationNotice {
    data class EntriesRecovered(val count: Int) : MigrationNotice

    data class PhotosFound(val count: Int) : MigrationNotice
}

class MigrationViewModel(
    private val migration: LegacyMigration,
    /**
     * 이전이 끝난 기기의 후속 작업(`MigrationFollowUp.run`)
     * 관문을 지난 뒤 한 번만 부름
     */
    private val followUp: suspend () -> FollowUpResult,
    /**
     * 파일을 읽는 일을 돌리는 곳
     * 시험에서는 시험 디스패처로 바꿈
     */
    private val background: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _state = MutableStateFlow<MigrationUiState>(MigrationUiState.Checking)
    val state: StateFlow<MigrationUiState> = _state.asStateFlow()

    private val _notices = Channel<MigrationNotice>(Channel.BUFFERED)
    val notices: Flow<MigrationNotice> = _notices.receiveAsFlow()
    private var followedUp = false
    private var running: Job? = null

    init {
        viewModelScope.launchSafely {
            // 있는지 알 수 없으면 있다고 보고 이전에 맡김
            // 그쪽 실패는 실패 화면으로 감
            val needed =
                try {
                    withContext(background) { migration.needsMigration() }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    true
                }
            if (needed) start() else pass()
        }
    }

    /** 실패 화면의 다시 시도·다시 확인 */
    fun retry() = start()

    /**
     * 실패 화면의 새 일기부터 쓰기
     * 예전 일기는 설정에서 다시 옮기고, 앱이 업데이트되면 저절로 다시 시도
     */
    fun skip() {
        viewModelScope.launchSafely {
            // 공간이 차 미뤄 뒀다고 적지 못해도 앱은 엶
            // 다음에 앱을 열면 이전부터 다시 봄
            orElseOnFailure(Unit) { withContext(background) { migration.defer() } }
            pass()
        }
    }

    /**
     * 결과 화면의 시작하기
     * 결과는 한 번만 보임
     */
    fun acknowledgeResult() = pass()

    private fun pass() {
        _state.value = MigrationUiState.Passed
        if (followedUp) return
        followedUp = true
        viewModelScope.launchSafely {
            // 후속 작업이 실패해도 일기 쓰기·읽기는 막지 않음
            // 다음에 앱을 열 때 다시 봄
            val result =
                orElseOnFailure(null) { withContext(background) { followUp() } } ?: return@launchSafely
            if (result.entriesRecovered > 0) _notices.send(MigrationNotice.EntriesRecovered(result.entriesRecovered))
            if (result.photosFound > 0) _notices.send(MigrationNotice.PhotosFound(result.photosFound))
        }
    }

    private fun start() {
        // 다시 시도를 연달아 눌러도 하나만 돎
        if (running?.isActive == true) return
        _state.value = MigrationUiState.Preparing
        running = viewModelScope.launchSafely {
            // 진행 값은 0.25초마다 옮겨 담음(contracts/migration-flow.md)
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
            // 어떤 실패든 앱을 끄지 않고 실패 화면으로 보냄
            val result =
                try {
                    migration.run()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    MigrationResult.Failed(MigrationStep.SNAPSHOTTING, MigrationFailureKind.UNKNOWN)
                }
            ticker.cancel()
            // 결과를 보여 줄 것이 없으면 바로 앱 틀로 감
            val next =
                when (result) {
                    is MigrationResult.Finished -> if (result.showResult) MigrationUiState.Result(result.report) else null
                    is MigrationResult.Failed -> MigrationUiState.Failed(result.kind, result.neededBytes, hasCopy())
                    is MigrationResult.AlreadyFinished, MigrationResult.NoLegacy -> null
                }
            if (next == null) pass() else _state.value = next
        }
    }

    /** 사본을 볼 수 없으면 없는 것처럼 줄을 숨김 */
    private suspend fun hasCopy(): Boolean = try {
        withContext(background) { migration.hasLegacyCopy() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val PROGRESS_MILLIS = 250L
    }
}
