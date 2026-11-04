package org.sjhstudio.diary.core.platform

/**
 * 생체 인증과 폰 화면 잠금 확인(contracts/lock.md, research R12).
 * 열 때는 생체 인증만 쓰고, 비밀번호를 잊어버렸을 때는 폰 PIN·패턴·암호도 받는다.
 */
interface DeviceAuthenticator {
    fun biometricStatus(): BiometricStatus

    /** 폰에 화면 잠금(PIN·패턴·암호)이 있는가. */
    fun isDeviceSecure(): Boolean

    /** 열기용. 생체 인증만 받고 [AuthPrompt.cancel]은 숫자판으로 돌아가는 버튼이다. */
    suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult

    /** 비밀번호를 잊어버렸을 때. 생체 인증이나 폰 화면 잠금으로 본인을 확인한다. */
    suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult
}

enum class BiometricStatus { Available, NoneEnrolled, Unavailable }

enum class AuthResult { Success, Canceled, Failed }

/**
 * 시스템 인증 창에 보일 글. 화면 문구라 부르는 쪽이 문자열 리소스에서 넘긴다.
 * [subtitle]은 왜 인증하는지 한 줄로 쓴다(Android 부제, iOS는 이유 문장).
 * 앱 이름은 넣지 않는다. 창에 시스템이 따로 보인다(Apple LocalAuthentication, Android 15 생체 인증 창).
 */
data class AuthPrompt(val title: String, val cancel: String, val subtitle: String? = null)

/** 인증 창을 띄울 수 없는 자리(미리보기, 시험). */
object NoDeviceAuthenticator : DeviceAuthenticator {
    override fun biometricStatus(): BiometricStatus = BiometricStatus.Unavailable

    override fun isDeviceSecure(): Boolean = false

    override suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult = AuthResult.Failed

    override suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult = AuthResult.Failed
}
