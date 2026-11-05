package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.RestorePreparation

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
        viewModelScope.launch { _state.value = restore.backups()?.let { RestoreListUi.Loaded(it) } ?: RestoreListUi.Failed }
    }
}

class RestorePreviewViewModel(private val snapshotName: String, private val restore: RestoreRepository) : ViewModel() {
    private val _state = MutableStateFlow<RestorePreparation?>(null)

    /** null이면 받아서 확인하는 중이다. */
    val state: StateFlow<RestorePreparation?> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = null
        viewModelScope.launch { _state.value = restore.prepare(snapshotName) }
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

    /** 복원하지 않고 나가면 받아 둔 일기 사본을 지운다. */
    override fun onCleared() {
        if (!restoring) restore.dismiss()
    }
}

class RestoreProgressViewModel(private val restore: RestoreRepository) : ViewModel() {
    val state = restore.state

    fun retry() = restore.merge()

    fun dismiss() = restore.dismiss()
}
