package org.sjhstudio.diary.core.platform

/**
 * 생체 인증과 휴대폰 화면 잠금 확인(contracts/lock.md, research R12)
 * 열 때는 생체 인증만, 비밀번호를 잊었을 때는 휴대폰 PIN·패턴·암호도 받음
 */
interface DeviceAuthenticator {
    fun biometricStatus(): BiometricStatus

    /** 숫자판 생체 인증 버튼 아이콘. Android는 지문과 얼굴을 미리 알 수 없어 지문 */
    fun biometricKind(): BiometricKind = BiometricKind.Fingerprint

    /** 휴대폰에 화면 잠금(PIN·패턴·암호)이 있는지 */
    fun isDeviceSecure(): Boolean

    /** 열기용. 생체 인증만 받고 [AuthPrompt.cancel]은 숫자판으로 돌아가는 버튼 */
    suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult

    /** 비밀번호를 잊었을 때. 생체 인증이나 휴대폰 화면 잠금으로 본인 확인 */
    suspend fun authenticateDeviceOwner(prompt: AuthPrompt): AuthResult
}

/**
 * 생체 인증을 지금 쓸 수 있는지와 못 쓰는 까닭. 잠금 설정이 까닭마다 할 일을 알림
 * [NoneEnrolled]는 등록한 생체 정보나 기기 화면 잠금이 없는 경우, [NotAllowed]는 iOS에서 이 앱의 Face ID를 끈 경우
 * [LockedOut]은 여러 번 틀려 기기 암호를 기다리는 경우
 */
enum class BiometricStatus { Available, NoneEnrolled, NotAllowed, LockedOut, Unavailable }

enum class BiometricKind { Fingerprint, Face }

/** [LockedOut]은 여러 번 틀려 생체 인증이 잠김. iOS는 창 없이 바로 돌아와 앱이 알려야 함 */
enum class AuthResult { Success, Canceled, Failed, LockedOut }

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
