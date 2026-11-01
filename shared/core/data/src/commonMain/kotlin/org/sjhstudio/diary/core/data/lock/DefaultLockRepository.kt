package org.sjhstudio.diary.core.data.lock

import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.platform.PinKeyStore

/**
 * 확인값은 `HMAC-SHA256(기기 키, salt ‖ PIN의 ASCII 바이트)`를 Base64로 적은 것이다(research R11).
 * 비교는 상수 시간으로 하고, 5번 연달아 틀리면 30초 기다린다(FR-020).
 * 틀린 횟수와 기다림 끝 시각은 파일에 있어서 앱을 다시 켜도 이어진다.
 */
class DefaultLockRepository(
    private val store: LockStore,
    private val keys: PinKeyStore,
    private val newSalt: () -> ByteArray = ::randomSalt
) : LockRepository {
    override val settings: Flow<LockSettings> = store.lock

    override suspend fun needsNewPin(): Boolean {
        val lock = store.lock.first()
        return lock.enabled && !canCheck(lock)
    }

    override suspend fun setPin(pin: Pin) {
        try {
            val alias = LockSettings.DEFAULT_KEY_ALIAS
            keys.ensureKey(alias)
            val salt = newSalt()
            val mac = checkNotNull(macOf(alias, salt, pin)) { "잠금 키를 쓸 수 없다" }
            store.update {
                it.copy(
                    enabled = true,
                    pinMac = Base64.encode(mac),
                    pinSalt = Base64.encode(salt),
                    keyAlias = alias,
                    failedAttempts = 0,
                    retryAfter = null
                )
            }
        } finally {
            pin.clear()
        }
    }

    override suspend fun verify(pin: Pin, now: Instant): VerifyResult = judgeAndWrite(pin, now) { it }

    override suspend fun disable(current: Pin, now: Instant): VerifyResult = judgeAndWrite(current, now) { it.turnedOff() }

    override suspend fun turnOffWithoutPin(): Boolean {
        var done = false
        store.update { lock ->
            if (lock.enabled && !canCheck(lock)) {
                done = true
                lock.turnedOff()
            } else {
                lock
            }
        }
        return done
    }

    override suspend fun setBiometric(enabled: Boolean) = store.update { it.copy(biometricEnabled = enabled) }

    override suspend fun setRelockAfter(value: RelockAfter) = store.update { it.copy(relockAfter = value) }

    /** 판정과 횟수 쓰기를 한 번의 파일 고침 안에서 한다. 맞으면 [onOk]로 바꾼 값을 쓴다. */
    private suspend fun judgeAndWrite(pin: Pin, now: Instant, onOk: (LockSettings) -> LockSettings): VerifyResult {
        var result: VerifyResult = VerifyResult.KeyMissing
        try {
            store.update { lock ->
                val (next, judged) = judge(lock, pin, now)
                result = judged
                if (judged == VerifyResult.Ok) onOk(next) else next
            }
        } finally {
            pin.clear()
        }
        return result
    }

    private fun judge(lock: LockSettings, pin: Pin, now: Instant): Pair<LockSettings, VerifyResult> {
        lock.retryAfter?.let { until ->
            // 기기 시각을 뒤로 돌려도 30초보다 오래 묶이지 않게 한다.
            val capped = minOf(until, now + WAIT)
            if (now < capped) return lock.copy(retryAfter = capped) to VerifyResult.Waiting(capped)
        }
        val expected = lock.pinMac?.let(::decode)
        val salt = lock.pinSalt?.let(::decode)
        val actual = if (expected != null && salt != null) macOf(lock.keyAlias, salt, pin) else null
        if (expected == null || actual == null) return lock to VerifyResult.KeyMissing
        if (constantTimeEquals(expected, actual)) return lock.copy(failedAttempts = 0, retryAfter = null) to VerifyResult.Ok
        val failed = lock.failedAttempts + 1
        if (failed < MAX_ATTEMPTS) return lock.copy(failedAttempts = failed, retryAfter = null) to VerifyResult.Wrong(MAX_ATTEMPTS - failed)
        // 기다림이 끝나면 다시 다섯 번을 준다.
        val until = now + WAIT
        return lock.copy(failedAttempts = 0, retryAfter = until) to VerifyResult.Waiting(until)
    }

    private fun canCheck(lock: LockSettings): Boolean = lock.hasPin && keys.hasKey(lock.keyAlias)

    private fun macOf(alias: String, salt: ByteArray, pin: Pin): ByteArray? {
        val pinBytes = pin.asciiBytes()
        val data = salt + pinBytes
        return try {
            keys.mac(alias, data)
        } finally {
            pinBytes.fill(0)
            data.fill(0)
        }
    }

    private fun LockSettings.turnedOff(): LockSettings =
        copy(enabled = false, pinMac = null, pinSalt = null, failedAttempts = 0, retryAfter = null)

    private fun decode(text: String): ByteArray? = runCatching { Base64.decode(text) }.getOrNull()

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        val WAIT = 30.seconds
    }
}

/** 16바이트 임의값. `Uuid.random()`은 암호용 난수로 만든다. */
@OptIn(ExperimentalUuidApi::class)
private fun randomSalt(): ByteArray = Uuid.random().toByteArray()
