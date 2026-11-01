package org.sjhstudio.diary.app

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.LockSettings

/**
 * 앱이 처음 그리기 전에 끝내야 하는 일(contracts/app-shell.md 시작 순서 1).
 * 설정과 잠금 파일을 읽어 첫 화면의 테마와 잠금 여부를 정한다.
 * 일기 DB(Room)는 열지 않는다. 목표는 300ms 안이다.
 * 옛 앱 잠금 잇기는 잠금을 읽기 전에 끝낸다(research R16). 옛 설정 잇기는 스펙 001 US3이 여기에 더한다.
 */
class Startup(
    /** 파일 폴더 만들기(`AppFiles.ensureDirectories`). */
    private val prepareStorage: () -> Unit,
    private val settingsStore: SettingsStore,
    private val lockStore: LockStore,
    private val scope: CoroutineScope,
    /** 옛 앱(1.7.x) 잠금 잇기(`LegacyLockBootstrap`). 옛 앱이 없는 iOS는 아무것도 하지 않는다. */
    private val bootstrapLegacyLock: suspend () -> Unit = {}
) {
    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    /** 스플래시를 거둘 때를 정한다(Android `setKeepOnScreenCondition`). */
    val ready: Boolean
        get() = _state.value is StartupState.Ready

    fun start() {
        if (_state.value != StartupState.Loading) return
        scope.launch {
            prepareStorage()
            val bootstrapped =
                try {
                    bootstrapLegacyLock()
                    true
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    false
                }
            val lock = lockStore.lock.first()
            // 잇기에 실패하면 옛 잠금이 있었는지 모른다. 풀어 두는 대신 잠근 채 새 비밀번호를 정하게 한다(헌법 II).
            _state.value =
                StartupState.Ready(
                    settings = settingsStore.settings.first(),
                    lock = if (bootstrapped || lock.enabled) lock else lock.copy(enabled = true, pinMac = null, pinSalt = null)
                )
        }
    }
}

sealed interface StartupState {
    data object Loading : StartupState

    data class Ready(val settings: AppSettings, val lock: LockSettings) : StartupState
}
