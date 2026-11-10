package org.sjhstudio.diary.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 휴지통(FR-034).
 * 처음 읽기 전에는 null이라 빈 상태가 잠깐 비치지 않음.
 * 읽지 못하면 [failed]가 참이 되어 다시 시도를 보임.
 */
class TrashViewModel(private val trash: TrashRepository) : ViewModel() {
    private val attempt = MutableStateFlow(0)
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<TrashedEntry>?> =
        attempt.flatMapLatest {
            trash.items()
                .onStart { _failed.value = false }
                .catch { if (it is Exception) _failed.value = true else throw it }
        }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    fun retry() {
        attempt.value++
    }

    // 복원이 끝남. 실패하면 앱 틀이 실패를 알리고 이 이벤트는 오지 않음
    private val _restored = Channel<String>(Channel.BUFFERED)
    val restored: Flow<String> = _restored.receiveAsFlow()

    fun restore(id: String) {
        viewModelScope.launchWrite {
            trash.restore(id)
            _restored.send(id)
        }
    }

    fun deleteForever(id: String) {
        viewModelScope.launchWrite { trash.deleteForever(listOf(id)) }
    }

    fun empty() {
        viewModelScope.launchWrite { trash.empty() }
    }
}
