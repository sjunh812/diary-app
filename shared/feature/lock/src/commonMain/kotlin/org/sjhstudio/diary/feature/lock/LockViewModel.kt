package org.sjhstudio.diary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.data.lock.VerifyResult
import org.sjhstudio.diary.core.model.Pin

/** 잠금 화면(L1) 상태. 입력한 숫자는 담지 않고 몇 칸 찼는지만 둔다. */
data class LockUiState(val entered: Int = 0, val message: LockMessage = LockMessage.None, val checking: Boolean = false)

sealed interface LockMessage {
    data object None : LockMessage

    data class Wrong(val remaining: Int) : LockMessage

    data class Waiting(val seconds: Int) : LockMessage
}

sealed interface LockEvent {
    data object Unlocked : LockEvent

    /** 비밀번호를 대어 볼 수 없다. 폰 화면 잠금 확인으로 간다. */
    data object KeyMissing : LockEvent
}

/**
 * 잠금 화면의 숫자 입력과 확인(contracts/lock.md).
 * 입력한 숫자는 글자 배열에만 두고 확인하자마자 지운다.
 */
class LockViewModel(private val repository: LockRepository, private val clock: Clock = Clock.System) : ViewModel() {
    private val digits = CharArray(Pin.LENGTH)
    private var count = 0
    private var ticker: Job? = null

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    private val _events = Channel<LockEvent>(Channel.BUFFERED)
    val events: Flow<LockEvent> = _events.receiveAsFlow()

    init {
        // 앱을 껐다 켜도 기다림이 이어진다(FR-020).
        viewModelScope.launch {
            val until = repository.settings.first().retryAfter
            if (until != null && clock.now() < until) waitUntil(until)
        }
    }

    fun onDigit(digit: Char) {
        val current = _state.value
        if (current.checking || current.message is LockMessage.Waiting || count >= Pin.LENGTH) return
        digits[count++] = digit
        // 틀린 뒤 새로 누르면 빨간 점과 안내를 지운다.
        _state.update { it.copy(entered = count, message = LockMessage.None) }
        if (count == Pin.LENGTH) check()
    }

    fun onDelete() {
        if (count == 0 || _state.value.checking) return
        digits[--count] = '\u0000'
        _state.update { it.copy(entered = count) }
    }

    /** 비밀번호를 대어 볼 수 없고 폰에도 화면 잠금이 없을 때 잠금을 끈다. */
    suspend fun turnOffWithoutPin(): Boolean = repository.turnOffWithoutPin()

    private fun check() {
        val pin = Pin.of(digits)
        clearDigits()
        if (pin == null) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            when (val result = repository.verify(pin, clock.now())) {
                VerifyResult.Ok -> {
                    _state.value = LockUiState()
                    _events.send(LockEvent.Unlocked)
                }

                is VerifyResult.Wrong ->
                    _state.value =
                        LockUiState(entered = Pin.LENGTH, message = LockMessage.Wrong(result.remainingBeforeWait))

                is VerifyResult.Waiting -> waitUntil(result.until)

                VerifyResult.KeyMissing -> {
                    _state.value = LockUiState()
                    _events.send(LockEvent.KeyMissing)
                }
            }
        }
    }

    private fun waitUntil(until: Instant) {
        ticker?.cancel()
        ticker =
            viewModelScope.launch {
                while (true) {
                    val left = until - clock.now()
                    if (!left.isPositive()) break
                    _state.value =
                        LockUiState(
                            message = LockMessage.Waiting(left.inWholeSeconds.toInt() + if (left.inWholeMilliseconds % 1000 > 0) 1 else 0)
                        )
                    delay(minOf(left, 1.seconds))
                }
                _state.value = LockUiState()
            }
    }

    private fun clearDigits() {
        digits.fill('\u0000')
        count = 0
    }

    override fun onCleared() = clearDigits()
}
