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
 * 앱이 처음 그리기 전에 끝내야 하는 일(contracts/app-shell.md 시작 순서 1)
 * 설정과 잠금 파일을 읽어 첫 화면의 테마와 잠금 여부를 정함. 일기 DB(Room)는 열지 않음
 * 목표는 300ms 안
 * 옛 앱의 잠금과 설정 잇기는 읽기 전에 끝냄(research R16). 잠금이 먼저
 */
class Startup(
    /** 파일 폴더 만들기(`AppFiles.ensureDirectories`) */
    private val prepareStorage: () -> Unit,
    private val settingsStore: SettingsStore,
    private val lockStore: LockStore,
    private val scope: CoroutineScope,
    /** 옛 앱(1.7.x) 잠금 잇기(`LegacyLockBootstrap`). 옛 앱이 없는 iOS는 아무것도 하지 않음 */
    private val bootstrapLegacyLock: suspend () -> Unit = {},
    /** 옛 앱 설정 잇기(`LegacySettingsBootstrap`). 실패해도 기본 설정으로 엶 */
    private val bootstrapLegacySettings: suspend () -> Unit = {},
    /** 고른 글꼴을 뒤에서 미리 읽기(`FontWarmup`). 시작 화면을 더 붙잡지 않고 나란히 돎 */
    private val warmFont: suspend (AppSettings) -> Unit = {},
    /** 첫 화면을 그린 뒤 해도 되는 일(매일 알림 다시 예약). 실패해도 앱은 엶 */
    private val afterReady: suspend () -> Unit = {}
) {
    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    /** 스플래시를 닫을 때를 정함(Android `setKeepOnScreenCondition`) */
    val ready: Boolean
        get() = _state.value is StartupState.Ready

    fun start() {
        if (_state.value != StartupState.Loading) return
        scope.launch {
            attempt { prepareStorage() }
            val bootstrapped = attempt { bootstrapLegacyLock() } != null
            attempt { bootstrapLegacySettings() }
            val lock = attempt { lockStore.lock.first() }
            val settings = attempt { settingsStore.settings.first() } ?: AppSettings()
            scope.launch { attempt { warmFont(settings) } }
            // 잇기나 읽기에 실패하면 잠금이 있었는지 모름
            // 풀어 두는 대신 잠근 채 새 비밀번호를 정하게 함(헌법 II)
            _state.value =
                StartupState.Ready(
                    settings = settings,
                    lock =
                        lock?.takeIf { bootstrapped || it.enabled }
                            ?: (lock ?: LockSettings()).copy(enabled = true, pinMac = null, pinSalt = null)
                )
            attempt { afterReady() }
        }
    }

    /** 시작을 멈추지 않음. 멈추면 스플래시가 사라지지 않아 앱을 쓸 수 없음 */
    private suspend fun <T> attempt(step: suspend () -> T): T? = try {
        step()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}

sealed interface StartupState {
    data object Loading : StartupState

    data class Ready(val settings: AppSettings, val lock: LockSettings) : StartupState
}
