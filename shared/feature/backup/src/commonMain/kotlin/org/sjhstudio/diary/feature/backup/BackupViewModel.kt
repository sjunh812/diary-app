package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.platform.ConnectResult

class BackupViewModel(private val controller: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    val status: StateFlow<BackupStatus> = controller.status.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_MILLIS),
        BackupStatus()
    )

    val nudge: StateFlow<BackupNudge?> = controller.nudge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** 바꾸기 뒤 되돌릴 수 있는 동안만 값이 있다. */
    val rollback: StateFlow<RollbackInfo?> = restore.rollback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    fun rollBack() {
        viewModelScope.launch { restore.rollBack() }
    }

    private val _found = MutableStateFlow<BackupEntry?>(null)

    /** 연결한 계정의 드라이브에 있던 가장 최근 백업. */
    val found: StateFlow<BackupEntry?> = _found.asStateFlow()

    private val _connectFailed = MutableStateFlow(false)
    val connectFailed: StateFlow<Boolean> = _connectFailed.asStateFlow()

    fun dismissNudge() {
        viewModelScope.launch { controller.dismissNudge() }
    }

    /** [hint]는 예전 앱이 쓰던 구글 계정이다. Android에서 먼저 보인다. */
    fun connect(hint: String? = null) {
        viewModelScope.launch {
            _connecting.value = true
            val result = controller.connect(hint)
            _connectFailed.value = result is ConnectResult.Failed
            _connecting.value = false
            // 새 휴대폰이면 드라이브에 있는 백업을 바로 권한다(B2).
            if (result is ConnectResult.Connected) {
                _found.value = restore.backups()?.firstOrNull { it.state == BackupEntryState.Ready && it.entryCount > 0 }
            }
        }
    }

    fun foundShown() {
        _found.value = null
    }

    fun connectFailureShown() {
        _connectFailed.value = false
    }

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
