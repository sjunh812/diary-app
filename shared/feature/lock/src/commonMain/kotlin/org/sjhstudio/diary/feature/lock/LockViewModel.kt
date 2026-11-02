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
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry

/** 잠금 화면(L1) 상태. 입력한 숫자는 담지 않고 몇 칸 찼는지만 둔다. */
data class LockUiState(val entered: Int = 0, val message: LockMessage = LockMessage.None, val checking: Boolean = false)

sealed interface LockMessage {
    data object None : LockMessage

    data class Wrong(val remaining: Int) : LockMessage

    data class Waiting(val seconds: Int) : LockMessage
}

/** 넣은 비밀번호로 무엇을 하는가. 잠금 화면은 [Verify], 잠금 설정의 "잠금 사용" 끄기는 [Disable]이다. */
enum class PinCheck { Verify, Disable }

sealed interface LockEvent {
    /** 비밀번호가 맞았다([PinCheck.Disable]이면 잠금도 껐다). */
    data object Unlocked : LockEvent

    /** 비밀번호를 대어 볼 수 없다. 폰 화면 잠금 확인으로 간다. */
    data object KeyMissing : LockEvent
}

/**
 * 잠금 화면의 숫자 입력과 확인(contracts/lock.md).
 * 입력한 숫자는 글자 배열에만 두고 확인하자마자 지운다.
 */
class LockViewModel(
    private val repository: LockRepository,
    private val clock: Clock = Clock.System,
    /** 결과(`ok`·`wrong`·`waiting`)만 보낸다. 비밀번호와 횟수는 보내지 않는다(contracts/lock.md). */
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    private val digits = CharArray(Pin.LENGTH)
    private var count = 0
    private var ticker: Job? = null
    private var check = PinCheck.Verify

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

    /** 잠금 설정에서 지금 비밀번호를 다시 받을 때 처음부터 시작한다. */
    fun start(check: PinCheck) {
        this.check = check
        clearDigits()
        if (_state.value.message !is LockMessage.Waiting) _state.value = LockUiState()
    }

    /** 비밀번호를 대어 볼 수 없고 폰에도 화면 잠금이 없을 때 잠금을 끈다. */
    suspend fun turnOffWithoutPin(): Boolean = repository.turnOffWithoutPin()

    private fun check() {
        val pin = Pin.of(digits)
        clearDigits()
        if (pin == null) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            val result = if (check == PinCheck.Disable) repository.disable(pin, clock.now()) else repository.verify(pin, clock.now())
            lockResult(result)?.let { telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to it)) }
            when (result) {
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

    private fun lockResult(result: VerifyResult): String? = when (result) {
        VerifyResult.Ok -> "ok"
        is VerifyResult.Wrong -> "wrong"
        is VerifyResult.Waiting -> "waiting"
        VerifyResult.KeyMissing -> null
    }

    /** 생체 인증 결과. 열렸으면 `biometric_ok`, 사용자가 닫았으면 `biometric_cancel`. */
    fun logBiometric(opened: Boolean) =
        telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to if (opened) "biometric_ok" else "biometric_cancel"))

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
