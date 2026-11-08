package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSink
import okio.Sink
import okio.Source
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.orElseOnFailure

sealed interface FileBackupUi {
    data object Idle : FileBackupUi

    data class Writing(val done: Int, val total: Int) : FileBackupUi

    data class Saved(val name: String, val bytes: Long) : FileBackupUi

    /** [left]면 쓰다 만 파일을 지우지 못했다. 사용자가 지워야 한다. */
    data class WriteFailed(val left: Boolean) : FileBackupUi

    data object Reading : FileBackupUi

    /** [bytesShort]는 공간이 얼마나 더 있어야 하는지다. 모르면 0이다. */
    data class ReadFailed(val reason: RestoreFailure, val bytesShort: Long = 0) : FileBackupUi
}

class FileBackupViewModel(private val restore: RestoreRepository) : ViewModel() {
    private val _estimate = MutableStateFlow<BundleEstimate?>(null)
    val estimate: StateFlow<BundleEstimate?> = _estimate.asStateFlow()

    private val _state = MutableStateFlow<FileBackupUi>(FileBackupUi.Idle)
    val state: StateFlow<FileBackupUi> = _state.asStateFlow()

    init {
        // 크기는 미리 알려 주는 곁일이라 읽지 못하면 비워 두고 파일 만들기는 그대로 할 수 있다.
        viewModelScope.launch { _estimate.value = orElseOnFailure(null) { restore.bundleEstimate() } }
    }

    private var writing: Job? = null

    /**
     * 저장 위치가 정해지면 부른다.
     * [open]은 그곳에 쓰는 흐름을 연다.
     * 끝까지 쓰지 못하면 [discard]로 지운다.
     * 쓰다 만 파일이 완성본 이름으로 남아 나중에 복원을 시도하게 두지 않는다.
     */
    fun write(name: String, open: () -> BufferedSink, discard: suspend () -> Unit = {}) {
        _state.value = FileBackupUi.Writing(0, 1)
        writing =
            viewModelScope.launch {
                _state.value =
                    try {
                        val counted = CountingSink(open())
                        counted.buffer().use { sink ->
                            restore.writeBundle(sink) { done, total ->
                                _state.value =
                                    FileBackupUi.Writing(done, total)
                            }
                        }
                        FileBackupUi.Saved(name, counted.written)
                    } catch (error: CancellationException) {
                        withContext(NonCancellable) { removed(discard) }
                        throw error
                    } catch (_: Exception) {
                        FileBackupUi.WriteFailed(left = !removed(discard))
                    }
            }
    }

    private suspend fun removed(discard: suspend () -> Unit): Boolean = try {
        discard()
        true
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    /** 저장 창을 열지 못했다. */
    fun saveFailed() {
        _state.value = FileBackupUi.WriteFailed(left = false)
    }

    /** 파일 고르기 창을 열지 못했거나 고른 파일을 받지 못했다. */
    fun pickFailed() {
        _state.value = FileBackupUi.ReadFailed(RestoreFailure.OTHER)
    }

    /** 사용자가 나가기로 해서 멈춘다. 만들던 파일은 지운다. */
    fun stop() {
        writing?.cancel()
        restore.forgetStoppedBundle()
        _state.value = FileBackupUi.Idle
    }

    /**
     * 고른 파일을 앱 안으로 옮겨 확인한다.
     * 끝나면 [onReady]로 미리보기에 간다.
     * [size]는 고른 파일의 크기다.
     * 몰라도 옮기다 공간이 차면 그렇게 알린다.
     */
    fun read(open: () -> Source, size: () -> Long? = { null }, onReady: () -> Unit) {
        _state.value = FileBackupUi.Reading
        viewModelScope.launch {
            val result =
                try {
                    open().use { source -> restore.prepareFile(source, withContext(Dispatchers.IO) { size() }) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    RestorePreparation.Failed(RestoreFailure.MISMATCH)
                }
            when (result) {
                is RestorePreparation.Ready -> {
                    _state.value = FileBackupUi.Idle
                    onReady()
                }

                is RestorePreparation.Failed -> _state.value = FileBackupUi.ReadFailed(result.reason, result.bytesShort)
            }
        }
    }

    fun clear() {
        _state.value = FileBackupUi.Idle
    }
}

/**
 * 실제로 쓴 크기를 센다.
 * 끝난 화면에 어림값이 아니라 파일 크기를 보이려고 쓴다.
 */
private class CountingSink(private val delegate: Sink) : Sink by delegate {
    var written = 0L
        private set

    override fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
        written += byteCount
    }
}
