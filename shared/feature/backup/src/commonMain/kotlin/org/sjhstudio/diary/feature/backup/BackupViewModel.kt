package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // 홈 줄, 백업 화면, 백업 설정이 같은 상태를 함께 봄
class BackupViewModel(private val controller: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    private val statusAttempt = MutableStateFlow(0)

    private val statusRead: StateFlow<StatusRead> =
        statusAttempt.flatMapLatest {
            controller.status
                .map<BackupStatus, StatusRead> { StatusRead.Loaded(it) }
                .onStart { emit(StatusRead.Loading) }
                .catch { if (it is Exception) emit(StatusRead.Failed) else throw it }
        }.stateInSafely(viewModelScope, WhileUiSubscribed, StatusRead.Loading)

    /**
     * 저장해 둔 상태를 읽기 전이나 읽지 못하면 null.
     * 기본값을 그리면 켜 둔 백업이 잠깐 또는 계속 꺼짐으로 보여 홈 줄과 백업 화면은 이 값을 봄
     */
    val loadedStatus: StateFlow<BackupStatus?> =
        statusRead.map { (it as? StatusRead.Loaded)?.status }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    /** 상태를 읽지 못함. 홈 줄과 백업 화면이 다시 시도를 보임 */
    val statusFailed: StateFlow<Boolean> =
        statusRead.map { it == StatusRead.Failed }.stateInSafely(viewModelScope, WhileUiSubscribed, false)

    /** 백업 설정(B13)처럼 연결한 뒤에만 여는 화면이 봄 */
    val status: StateFlow<BackupStatus> =
        statusRead.map {
            (it as? StatusRead.Loaded)?.status ?: BackupStatus()
        }.stateInSafely(viewModelScope, WhileUiSubscribed, BackupStatus())

    fun retryStatus() = statusAttempt.update { it + 1 }

    /** 지금 백업이 올리지 않고 끝났을 때 한 번 옴 */
    val skipped: Flow<BackupSkip> = controller.skipped

    val nudge: StateFlow<BackupNudge?> = controller.nudge.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** 바꾸기 뒤 되돌릴 수 있는 동안만 값이 있음 */
    val rollback: StateFlow<RollbackInfo?> = restore.rollback.stateInSafely(
        viewModelScope,
        WhileUiSubscribed,
        null
    )

    fun rollBack() {
        viewModelScope.launchSafely { restore.rollBack() }
    }

    /** 복원한 사진을 받는 동안만 값이 있음 */
    val photoProgress = restore.photoProgress

    /** 연결한 계정의 드라이브에 있던 가장 최근 백업 */
    val found: StateFlow<BackupEntry?> = controller.foundBackup

    /** 방금 연결에 실패한 까닭 */
    val connectFailure: StateFlow<BackupFailure?> = controller.connectFailure

    fun dismissNudge() {
        viewModelScope.launchSafely { quietly { controller.dismissNudge() } }
    }

    /**
     * [hint]는 예전 앱이 쓰던 구글 계정
     * Android에서 먼저 보임
     */
    fun connect(hint: String? = null) {
        viewModelScope.launchSafely {
            _connecting.value = true
            try {
                // 연결 실패 까닭은 [connectFailure]가 말함. 예상하지 못한 예외에도 앱을 끄지 않음
                quietly { controller.connect(hint) }
            } finally {
                _connecting.value = false
            }
        }
    }

    /**
     * [나중에]
     * 이때 이 기기의 첫 백업을 함
     */
    fun skipFound() = controller.skipFoundBackup()

    fun takeFound() = controller.takeFoundBackup()

    fun connectFailureShown() = controller.connectFailureShown()

    fun backUpNow(cellularOnce: Boolean = false) = controller.backUpNow(cellularOnce)

    fun setAuto(enabled: Boolean) {
        viewModelScope.launchSafely { quietly { controller.setAuto(enabled) } }
    }

    fun setPhotosOnCellular(enabled: Boolean) {
        viewModelScope.launchSafely { quietly { controller.setPhotosOnCellular(enabled) } }
    }

    fun disconnect() {
        viewModelScope.launchSafely { quietly { controller.disconnect() } }
    }

    /**
     * 저장 공간이 차서 설정을 적지 못해도 앱을 끄지 않음
     * 스위치는 저장된 값을 그려 적지 못하면 원래 자리로 돌아감. 사실과 어긋나지 않음
     */
    private suspend fun quietly(block: suspend () -> Unit) = orElseOnFailure(Unit) { block() }
}

private sealed interface StatusRead {
    data object Loading : StatusRead

    data class Loaded(val status: BackupStatus) : StatusRead

    data object Failed : StatusRead
}
