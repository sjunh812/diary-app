package org.sjhstudio.diary.app.lock

import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.platform.AppForeground

/**
 * 앱이 잠겼는지 정한다(contracts/lock.md, research R13).
 * 프로세스와 함께 사는 하나뿐인 객체라, 프로세스가 새로 시작하면 늘 잠긴 채로 시작한다.
 * 다시 잠그기는 단조 시계로 잰다.
 * 기기 시각을 바꿔도 피할 수 없다.
 */
class LockController(
    private val repository: LockRepository,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    private val observeForeground: (onBackground: () -> Unit, onForeground: () -> Unit) -> Unit = { background, foreground ->
        AppForeground.observe(background, foreground)
    }
) {
    private val _state = MutableStateFlow<LockState>(LockState.Starting)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private var lock = LockSettings()
    private var leftAt: ComparableTimeMark? = null
    private var holds = 0
    private var watching = false

    /**
     * 잠금이 켜져 있는가.
     * 최근 앱 가림이 본다.
     */
    val lockEnabled: Boolean
        get() = lock.enabled

    init {
        // 다시 잠그기 시간과 켜짐만 따라간다.
        // 잠금을 여는 일은 [onUnlocked]로만 한다.
        scope.launch { repository.settings.collect { lock = it } }
    }

    /**
     * 시작 단계가 읽은 잠금으로 첫 상태를 정한다.
     * 한 번만 쓴다.
     */
    fun onColdStart(initial: LockSettings) {
        if (_state.value != LockState.Starting) return
        lock = initial
        if (!initial.enabled) {
            _state.value = LockState.Unlocked
            return
        }
        _state.value = if (initial.hasPin) LockState.Locked else LockState.NeedsNewPin
        if (initial.hasPin) scope.launch { if (repository.needsNewPin()) onKeyMissing() }
    }

    /**
     * 앱이 뒤로 가고 돌아오는 때를 듣기 시작한다. 메인 스레드에서 부른다.
     * 프로세스가 끝날 때까지 듣고 그만 듣지 않는다.
     * Android 11까지는 맨 앞 화면에서 뒤로 가면 화면이 먼저 사라지고 프로세스는 남는다.
     * 화면이 살아 있는 동안만 들으면 앱이 뒤로 간 것을 놓쳐, 다시 열 때 잠기지 않는다(헌법 II).
     */
    fun watchForeground() {
        if (watching) return
        watching = true
        observeForeground(::onBackground, ::onForeground)
    }

    fun onBackground() {
        if (holds == 0) leftAt = timeSource.markNow()
    }

    fun onForeground() {
        val left = leftAt ?: return
        leftAt = null
        val after = lock.relockAfter.duration ?: return
        if (lock.enabled && _state.value == LockState.Unlocked && left.elapsedNow() >= after) _state.value = LockState.Locked
    }

    fun onUnlocked() {
        _state.value = LockState.Unlocked
    }

    /**
     * 비밀번호를 대어 볼 수 없다(키 없음).
     * 폰 화면 잠금 확인 뒤 새 비밀번호를 정한다.
     */
    fun onKeyMissing() {
        if (_state.value != LockState.Unlocked) _state.value = LockState.NeedsNewPin
    }

    /**
     * 폰에 화면 잠금이 없는데 키도 잠깐 쓸 수 없어 잠금을 끄지 못했다.
     * 열지 않고 잠금 화면으로 돌아간다.
     * 다음에 비밀번호를 대어 볼 때 다시 본다.
     */
    fun onKeepLocked() {
        if (_state.value == LockState.NeedsNewPin) _state.value = LockState.Locked
    }

    /**
     * 폰 화면 잠금 확인처럼 앱이 잠시 뒤로 가는 동안 다시 잠그지 않는다.
     * Android의 폰 잠금 확인은 다른 앱 화면이라 앱이 백그라운드로 간 것으로 잡힌다.
     */
    suspend fun <T> withoutRelock(block: suspend () -> T): T {
        holds++
        return try {
            block()
        } finally {
            holds--
            leftAt = null
        }
    }
}

sealed interface LockState {
    /**
     * 시작 단계가 아직 잠금을 읽지 않았다.
     * 아무것도 그리지 않는다.
     */
    data object Starting : LockState

    data object Unlocked : LockState

    data object Locked : LockState

    /**
     * 비밀번호를 대어 볼 수 없다.
     * 폰 화면 잠금 확인 뒤 새 비밀번호를 정한다.
     */
    data object NeedsNewPin : LockState
}
