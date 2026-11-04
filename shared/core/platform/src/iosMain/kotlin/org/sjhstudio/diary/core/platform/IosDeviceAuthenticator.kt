package org.sjhstudio.diary.core.platform

import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

/**
 * `LAContext`(research R12).
 * 열기는 `.deviceOwnerAuthenticationWithBiometrics`, 분실은 `.deviceOwnerAuthentication`(생체 또는 기기 암호)이다.
 * Face ID 문구는 `Info.plist`의 `NSFaceIDUsageDescription`이다.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosDeviceAuthenticator : DeviceAuthenticator {
    override fun biometricStatus(): BiometricStatus = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        when {
            LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error.ptr) -> BiometricStatus.Available
            error.value?.code == LAErrorBiometryNotEnrolled -> BiometricStatus.NoneEnrolled
            else -> BiometricStatus.Unavailable
        }
    }

    override fun isDeviceSecure(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)

    override suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult = evaluate(biometricOnly = true, prompt)

    override suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult = evaluate(biometricOnly = false, prompt)

    private suspend fun evaluate(biometricOnly: Boolean, prompt: AuthPrompt): AuthResult = suspendCancellableCoroutine { continuation ->
        val context = LAContext()
        context.localizedCancelTitle = prompt.cancel
        // 생체 인증만 받을 때는 "암호 입력" 버튼을 숨긴다. 숫자판이 그 일을 한다.
        if (biometricOnly) context.localizedFallbackTitle = ""
        val policy = if (biometricOnly) LAPolicyDeviceOwnerAuthenticationWithBiometrics else LAPolicyDeviceOwnerAuthentication
        context.evaluatePolicy(policy, localizedReason = prompt.reason ?: prompt.title) { success, error ->
            if (!continuation.isActive) return@evaluatePolicy
            val code = error?.code
            val result =
                when {
                    success -> AuthResult.Success
                    code == LAErrorUserCancel || code == LAErrorSystemCancel || code == LAErrorAppCancel -> AuthResult.Canceled
                    else -> AuthResult.Failed
                }
            continuation.resume(result)
        }
        continuation.invokeOnCancellation { context.invalidate() }
    }
}
