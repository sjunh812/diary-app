package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

sealed interface RestoreListUi {
    data object Loading : RestoreListUi

    data object Failed : RestoreListUi

    data class Loaded(val entries: List<BackupEntry>) : RestoreListUi
}

class RestoreListViewModel(private val restore: RestoreRepository) : ViewModel() {
    private val _state = MutableStateFlow<RestoreListUi>(RestoreListUi.Loading)
    val state: StateFlow<RestoreListUi> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = RestoreListUi.Loading
        viewModelScope.launchSafely {
            // 드라이브에 묻는 데 몇 초가 걸려 지난번 목록을 먼저 보여 줌
            restore.savedBackups().takeIf { it.isNotEmpty() }?.let { _state.value = RestoreListUi.Loaded(it) }
            _state.value = restore.backups()?.let { RestoreListUi.Loaded(it) } ?: RestoreListUi.Failed
        }
    }
}

class RestorePreviewViewModel(private val snapshotName: String, private val restore: RestoreRepository) : ViewModel() {
    private val _state = MutableStateFlow<RestorePreparation?>(null)

    /** null이면 받아서 확인하는 중 */
    val state: StateFlow<RestorePreparation?> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = null
        // 앱이 꺼졌다 돌아와 목록부터 다시 받다가 끊겨도 앱을 끄지 않고 실패 화면에서 다시 시도
        viewModelScope.launchSafely {
            _state.value = orElseOnFailure(RestorePreparation.Failed(RestoreFailure.OTHER)) { restore.prepare(snapshotName) }
        }
    }

    private var restoring = false

    fun merge() {
        restoring = true
        restore.merge()
    }

    fun replace() {
        restoring = true
        restore.replace()
    }

    /** 복원하지 않고 나가면 받아 둔 일기 사본을 지움 */
    override fun onCleared() {
        if (!restoring) restore.dismiss()
    }
}

class RestoreStatusViewModel(private val restore: RestoreRepository) : ViewModel() {
    val waiting: StateFlow<WaitingPhotos?> = restore.waitingPhotos.stateInSafely(
        viewModelScope,
        WhileUiSubscribed,
        null
    )

    val rollback: StateFlow<RollbackRun> = restore.rollbackRun

    val folderRecreated: StateFlow<Boolean> =
        restore.folderRecreated.stateInSafely(viewModelScope, WhileUiSubscribed, false)

    fun dismissFolderRecreated() = restore.dismissFolderRecreated()

    fun fetchNow() = restore.fetchPhotosNow()

    fun rollBack() {
        viewModelScope.launchSafely { restore.rollBack() }
    }
}

class RestoreProgressViewModel(private val restore: RestoreRepository) : ViewModel() {
    val state = restore.state

    fun retry() = restore.retry()

    fun dismiss() = restore.dismiss()
}
