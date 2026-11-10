package org.sjhstudio.diary.feature.lock.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.feature.lock.LockMessage
import org.sjhstudio.diary.feature.lock.LockScreen
import org.sjhstudio.diary.feature.lock.LockSettingsScreen
import org.sjhstudio.diary.feature.lock.LockUiState
import org.sjhstudio.diary.feature.lock.PinSetupScreen
import org.sjhstudio.diary.feature.lock.PinSetupUiState

// 잠금 화면(L1)과 비밀번호 정하기(L2) 프리뷰
// 상태는 Figma "잠금 상태"와 같음
// 비밀번호를 잊어버렸을 때(L3)는 바텀 시트라 프리뷰 창에 그려지지 않음
// 개발 빌드 화면 모음(lock_forgot)으로 봄

@Composable
private fun Lock(state: LockUiState, dark: Boolean = false, biometric: Boolean = true) = DiaryTheme(darkTheme = dark) {
    LockScreen(state, showBiometric = biometric, onDigit = {}, onDelete = {}, onBiometric = {}, onForgot = {})
}

@Preview(name = "L1 잠금 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LockLight() = Lock(LockUiState())

@Preview(name = "L1 잠금 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LockDark() = Lock(LockUiState(entered = 2), dark = true)

@Preview(name = "L1 비밀번호가 맞지 않아요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LockWrong() = Lock(LockUiState(message = LockMessage.Wrong(3), wrongCount = 1))

@Preview(name = "L1 잠시 기다려 주세요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LockWaiting() = Lock(LockUiState(message = LockMessage.Waiting(27)))

@Preview(name = "L1 태블릿", locale = "ko", widthDp = 1194, heightDp = 834)
@Composable
private fun LockTablet() = Lock(LockUiState(), biometric = false)

@Composable
private fun Setup(state: PinSetupUiState, dark: Boolean = false) = DiaryTheme(darkTheme = dark) {
    PinSetupScreen(state, onDigit = {}, onDelete = {}, onBack = {})
}

@Preview(name = "L2 새 비밀번호 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SetupLight() = Setup(PinSetupUiState(entered = 2))

@Preview(name = "L2 새 비밀번호 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SetupDark() = Setup(PinSetupUiState(entered = 2), dark = true)

@Preview(name = "L2 한 번 더", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SetupAgain() = Setup(PinSetupUiState(entered = 1, again = true))

@Preview(name = "L2 서로 달라요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SetupMismatch() = Setup(PinSetupUiState(mismatch = true))

@Composable
private fun Settings(settings: LockSettings, dark: Boolean = false, biometricAvailable: Boolean = true) = DiaryTheme(darkTheme = dark) {
    LockSettingsScreen(
        settings,
        biometricAvailable = biometricAvailable,
        onBack = {},
        onEnabledChange = {},
        onChangePin = {},
        onBiometricChange = {},
        onRelockChange = {}
    )
}

private val lockOn =
    LockSettings(enabled = true, pinMac = "bWFj", pinSalt = "c2FsdA==", biometricEnabled = true, relockAfter = RelockAfter.ONE_MINUTE)

@Preview(name = "L4 잠금 설정 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsLight() = Settings(lockOn)

@Preview(name = "L4 잠금 설정 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsDark() = Settings(lockOn, dark = true)

@Preview(name = "L4 생체 인증을 쓸 수 없어요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsNoBiometric() = Settings(lockOn, biometricAvailable = false)

@Preview(name = "L4 잠금 꺼짐", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsOff() = Settings(LockSettings())

@Preview(name = "새 비밀번호 · 저장 실패", locale = "ko")
@Composable
private fun SetupSaveFailed() = DiaryTheme(darkTheme = false) { PinSetupScreen(PinSetupUiState(saveFailed = true), {}, {}, {}) }

@Preview(name = "잠금 · 확인할 수 없음", locale = "ko")
@Composable
private fun KeyUnavailable() = DiaryTheme(darkTheme = false) {
    LockScreen(LockUiState(message = LockMessage.KeyUnavailable), showBiometric = false, {}, {}, {}, onForgot = {})
}

@Preview(name = "잠금 끄기 · 저장 실패", locale = "ko")
@Composable
private fun TurnOffFailed() = DiaryTheme(darkTheme = true) {
    LockScreen(LockUiState(message = LockMessage.NotSaved), showBiometric = false, {}, {}, {}, onForgot = null)
}
