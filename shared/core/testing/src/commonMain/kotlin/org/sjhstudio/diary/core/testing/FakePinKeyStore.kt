package org.sjhstudio.diary.core.testing

import org.sjhstudio.diary.core.platform.PinKeyStore

/**
 * 시험용 기기 키.
 * 진짜 HMAC 대신 키 번호와 입력을 섞은 값을 돌려준다.
 * [lose]로 키가 없어진 기기(다른 기기로 옮김, 키체인 초기화)를 흉내 낸다.
 */
class FakePinKeyStore : PinKeyStore {
    private val keys = mutableMapOf<String, Int>()
    private var made = 0

    /**
     * 마지막으로 [mac]에 들어온 입력.
     * 확인값 형식 시험에서 본다.
     */
    var lastInput: ByteArray? = null
        private set

    /** true면 키를 만들 때 예외가 난다(공간이 꽉 찬 기기의 Keystore). */
    var failMaking = false

    /** true면 키 저장소를 읽을 때 예외가 난다. 키가 없어진 것과 다르다. */
    var failLookup = false

    /** true면 키는 있는데 확인값을 만들 때 예외가 난다. */
    var failMac = false

    override fun ensureKey(alias: String) {
        check(!failMaking) { "키를 만들 수 없다" }
        if (alias !in keys) keys[alias] = ++made
    }

    override fun hasKey(alias: String): Boolean {
        check(!failLookup) { "키 저장소를 읽을 수 없다" }
        return alias in keys
    }

    override fun mac(alias: String, data: ByteArray): ByteArray? {
        check(!failMac) { "키를 쓸 수 없다" }
        val key = keys[alias] ?: return null
        lastInput = data.copyOf()
        return ByteArray(32) { i -> (data[i % data.size].toInt() * 31 + key * 7 + i).toByte() }
    }

    fun lose(alias: String) {
        keys.remove(alias)
    }
}
