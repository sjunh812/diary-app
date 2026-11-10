package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import org.sjhstudio.diary.core.platform.DeviceAuthenticator
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.forgot_prompt_title
import org.sjhstudio.diary.feature.lock.resources.lock_biometric_prompt_cancel
import org.sjhstudio.diary.feature.lock.resources.lock_biometric_prompt_reason
import org.sjhstudio.diary.feature.lock.resources.lock_biometric_prompt_title
import org.sjhstudio.diary.feature.lock.resources.lock_turned_off

private enum class LockStep { Enter, NewPin }

/**
 * 잠금 관문 안쪽 흐름(contracts/lock.md)
 * 잠금 화면 → "비밀번호를 잊어버렸어요" → 폰 화면 잠금 확인 → 새 비밀번호 정하기
 * [needsNewPin](키 없음)이면 숫자판 없이 바로 폰 화면 잠금 확인
 * 폰에 화면 잠금이 없으면 잠금을 끄고 알림
 * 잠금을 끄지 못하면 앱을 열지 않고 [onKeepLock]로 잠금 화면에 남음
 * [withoutRelock]: 폰 잠금 확인으로 앱이 잠깐 가려지는 동안 다시 잠그지 않게 감쌈
 * [authenticator]: 시험에서 시스템 인증 창 대신 대역을 넣는 자리
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockFlow(
    needsNewPin: Boolean,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onKeyMissing: () -> Unit,
    onKeepLock: () -> Unit,
    onLockTurnedOff: (message: String) -> Unit,
    withoutRelock: suspend (suspend () -> AuthResult) -> AuthResult,
    authenticator: DeviceAuthenticator = rememberDeviceAuthenticator()
) {
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
        AuthPrompt(
            stringResource(Res.string.lock_biometric_prompt_title),
            stringResource(Res.string.lock_biometric_prompt_cancel),
            reason = stringResource(Res.string.lock_biometric_prompt_reason)
        )
    val ownerPrompt = AuthPrompt(stringResource(Res.string.forgot_prompt_title), stringResource(DsRes.string.ds_close))
    val turnedOff = stringResource(Res.string.lock_turned_off)
    val keepLocked by rememberUpdatedState(onKeepLock)

    LaunchedEffect(lock) {
        lock.events.collect { event ->
            when (event) {
                LockEvent.Unlocked -> onUnlocked()
                LockEvent.KeyMissing -> onKeyMissing()
            }
        }
    }
    LaunchedEffect(setup) { setup.done.collect { onUnlocked() } }
    // 열자마자 생체 인증 띄움
    // 취소·실패하면 숫자판이 그대로 남음
    // 실패 까닭(지문 불일치, 잠시 잠김)은 시스템 창이 직접 알림
    val unlockWithBiometric: suspend () -> Unit = {
        val result = authenticator.authenticateBiometric(biometricPrompt)
        if (result != AuthResult.Failed) lock.logBiometric(opened = result == AuthResult.Success)
        if (result == AuthResult.Success) {
            // 누르다 만 숫자가 다음 잠금에 남으면 틀린 번호로 셈
            lock.start(PinCheck.Verify)
            onUnlocked()
        }
    }
    LaunchedEffect(showBiometric) { if (showBiometric) unlockWithBiometric() }
    LaunchedEffect(needsNewPin) {
        if (!needsNewPin) return@LaunchedEffect
        if (authenticator.isDeviceSecure()) {
            asking = true
        } else if (lock.turnOffWithoutPin()) {
            onLockTurnedOff(turnedOff)
        } else {
            keepLocked()
        }
    }

    when (step) {
        LockStep.Enter ->
            LockScreen(
                state = lockState,
                showBiometric = showBiometric,
                onDigit = lock::onDigit,
                onDelete = lock::onDelete,
                onBiometric = { scope.launch { unlockWithBiometric() } },
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
