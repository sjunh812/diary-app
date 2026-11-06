package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/**
 * 잠금 설정(`lock.preferences_pb`).
 * 비밀번호 평문은 어디에도 두지 않고, 기기에 묶인 키로 만든 확인값만 둔다(research R11).
 */
data class LockSettings(
    val enabled: Boolean = false,
    /** Base64 `HMAC-SHA256(key, salt ‖ PIN)`. */
    val pinMac: String? = null,
    /** Base64 16바이트 임의값. */
    val pinSalt: String? = null,
    /** Keystore·키체인 키 이름. */
    val keyAlias: String = DEFAULT_KEY_ALIAS,
    val biometricEnabled: Boolean = false,
    val relockAfter: RelockAfter = RelockAfter.DEFAULT,
    /**
     * 연속으로 틀린 횟수.
     * 맞으면 0.
     */
    val failedAttempts: Int = 0,
    /**
     * 다시 입력할 수 있는 시각.
     * null이면 기다리지 않는다.
     */
    val retryAfter: Instant? = null
) {
    /** 잠금은 켜졌는데 확인값이 없으면 폰 화면 잠금 확인 뒤 새 비밀번호를 정한다. */
    val hasPin: Boolean
        get() = pinMac != null && pinSalt != null

    companion object {
        const val DEFAULT_KEY_ALIAS = "diary.pin.v1"
    }
}
