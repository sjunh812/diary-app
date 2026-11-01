package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.resources.Res as DsRes
import org.sjhstudio.diary.core.designsystem.resources.ds_close
import org.sjhstudio.diary.core.platform.AuthPrompt
import org.sjhstudio.diary.core.platform.AuthResult
import org.sjhstudio.diary.core.platform.BiometricStatus
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.forgot_prompt_title
import org.sjhstudio.diary.feature.lock.resources.lock_biometric_prompt_cancel
import org.sjhstudio.diary.feature.lock.resources.lock_biometric_prompt_title
import org.sjhstudio.diary.feature.lock.resources.lock_turned_off

private enum class LockStep { Enter, NewPin }

/**
 * 잠금 관문 안쪽 흐름(contracts/lock.md).
 * 잠금 화면 → "비밀번호를 잊어버렸어요" → 폰 화면 잠금 확인 → 새 비밀번호 정하기.
 * [needsNewPin]이면(키 없음) 숫자판 대신 바로 폰 화면 잠금 확인으로 가고, 폰에 화면 잠금이 없으면 잠금을 끄고 알린다.
 * [withoutRelock]은 폰 잠금 확인으로 앱이 잠깐 가려지는 동안 다시 잠그지 않게 감싼다.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockFlow(
    needsNewPin: Boolean,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onKeyMissing: () -> Unit,
    onLockTurnedOff: (message: String) -> Unit,
    withoutRelock: suspend (suspend () -> AuthResult) -> AuthResult
) {
    val authenticator = rememberDeviceAuthenticator()
    val lock = koinViewModel<LockViewModel>()
    val setup = koinViewModel<PinSetupViewModel>()
    val lockState by lock.state.collectAsStateWithLifecycle()
    val setupState by setup.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(LockStep.Enter) }
    var asking by rememberSaveable { mutableStateOf(false) }
    val showBiometric = remember(authenticator, biometricEnabled, needsNewPin) {
        biometricEnabled && !needsNewPin && authenticator.biometricStatus() == BiometricStatus.Available
    }
    val biometricPrompt =
        AuthPrompt(stringResource(Res.string.lock_biometric_prompt_title), stringResource(Res.string.lock_biometric_prompt_cancel))
    val ownerPrompt = AuthPrompt(stringResource(Res.string.forgot_prompt_title), stringResource(DsRes.string.ds_close))
    val turnedOff = stringResource(Res.string.lock_turned_off)

    LaunchedEffect(lock) {
        lock.events.collect { event ->
            when (event) {
                LockEvent.Unlocked -> onUnlocked()
                LockEvent.KeyMissing -> onKeyMissing()
            }
        }
    }
    LaunchedEffect(setup) { setup.done.collect { onUnlocked() } }
    // 열자마자 생체 인증을 띄운다. 취소하거나 실패하면 숫자판이 그대로 남는다.
    LaunchedEffect(showBiometric) {
        if (showBiometric && authenticator.authenticateBiometric(biometricPrompt) == AuthResult.Success) onUnlocked()
    }
    LaunchedEffect(needsNewPin) {
        if (!needsNewPin) return@LaunchedEffect
        if (authenticator.isDeviceSecure()) {
            asking = true
        } else {
            lock.turnOffWithoutPin()
            onLockTurnedOff(turnedOff)
        }
    }

    when (step) {
        LockStep.Enter ->
            LockScreen(
                state = lockState,
                showBiometric = showBiometric,
                onDigit = lock::onDigit,
                onDelete = lock::onDelete,
                onBiometric = {
                    scope.launch { if (authenticator.authenticateBiometric(biometricPrompt) == AuthResult.Success) onUnlocked() }
                },
                onForgot = { asking = true }
            )

        LockStep.NewPin -> {
            val back = { if (!setup.back()) step = LockStep.Enter }
            BackHandler(onBack = back)
            PinSetupScreen(setupState, onDigit = setup::onDigit, onDelete = setup::onDelete, onBack = back)
        }
    }
    if (asking) {
        ForgotPinSheet(
            deviceSecure = remember(authenticator) { authenticator.isDeviceSecure() },
            onDismiss = { asking = false },
            onConfirm = {
                asking = false
                scope.launch {
                    if (withoutRelock { authenticator.authenticateDeviceOwner(ownerPrompt) } == AuthResult.Success) {
                        setup.reset()
                        step = LockStep.NewPin
                    }
                }
            }
        )
    }
}
