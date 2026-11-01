package org.sjhstudio.diary.core.platform

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

actual fun devicePinKeyStore(): PinKeyStore = AndroidPinKeyStore

/**
 * Android Keystore의 HMAC-SHA256 키(API 23부터).
 * 사용자 인증을 요구하지 않는다.
 * 잠금 화면에서 비밀번호를 대어 볼 때마다 생체 인증을 띄울 수는 없어서다.
 */
private object AndroidPinKeyStore : PinKeyStore {
    private const val PROVIDER = "AndroidKeyStore"

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun ensureKey(alias: String) {
        if (hasKey(alias)) return
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER).apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }
    }

    override fun hasKey(alias: String): Boolean = try {
        keyStore().containsAlias(alias)
    } catch (_: GeneralSecurityException) {
        false
    }

    override fun mac(alias: String, data: ByteArray): ByteArray? = try {
        val key = keyStore().getKey(alias, null) as? SecretKey
        key?.let { Mac.getInstance("HmacSHA256").apply { init(it) }.doFinal(data) }
    } catch (_: GeneralSecurityException) {
        null
    }
}
