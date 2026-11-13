package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.LocalTopBarBackHidden
import org.sjhstudio.diary.core.platform.BiometricStatus
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.lock_confirm_title

private enum class SettingsStep { Main, ConfirmToDisable, ConfirmToChange, NewPin }

/**
 * 잠금 설정과 그 안의 비밀번호 화면(contracts/lock.md)
 * 켜기: 새 비밀번호 두 번
 * 끄기: 지금 비밀번호
 * 바꾸기: 지금 비밀번호 → 새 비밀번호 두 번
 * 지금 비밀번호를 잊어버렸으면 앱 잠금 화면의 "비밀번호를 잊어버렸어요"로 다시 정함
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockSettingsRoute(onBack: () -> Unit) {
    val authenticator = rememberDeviceAuthenticator()
    val viewModel = koinViewModel<LockSettingsViewModel>()
    // 잠금 관문의 화면 상태와 섞이지 않게 따로 둠
    val confirm = koinViewModel<LockViewModel>(key = "lock-settings-confirm")
    val setup = koinViewModel<PinSetupViewModel>(key = "lock-settings-setup")
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val confirmState by confirm.state.collectAsStateWithLifecycle()
    val setupState by setup.state.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(SettingsStep.Main) }
    val biometricAvailable = remember(authenticator) { authenticator.biometricStatus() == BiometricStatus.Available }
    // 설정 앱에서 화면 잠금을 바꾸고 돌아올 수 있어 돌아올 때마다 다시 봄
    var deviceSecure by remember(authenticator) { mutableStateOf(authenticator.isDeviceSecure()) }
    LifecycleResumeEffect(authenticator) {
        deviceSecure = authenticator.isDeviceSecure()
        onPauseOrDispose {}
    }

    LaunchedEffect(confirm) {
        confirm.events.collect { event ->
            if (event != LockEvent.Unlocked) return@collect
            if (step == SettingsStep.ConfirmToChange) {
                setup.reset()
                step = SettingsStep.NewPin
            } else {
                viewModel.onLockChanged(enabled = false)
                step = SettingsStep.Main
            }
        }
    }
    LaunchedEffect(setup) {
        setup.done.collect {
            if (!settings.enabled) viewModel.onLockChanged(enabled = true)
            step = SettingsStep.Main
        }
    }

    // 비밀번호 확인·새 비밀번호 단계는 이 화면 안의 이동이라 두 칸 오른쪽에서도 뒤로 단추를 둠
    val hideBack = LocalTopBarBackHidden.current && step == SettingsStep.Main
    CompositionLocalProvider(LocalTopBarBackHidden provides hideBack) {
        when (step) {
            SettingsStep.Main ->
                LockSettingsScreen(
                    settings = settings,
                    biometricAvailable = biometricAvailable,
                    deviceSecure = deviceSecure,
                    onBack = onBack,
                    onEnabledChange = { on ->
                        if (on) {
                            setup.reset()
                            step = SettingsStep.NewPin
                        } else {
                            confirm.start(PinCheck.Disable)
                            step = SettingsStep.ConfirmToDisable
                        }
                    },
                    onChangePin = {
                        confirm.start(PinCheck.Verify)
                        step = SettingsStep.ConfirmToChange
                    },
                    onBiometricChange = viewModel::setBiometric,
                    onRelockChange = viewModel::setRelockAfter
                )

            SettingsStep.ConfirmToDisable, SettingsStep.ConfirmToChange -> {
                val back = { step = SettingsStep.Main }
                BackHandler(onBack = back)
                LockScreen(
                    state = confirmState,
                    showBiometric = false,
                    onDigit = confirm::onDigit,
                    onDelete = confirm::onDelete,
                    onBiometric = {},
                    onForgot = null,
                    title = stringResource(Res.string.lock_confirm_title),
                    onBack = back
                )
            }

            SettingsStep.NewPin -> {
                val back = { if (!setup.back()) step = SettingsStep.Main }
                BackHandler(onBack = back)
                PinSetupScreen(setupState, onDigit = setup::onDigit, onDelete = setup::onDelete, onBack = back, deviceSecure = deviceSecure)
            }
        }
    }
}
