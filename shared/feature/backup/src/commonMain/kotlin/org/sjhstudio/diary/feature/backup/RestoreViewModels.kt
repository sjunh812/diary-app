package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.BackupList
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
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

    /** [retrying]이면 실패 화면을 둔 채 버튼에서 기다림. 화면 전체를 로딩으로 바꾸면 빨리 실패할 때 깜빡임 */
    data class Failed(val kind: BackupFailure, val retrying: Boolean = false) : RestoreListUi

    data class Loaded(val entries: List<BackupEntry>) : RestoreListUi
}

class RestoreListViewModel(private val restore: RestoreRepository) : ViewModel() {
    private val _state = MutableStateFlow<RestoreListUi>(RestoreListUi.Loading)
    val state: StateFlow<RestoreListUi> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val failed = _state.value as? RestoreListUi.Failed
        _state.value = failed?.copy(retrying = true) ?: RestoreListUi.Loading
        viewModelScope.launchSafely(onFailure = { _state.value = RestoreListUi.Failed(BackupFailure.UNKNOWN) }) {
            // 드라이브에 묻는 데 몇 초가 걸려 지난번 목록을 먼저 보여 줌
            // 다시 시도할 때는 실패 화면에서 목록으로 잠깐 바뀌었다 돌아오지 않게 보이지 않음
            if (failed == null) restore.savedBackups().takeIf { it.isNotEmpty() }?.let { _state.value = RestoreListUi.Loaded(it) }
            _state.value =
                when (val listed = restore.backups()) {
                    is BackupList.Loaded -> RestoreListUi.Loaded(listed.entries)
                    is BackupList.Failed -> RestoreListUi.Failed(listed.kind)
                }
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
