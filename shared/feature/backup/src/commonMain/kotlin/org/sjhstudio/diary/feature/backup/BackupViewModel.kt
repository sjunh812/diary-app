package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

class BackupViewModel(private val controller: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    val status: StateFlow<BackupStatus> = controller.status.stateInSafely(
        viewModelScope,
        WhileUiSubscribed,
        BackupStatus()
    )

    /**
     * 저장해 둔 상태를 읽기 전에는 null이다.
     * 앱이 뜨는 순간 켜 둔 백업이 꺼짐으로 보이지 않게 홈 줄은 이 값을 본다.
     */
    val loadedStatus: StateFlow<BackupStatus?> = controller.status
        .map<BackupStatus, BackupStatus?> { it }
        .stateInSafely(viewModelScope, WhileUiSubscribed, null)

    /** 지금 백업이 올리지 않고 끝났을 때 한 번 온다. */
    val skipped: Flow<BackupSkip> = controller.skipped

    val nudge: StateFlow<BackupNudge?> = controller.nudge.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** 바꾸기 뒤 되돌릴 수 있는 동안만 값이 있다. */
    val rollback: StateFlow<RollbackInfo?> = restore.rollback.stateInSafely(
        viewModelScope,
        WhileUiSubscribed,
        null
    )

    fun rollBack() {
        viewModelScope.launchSafely { restore.rollBack() }
    }

    /** 복원한 사진을 받는 동안만 값이 있다. */
    val photoProgress = restore.photoProgress

    /** 연결한 계정의 드라이브에 있던 가장 최근 백업. */
    val found: StateFlow<BackupEntry?> = controller.foundBackup

    /** 방금 연결에 실패한 까닭. */
    val connectFailure: StateFlow<BackupFailure?> = controller.connectFailure

    fun dismissNudge() {
        viewModelScope.launchSafely { quietly { controller.dismissNudge() } }
    }

    /**
     * [hint]는 예전 앱이 쓰던 구글 계정이다.
     * Android에서 먼저 보인다.
     */
    fun connect(hint: String? = null) {
        viewModelScope.launchSafely {
            _connecting.value = true
            try {
                // 연결 실패 까닭은 [connectFailure]가 말한다. 예상하지 못한 예외에도 앱을 끄지 않는다.
                quietly { controller.connect(hint) }
            } finally {
                _connecting.value = false
            }
        }
    }

    /**
     * [나중에].
     * 이때 이 기기의 첫 백업을 한다.
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
     * 저장 공간이 차서 설정을 적지 못해도 앱을 끄지 않는다.
     * 스위치는 저장된 값을 그리므로 적지 못하면 원래 자리로 돌아가 사실과 어긋나지 않는다.
     */
    private suspend fun quietly(block: suspend () -> Unit) = orElseOnFailure(Unit) { block() }
}
