package org.sjhstudio.diary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.Pin

/**
 * 비밀번호 정하기(L2) 상태.
 * [again]이면 두 번째 입력이다.
 * [wrongCount]는 두 번 입력이 다를 때마다 는다(점이 흔들리는 신호).
 */
data class PinSetupUiState(
    val entered: Int = 0,
    val again: Boolean = false,
    val mismatch: Boolean = false,
    val saving: Boolean = false,
    val wrongCount: Int = 0,
    /** 기기 키를 만들지 못했거나 저장하지 못했다. 처음부터 다시 받는다. */
    val saveFailed: Boolean = false
)

/**
 * 새 비밀번호를 두 번 받아 같으면 정한다(contracts/lock.md).
 * 다르면 처음부터 다시 받는다.
 */
class PinSetupViewModel(private val repository: LockRepository) : ViewModel() {
    private val first = CharArray(Pin.LENGTH)
    private val second = CharArray(Pin.LENGTH)
    private var count = 0

    private val _state = MutableStateFlow(PinSetupUiState())
    val state: StateFlow<PinSetupUiState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** 비밀번호를 정했다. */
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun onDigit(digit: Char) {
        val current = _state.value
        if (current.saving || count >= Pin.LENGTH) return
        (if (current.again) second else first)[count++] = digit
        _state.value = current.copy(entered = count, mismatch = false, saveFailed = false)
        if (count < Pin.LENGTH) return
        count = 0
        when {
            !current.again -> _state.value = PinSetupUiState(again = true, wrongCount = current.wrongCount)

            first.contentEquals(second) -> save()

            else -> {
                clear()
                _state.value = PinSetupUiState(mismatch = true, wrongCount = current.wrongCount + 1)
            }
        }
    }

    fun onDelete() {
        if (count == 0 || _state.value.saving) return
        count--
        _state.value = _state.value.copy(entered = count)
    }

    /** 새로 정하기를 시작할 때 지난번 입력을 비운다. */
    fun reset() {
        clear()
        _state.value = PinSetupUiState()
    }

    /**
     * 두 번째 입력에서 뒤로 가면 첫 입력부터 다시 한다.
     * 첫 입력이었으면 false다.
     */
    fun back(): Boolean {
        if (!_state.value.again) return false
        clear()
        _state.value = PinSetupUiState()
        return true
    }

    private fun save() {
        val pin = Pin.of(first)
        clear()
        if (pin == null) return
        _state.value = PinSetupUiState(entered = Pin.LENGTH, again = true, saving = true)
        viewModelScope.launch {
            // 저장하지 못했으면 정해진 것이 없다.
            // 앱을 열지 않고 다시 받는다.
            try {
                repository.setPin(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = PinSetupUiState(saveFailed = true)
                return@launch
            }
            _done.send(Unit)
        }
    }

    private fun clear() {
        first.fill('\u0000')
        second.fill('\u0000')
        count = 0
    }

    override fun onCleared() = clear()
}
