package org.sjhstudio.diary.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

actual fun devicePinKeyStore(): PinKeyStore = IosPinKeyStore

/** CommonCrypto HMAC-SHA256. 키체인과 떼어 두어 시뮬레이터 시험에서 표준 시험값으로 확인 */
@OptIn(ExperimentalForeignApi::class)
internal fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    require(key.isNotEmpty() && data.isNotEmpty())
    val out = ByteArray(CC_SHA256_DIGEST_LENGTH)
    key.usePinned { k ->
        data.usePinned { d ->
            out.usePinned { o ->
                CCHmac(kCCHmacAlgSHA256, k.addressOf(0), key.size.convert(), d.addressOf(0), data.size.convert(), o.addressOf(0))
            }
        }
    }
    return out
}

/**
 * 키체인에 둔 256비트 임의 키와 CommonCrypto HMAC-SHA256.
 * `AfterFirstUnlockThisDeviceOnly`라 iCloud 키체인이나 다른 기기 백업으로 옮겨 가지 않음
 */
@OptIn(ExperimentalForeignApi::class)
private object IosPinKeyStore : PinKeyStore {
    private const val SERVICE = "org.sjhstudio.diary.pin"
    private const val KEY_BYTES = 32

    override fun ensureKey(alias: String) {
        if (hasKey(alias)) return
        val key = ByteArray(KEY_BYTES)
        val made = key.usePinned { SecRandomCopyBytes(kSecRandomDefault, KEY_BYTES.convert(), it.addressOf(0)) }
        if (made != 0) return
        val value = CFBridgingRetain(key.toNSData())
        key.fill(0)
        try {
            withQuery(alias, kSecValueData to value, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly) {
                SecItemAdd(it, null)
            }
        } finally {
            CFRelease(value)
        }
    }

    override fun hasKey(alias: String): Boolean {
        val status = withQuery(alias, kSecMatchLimit to kSecMatchLimitOne) { SecItemCopyMatching(it, null) }
        check(status == errSecSuccess || status == errSecItemNotFound) { "키체인을 읽지 못했다" }
        return status == errSecSuccess
    }

    override fun mac(alias: String, data: ByteArray): ByteArray? {
        if (data.isEmpty()) return null
        val key = readKey(alias) ?: return null
        return try {
            hmacSha256(key, data)
        } finally {
            key.fill(0)
        }
    }

    private fun readKey(alias: String): ByteArray? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status =
            withQuery(alias, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
                SecItemCopyMatching(it, result.ptr)
            }
        if (status != errSecSuccess) return@memScoped null
        (CFBridgingRelease(result.value) as? NSData)?.toByteArray()?.takeIf { it.size == KEY_BYTES }
    }

    private fun <T> withQuery(alias: String, vararg extra: Pair<CFStringRef?, CFTypeRef?>, block: (CFMutableDictionaryRef?) -> T): T {
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(alias)
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service)
            CFDictionaryAddValue(query, kSecAttrAccount, account)
            extra.forEach { (name, value) -> CFDictionaryAddValue(query, name, value) }
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(service)
            CFRelease(account)
        }
    }

    private fun ByteArray.toNSData(): NSData = usePinned { NSData.dataWithBytes(it.addressOf(0), size.convert()) }

    private fun NSData.toByteArray(): ByteArray {
        val out = ByteArray(length.toInt())
        if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
        return out
    }
}
