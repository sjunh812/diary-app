package org.sjhstudio.diary.feature.lock

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.platform.BiometricStatus
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.lock_settings_biometric_locked_out
import org.sjhstudio.diary.feature.lock.resources.lock_settings_biometric_none_enrolled
import org.sjhstudio.diary.feature.lock.resources.lock_settings_biometric_not_allowed
import org.sjhstudio.diary.feature.lock.resources.lock_settings_biometric_unavailable

/** 생체 인증을 못 쓰면 기기 탓으로 뭉뚱그리지 않고 까닭과 할 일을 말함 */
@OptIn(ExperimentalTestApi::class)
class LockSettingsUiTest {
    private val lockOn = LockSettings(enabled = true, pinMac = "bWFj", pinSalt = "c2FsdA==")

    private fun showsReason(status: BiometricStatus, reason: StringResource) = runComposeUiTest {
        var text = ""
        setContent {
            text = stringResource(reason)
            DiaryTheme(darkTheme = false) {
                LockSettingsScreen(lockOn, biometricStatus = status, deviceSecure = true, onBack = {
                }, onEnabledChange = {}, onChangePin = {}, onBiometricChange = {}, onRelockChange = {})
            }
        }
        onNodeWithText(text).assertExists()
    }

    @Test
    fun not_enrolled_says_to_set_it_up() = showsReason(BiometricStatus.NoneEnrolled, Res.string.lock_settings_biometric_none_enrolled)

    @Test
    fun turned_off_for_this_app_says_to_allow_it() = showsReason(BiometricStatus.NotAllowed, Res.string.lock_settings_biometric_not_allowed)

    @Test
    fun locked_after_tries_says_how_to_unlock() = showsReason(BiometricStatus.LockedOut, Res.string.lock_settings_biometric_locked_out)

    @Test
    fun no_hardware_says_it_cannot_be_used() = showsReason(BiometricStatus.Unavailable, Res.string.lock_settings_biometric_unavailable)
}
