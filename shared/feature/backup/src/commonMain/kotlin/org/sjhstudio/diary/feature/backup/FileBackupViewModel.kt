package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import okio.Sink
import okio.Source
import org.sjhstudio.diary.core.data.BundleSave
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.data.prepareFile
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

sealed interface FileBackupUi {
    data object Idle : FileBackupUi

    data class Writing(val done: Int, val total: Int) : FileBackupUi

    data class Saved(val name: String, val bytes: Long) : FileBackupUi

    /** [left]면 쓰다 만 파일을 지우지 못함. 사용자가 지워야 함 */
    data class WriteFailed(val left: Boolean) : FileBackupUi

    data object Reading : FileBackupUi

    /** [bytesShort]는 더 있어야 하는 공간. 모르면 0 */
    data class ReadFailed(val reason: RestoreFailure, val bytesShort: Long = 0) : FileBackupUi
}

class FileBackupViewModel(private val restore: RestoreRepository) : ViewModel() {
    private val _estimate = MutableStateFlow<BundleEstimate?>(null)
    val estimate: StateFlow<BundleEstimate?> = _estimate.asStateFlow()

    private val _state = MutableStateFlow<FileBackupUi>(FileBackupUi.Idle)
    val state: StateFlow<FileBackupUi> = _state.asStateFlow()

    init {
        // 크기는 미리 알려 주는 곁일이라 읽지 못하면 비워 둠. 파일 만들기는 그대로 됨
        viewModelScope.launchSafely { _estimate.value = orElseOnFailure(null) { restore.bundleEstimate() } }
    }

    private var writing: Job? = null

    /**
     * 저장 위치가 정해지면 부름.
     * [open]은 그곳에 쓰는 흐름을 열고, [discard]는 그 파일을 지움.
     * 열고 닫기와 실패한 파일 지우기는 저장소가 맡음.
     */
    fun write(name: String, open: () -> Sink, discard: suspend () -> Unit = {}) {
        _state.value = FileBackupUi.Writing(0, 1)
        writing =
            viewModelScope.launchSafely {
                val saved = restore.saveBundle(open, discard) { done, total -> _state.value = FileBackupUi.Writing(done, total) }
                _state.value =
                    when (saved) {
                        is BundleSave.Saved -> FileBackupUi.Saved(name, saved.bytes)
                        is BundleSave.Failed -> FileBackupUi.WriteFailed(saved.left)
                    }
            }
    }

    /** 저장 창을 열지 못함 */
    fun saveFailed() {
        _state.value = FileBackupUi.WriteFailed(left = false)
    }

    /** 파일 고르기 창을 열지 못했거나 고른 파일을 받지 못함 */
    fun pickFailed() {
        _state.value = FileBackupUi.ReadFailed(RestoreFailure.OTHER)
    }

    /** 사용자가 나가기로 해 멈춤. 만들던 파일은 지움 */
    fun stop() {
        writing?.cancel()
        restore.forgetStoppedBundle()
        _state.value = FileBackupUi.Idle
    }

    // 파일 확인이 끝남. 미리보기로 가는 신호
    private val _ready = Channel<Unit>(Channel.BUFFERED)
    val ready: Flow<Unit> = _ready.receiveAsFlow()

    /**
     * 고른 파일을 앱 안으로 옮겨 확인.
     * 끝나면 [ready]로 미리보기에 감.
     * [size]는 고른 파일의 크기.
     * 몰라도 옮기다 공간이 차면 그렇게 알림.
     */
    fun read(open: () -> Source, size: () -> Long? = { null }) {
        _state.value = FileBackupUi.Reading
        viewModelScope.launchSafely {
            when (val result = restore.prepareFile(open, size)) {
                is RestorePreparation.Ready -> {
                    _state.value = FileBackupUi.Idle
                    _ready.send(Unit)
                }

                is RestorePreparation.Failed -> _state.value = FileBackupUi.ReadFailed(result.reason, result.bytesShort)
            }
        }
    }

    fun clear() {
        _state.value = FileBackupUi.Idle
    }
}
