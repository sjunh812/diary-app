package org.sjhstudio.diary.core.platform

/**
 * 잠금 비밀번호 확인값을 만드는 기기 키(contracts/lock.md, research R11).
 * 키는 기기 밖으로 나가지 않는다.
 * 확인값 파일을 가져가도 키가 없으면 4자리를 하나씩 대어 볼 수 없다.
 */
interface PinKeyStore {
    /**
     * 키가 없으면 새로 만든다.
     * 만들지 못하면 예외가 난다.
     */
    fun ensureKey(alias: String)

    /**
     * 키가 있는가.
     * 키 저장소를 읽지 못하면 없다고 하지 않고 예외를 낸다.
     * 없다는 말은 잠금을 끄는 근거가 되어서 잠깐의 오류와 구별한다.
     */
    fun hasKey(alias: String): Boolean

    /**
     * `HMAC-SHA256(key, data)`.
     * 키가 없거나 쓸 수 없으면 null이다.
     */
    fun mac(alias: String, data: ByteArray): ByteArray?
}

/** Android는 Keystore, iOS는 키체인에 둔 키를 쓴다. */
expect fun devicePinKeyStore(): PinKeyStore
