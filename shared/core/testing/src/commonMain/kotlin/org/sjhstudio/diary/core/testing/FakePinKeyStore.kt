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

    /** 마지막으로 [mac]에 들어온 입력. 확인값 형식 시험에서 본다. */
    var lastInput: ByteArray? = null
        private set

    override fun ensureKey(alias: String) {
        if (alias !in keys) keys[alias] = ++made
    }

    override fun hasKey(alias: String): Boolean = alias in keys

    override fun mac(alias: String, data: ByteArray): ByteArray? {
        val key = keys[alias] ?: return null
        lastInput = data.copyOf()
        return ByteArray(32) { i -> (data[i % data.size].toInt() * 31 + key * 7 + i).toByte() }
    }

    fun lose(alias: String) {
        keys.remove(alias)
    }
}
