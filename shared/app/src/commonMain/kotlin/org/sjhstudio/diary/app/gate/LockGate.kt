package org.sjhstudio.diary.app.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.StartupState
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.lock.LockState
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.platform.AppForeground
import org.sjhstudio.diary.core.platform.DeviceAuthenticator
import org.sjhstudio.diary.feature.lock.LockFlow
import org.sjhstudio.diary.feature.lock.rememberDeviceAuthenticator

/** 잠긴 동안 치워 둔 안쪽 화면의 저장 상태 이름. */
private const val APP_CONTENT = "app"

/**
 * 잠금 관문(contracts/app-shell.md 시작 순서, research R13).
 * 기록 이전 관문보다 바깥에 있다.
 * 잠긴 동안 안쪽 화면은 그리지 않고 저장 상태만 들고 있다가, 열면 보던 화면으로 돌아간다.
 * 기록 이전도 잠금을 연 뒤에 시작한다(FR-015).
 * [authenticator]는 시험이 시스템 인증 창 대신 대역을 넣는 자리다.
 */
@Composable
fun LockGate(authenticator: DeviceAuthenticator = rememberDeviceAuthenticator(), content: @Composable () -> Unit) {
    val controller = koinInject<LockController>()
    val startup = koinInject<Startup>()
    val startupState by startup.state.collectAsStateWithLifecycle()
    val initialLock = (startupState as? StartupState.Ready)?.lock ?: LockSettings()
    val lock by koinInject<LockRepository>().settings.collectAsStateWithLifecycle(initialValue = initialLock)
    val state by controller.state.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    val holder = rememberSaveableStateHolder()

    LaunchedEffect(startupState) { (startupState as? StartupState.Ready)?.let { controller.onColdStart(it.lock) } }
    DisposableEffect(controller) {
        val stop = AppForeground.observe(onBackground = controller::onBackground, onForeground = controller::onForeground)
        onDispose { stop() }
    }

    Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) {
        when (state) {
            // 시작 단계가 잠금을 읽기 전에는 아무것도 그리지 않는다(스플래시가 덮고 있다).
            LockState.Starting -> Unit

            LockState.Unlocked -> holder.SaveableStateProvider(APP_CONTENT) { content() }

            LockState.Locked, LockState.NeedsNewPin ->
                LockFlow(
                    needsNewPin = state == LockState.NeedsNewPin,
                    biometricEnabled = lock.biometricEnabled,
                    onUnlocked = controller::onUnlocked,
                    onKeyMissing = controller::onKeyMissing,
                    onLockTurnedOff = { message ->
                        controller.onUnlocked()
                        scope.launch { toast.show(message) }
                    },
                    withoutRelock = { block -> controller.withoutRelock(block) },
                    authenticator = authenticator
                )
        }
        // 잠금을 끈 알림은 잠금이 풀린 뒤 앱 틀 위에 뜬다.
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = toastBottomOverApp()))
    }
}
