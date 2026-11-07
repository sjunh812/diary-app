package org.sjhstudio.diary.core.platform

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
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

    override fun hasKey(alias: String): Boolean = keyStore().containsAlias(alias)

    override fun mac(alias: String, data: ByteArray): ByteArray? = try {
        val key = keyStore().getKey(alias, null) as? SecretKey
        key?.let { Mac.getInstance("HmacSHA256").apply { init(it) }.doFinal(data) }
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: ProviderException) {
        // 공간이 꽉 찬 기기의 Keystore는 이 예외를 던진다.
        null
    }
}
