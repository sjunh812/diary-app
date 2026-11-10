package org.sjhstudio.diary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
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
import org.sjhstudio.diary.core.ui.launchSafely

/**
 * 잠금 화면(L1) 상태.
 * 입력한 숫자는 담지 않고 몇 칸 찼는지만 둔다.
 * [wrongCount]는 틀릴 때마다 는다.
 * 점이 흔들리는 신호라 같은 수로 두 번 틀려도 다시 흔들린다.
 */
data class LockUiState(
    val entered: Int = 0,
    val message: LockMessage = LockMessage.None,
    val checking: Boolean = false,
    val wrongCount: Int = 0
)

sealed interface LockMessage {
    data object None : LockMessage

    data class Wrong(val remaining: Int) : LockMessage

    data class Waiting(val seconds: Int) : LockMessage

    /** 기기 키를 쓸 수 없어 비밀번호를 대어 보지 못했다. */
    data object KeyUnavailable : LockMessage

    /** 비밀번호는 맞았지만 잠금을 끈 것을 저장하지 못했다. */
    data object NotSaved : LockMessage
}

/**
 * 넣은 비밀번호로 무엇을 하는가.
 * 잠금 화면은 [Verify], 잠금 설정의 "잠금 사용" 끄기는 [Disable]이다.
 */
enum class PinCheck { Verify, Disable }

sealed interface LockEvent {
    /** 비밀번호가 맞았다([PinCheck.Disable]이면 잠금도 껐다). */
    data object Unlocked : LockEvent

    /**
     * 비밀번호를 대어 볼 수 없다.
     * 폰 화면 잠금 확인으로 간다.
     */
    data object KeyMissing : LockEvent
}

/**
 * 잠금 화면의 숫자 입력과 확인(contracts/lock.md).
 * 입력한 숫자는 글자 배열에만 두고 확인하자마자 지운다.
 */
class LockViewModel(
    private val repository: LockRepository,
    private val clock: Clock = Clock.System,
    /**
     * 결과(`ok`·`wrong`·`waiting`)만 보낸다.
     * 비밀번호와 횟수는 보내지 않는다(contracts/lock.md).
     */
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
        viewModelScope.launchSafely {
            val until = repository.settings.first().retryAfter
            if (until != null && clock.now() < until) waitUntil(until)
        }
    }

    fun onDigit(digit: Char) {
        val current = _state.value
        if (current.checking || current.message is LockMessage.Waiting || count >= Pin.LENGTH) return
        digits[count++] = digit
        // 틀린 뒤 새로 누르면 안내를 지운다.
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

    /**
     * 비밀번호를 대어 볼 수 없고 폰에도 화면 잠금이 없을 때 잠금을 끈다.
     * 끄지 못했으면 false이고, 그때는 앱을 열면 안 된다.
     */
    suspend fun turnOffWithoutPin(): Boolean {
        val off =
            try {
                repository.turnOffWithoutPin()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        if (!off) _state.update { it.copy(message = LockMessage.KeyUnavailable, checking = false) }
        return off
    }

    private fun check() {
        val pin = Pin.of(digits)
        clearDigits()
        if (pin == null) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launchSafely {
            // 확인하다 실패하면 열지 않고 키를 쓸 수 없을 때의 길로 보낸다.
            val result =
                try {
                    if (check == PinCheck.Disable) repository.disable(pin, clock.now()) else repository.verify(pin, clock.now())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    VerifyResult.KeyMissing
                }
            lockResult(result)?.let { telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to it)) }
            when (result) {
                VerifyResult.Ok -> {
                    _state.value = LockUiState()
                    _events.send(LockEvent.Unlocked)
                }

                // 점은 흔들린 뒤 바로 비어 다시 누르면 된다.
                // 남은 횟수 안내는 다음 숫자를 누를 때까지 남는다.
                is VerifyResult.Wrong ->
                    _state.update {
                        LockUiState(message = LockMessage.Wrong(result.remainingBeforeWait), wrongCount = it.wrongCount + 1)
                    }

                is VerifyResult.Waiting -> waitUntil(result.until)

                VerifyResult.KeyMissing -> {
                    _state.value = LockUiState(message = LockMessage.KeyUnavailable)
                    _events.send(LockEvent.KeyMissing)
                }

                VerifyResult.NotSaved -> _state.value = LockUiState(message = LockMessage.NotSaved)
            }
        }
    }

    private fun lockResult(result: VerifyResult): String? = when (result) {
        VerifyResult.Ok -> "ok"
        is VerifyResult.Wrong -> "wrong"
        is VerifyResult.Waiting -> "waiting"
        VerifyResult.KeyMissing, VerifyResult.NotSaved -> null
    }

    /**
     * 생체 인증 결과.
     * 열렸으면 `biometric_ok`, 사용자가 닫았으면 `biometric_cancel`.
     */
    fun logBiometric(opened: Boolean) =
        telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to if (opened) "biometric_ok" else "biometric_cancel"))

    private fun waitUntil(until: Instant) {
        ticker?.cancel()
        ticker =
            viewModelScope.launchSafely {
                while (true) {
                    val left = waitLeft(until, clock.now())
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

/**
 * 남은 기다림.
 * 기기 시계를 뒤로 돌리면 저장된 시각이 먼 미래가 되므로 한 번의 기다림보다 길어지지 않게 자른다.
 */
internal fun waitLeft(until: Instant, now: Instant): Duration = (until - now).coerceAtMost(LockRepository.WAIT)
