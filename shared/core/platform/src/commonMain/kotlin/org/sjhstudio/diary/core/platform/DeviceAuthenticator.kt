package org.sjhstudio.diary.core.platform

/**
 * 생체 인증과 휴대폰 화면 잠금 확인(contracts/lock.md, research R12)
 * 열 때는 생체 인증만, 비밀번호를 잊었을 때는 휴대폰 PIN·패턴·암호도 받음
 */
interface DeviceAuthenticator {
    fun biometricStatus(): BiometricStatus

    /** 휴대폰에 화면 잠금(PIN·패턴·암호)이 있는지 */
    fun isDeviceSecure(): Boolean

    /** 열기용. 생체 인증만 받고 [AuthPrompt.cancel]은 숫자판으로 돌아가는 버튼 */
    suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult

    /** 비밀번호를 잊었을 때. 생체 인증이나 휴대폰 화면 잠금으로 본인 확인 */
    suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult
}

enum class BiometricStatus { Available, NoneEnrolled, Unavailable }

enum class AuthResult { Success, Canceled, Failed }

/**
 * 시스템 인증 창에 보일 글. 화면 문구라 부르는 쪽이 문자열 리소스에서 넘김
 * Android 창은 제목과 아래 버튼만 씀(토스 앱잠금과 같은 꼴)
 * [reason]은 iOS 이유 문장으로, 왜 인증하는지와 그 결과를 짧게 씀
 * 앱 이름은 시스템이 창에 따로 보여 둘 다 넣지 않음(Apple LocalAuthentication, Android 15 생체 인증 창)
 */
data class AuthPrompt(val title: String, val cancel: String, val reason: String? = null)

/** 인증 창을 띄울 수 없는 자리(미리보기, 시험) */
object NoDeviceAuthenticator : DeviceAuthenticator {
    override fun biometricStatus(): BiometricStatus = BiometricStatus.Unavailable

    override fun isDeviceSecure(): Boolean = false

    override suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult = AuthResult.Failed

    override suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult = AuthResult.Failed
}
