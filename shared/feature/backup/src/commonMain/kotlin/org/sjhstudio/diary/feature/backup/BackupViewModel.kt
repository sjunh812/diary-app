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
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.ConnectResult

class BackupViewModel(private val controller: BackupRepository) : ViewModel() {
    val status: StateFlow<BackupStatus> = controller.status.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_MILLIS),
        BackupStatus()
    )

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    private val _connectFailed = MutableStateFlow(false)
    val connectFailed: StateFlow<Boolean> = _connectFailed.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            _connecting.value = true
            _connectFailed.value = controller.connect() is ConnectResult.Failed
            _connecting.value = false
        }
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
