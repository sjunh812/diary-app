package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.RollbackInfo

class BackupViewModel(private val controller: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    val status: StateFlow<BackupStatus> = controller.status.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_MILLIS),
        BackupStatus()
    )

    /**
     * 저장해 둔 상태를 읽기 전에는 null이다.
     * 앱이 뜨는 순간 켜 둔 백업이 꺼짐으로 보이지 않게 홈 줄은 이 값을 본다.
     */
    val loadedStatus: StateFlow<BackupStatus?> = controller.status
        .map<BackupStatus, BackupStatus?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    /** 지금 백업이 올리지 않고 끝났을 때 한 번 온다. */
    val skipped: Flow<BackupSkip> = controller.skipped

    val nudge: StateFlow<BackupNudge?> = controller.nudge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** 바꾸기 뒤 되돌릴 수 있는 동안만 값이 있다. */
    val rollback: StateFlow<RollbackInfo?> = restore.rollback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    fun rollBack() {
        viewModelScope.launch { restore.rollBack() }
    }

    /** 연결한 계정의 드라이브에 있던 가장 최근 백업. */
    val found: StateFlow<BackupEntry?> = controller.foundBackup

    /** 방금 연결에 실패한 까닭. */
    val connectFailure: StateFlow<BackupFailure?> = controller.connectFailure

    fun dismissNudge() {
        viewModelScope.launch { controller.dismissNudge() }
    }

    /**
     * [hint]는 예전 앱이 쓰던 구글 계정이다.
     * Android에서 먼저 보인다.
     */
    fun connect(hint: String? = null) {
        viewModelScope.launch {
            _connecting.value = true
            try {
                controller.connect(hint)
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
        viewModelScope.launch { controller.setAuto(enabled) }
    }

    fun setPhotosOnCellular(enabled: Boolean) {
        viewModelScope.launch { controller.setPhotosOnCellular(enabled) }
    }

    fun disconnect() {
        viewModelScope.launch { controller.disconnect() }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
