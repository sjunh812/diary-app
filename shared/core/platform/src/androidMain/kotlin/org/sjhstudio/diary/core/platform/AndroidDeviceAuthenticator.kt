package org.sjhstudio.diary.core.platform

import android.app.KeyguardManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * `androidx.biometric` 1.1.0(research R12).
 * 열기는 `BIOMETRIC_WEAK`만, 분실은 `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`이다.
 * `DEVICE_CREDENTIAL` 단독은 API 30 미만에서 안 되고, `BIOMETRIC_WEAK`와 묶으면 API 24부터 된다.
 */
class AndroidDeviceAuthenticator(private val activity: FragmentActivity) : DeviceAuthenticator {
    override fun biometricStatus(): BiometricStatus = when (BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
        else -> BiometricStatus.Unavailable
    }

    override fun isDeviceSecure(): Boolean = activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    override suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult = authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(prompt.title)
            .setNegativeButtonText(prompt.cancel)
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
    )

    // 폰 화면 잠금을 함께 받을 때는 취소 버튼 글을 넣을 수 없다(시스템이 정한다).
    override suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult = authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(prompt.title)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
    )

    private suspend fun authenticate(info: BiometricPrompt.PromptInfo): AuthResult = suspendCancellableCoroutine { continuation ->
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(AuthResult.Success)
                }

                // 한 번 틀리면 창은 그대로 남는다.
                // 창이 닫힐 때만 끝낸다.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!continuation.isActive) return
                    val canceled =
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                    continuation.resume(if (canceled) AuthResult.Canceled else AuthResult.Failed)
                }
            }
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        continuation.invokeOnCancellation { biometricPrompt.cancelAuthentication() }
        biometricPrompt.authenticate(info)
    }
}
