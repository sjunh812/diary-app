package org.sjhstudio.diary.core.data.lock

import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.platform.PinKeyStore

/**
 * 확인값은 `HMAC-SHA256(기기 키, salt ‖ PIN의 ASCII 바이트)`를 Base64로 적은 것(research R11)
 * 비교는 상수 시간, 5번 연달아 틀리면 30초 기다림(FR-020)
 * 틀린 횟수와 기다림 끝 시각은 파일에 있어 앱을 다시 켜도 이어짐
 * 공간이 꽉 차 파일에 못 적으면 메모리에 들고 있다가 다음에 함께 적음
 * 앱이 떠 있는 동안은 횟수 제한이 그대로 걸림
 */
class DefaultLockRepository(
    private val store: LockStore,
    private val keys: PinKeyStore,
    private val newSalt: () -> ByteArray = ::randomSalt,
    // 기기 키 저장소는 디스크를 써 화면 스레드에서 부르지 않음. 틀린 횟수가 섞이지 않게 한 줄로
    private val keyWork: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : LockRepository {
    override val settings: Flow<LockSettings> = store.lock

    /** 파일에 적지 못한 틀린 횟수와 기다림 */
    private var unsaved: Attempts? = null

    override suspend fun needsNewPin(): Boolean = withContext(keyWork) {
        val lock = store.lock.first()
        lock.enabled && !canCheck(lock)
    }

    override suspend fun setPin(pin: Pin) = withContext(keyWork) {
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
            unsaved = null
        } finally {
            pin.clear()
        }
    }

    override suspend fun verify(pin: Pin, now: Instant): VerifyResult = withContext(keyWork) { judgeAndWrite(pin, now) { it } }

    override suspend fun disable(current: Pin, now: Instant): VerifyResult =
        withContext(keyWork) { judgeAndWrite(current, now) { it.turnedOff() } }

    override suspend fun turnOffWithoutPin(): Boolean = withContext(keyWork) {
        var done = false
        store.update { lock ->
            if (lock.enabled && keyIsGone(lock)) {
                done = true
                lock.turnedOff()
            } else {
                lock
            }
        }
        done
    }

    override suspend fun setBiometric(enabled: Boolean) = store.update { it.copy(biometricEnabled = enabled) }

    override suspend fun setRelockAfter(value: RelockAfter) = store.update { it.copy(relockAfter = value) }

    /**
     * 판정과 횟수 쓰기를 한 번의 파일 고침 안에서 함
     * 맞으면 [onOk]로 바꾼 값을 씀
     */
    private suspend fun judgeAndWrite(pin: Pin, now: Instant, onOk: (LockSettings) -> LockSettings): VerifyResult {
        var result: VerifyResult = VerifyResult.KeyMissing
        var attempts: Attempts? = null
        var turnedOff = false
        return try {
            store.update { saved ->
                val (next, judged) = judge(saved.with(unsaved), pin, now)
                val written = if (judged == VerifyResult.Ok) onOk(next) else next
                result = judged
                attempts = Attempts(next.failedAttempts, next.retryAfter)
                turnedOff = written.enabled != next.enabled
                written
            }
            unsaved = null
            result
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 판정은 끝났고 적지만 못함
            // 공간이 꽉 찬 기기에서도 비밀번호를 넣어 앱을 열 수 있어야 공간을 비울 수 있음
            unsaved = attempts ?: unsaved
            if (turnedOff) VerifyResult.NotSaved else result
        } finally {
            pin.clear()
        }
    }

    private fun judge(lock: LockSettings, pin: Pin, now: Instant): Pair<LockSettings, VerifyResult> {
        lock.retryAfter?.let { until ->
            // 기기 시각을 뒤로 돌려도 30초보다 오래 묶이지 않게
            val capped = minOf(until, now + LockRepository.WAIT)
            if (now < capped) return lock.copy(retryAfter = capped) to VerifyResult.Waiting(capped)
        }
        val expected = lock.pinMac?.let(::decode)
        val salt = lock.pinSalt?.let(::decode)
        val actual = if (expected != null && salt != null) macOf(lock.keyAlias, salt, pin) else null
        if (expected == null || actual == null) return lock to VerifyResult.KeyMissing
        if (constantTimeEquals(expected, actual)) return lock.copy(failedAttempts = 0, retryAfter = null) to VerifyResult.Ok
        val failed = lock.failedAttempts + 1
        if (failed < MAX_ATTEMPTS) return lock.copy(failedAttempts = failed, retryAfter = null) to VerifyResult.Wrong(MAX_ATTEMPTS - failed)
        // 기다림이 끝나면 다시 다섯 번을 줌
        val until = now + LockRepository.WAIT
        return lock.copy(failedAttempts = 0, retryAfter = until) to VerifyResult.Waiting(until)
    }

    /** 키 저장소를 읽지 못해도 대어 볼 수 없는 것은 같아 복구 길로 보냄 */
    private fun canCheck(lock: LockSettings): Boolean = lock.hasPin && runCatching { keys.hasKey(lock.keyAlias) }.getOrDefault(false)

    /**
     * 확인값이나 키가 정말 없는지
     * 잠금을 끄는 근거라서, 키 저장소를 잠깐 못 읽는 것은 없어진 것으로 보지 않음
     */
    private fun keyIsGone(lock: LockSettings): Boolean = !lock.hasPin || runCatching { !keys.hasKey(lock.keyAlias) }.getOrDefault(false)

    private fun macOf(alias: String, salt: ByteArray, pin: Pin): ByteArray? {
        val pinBytes = pin.asciiBytes()
        val data = salt + pinBytes
        return try {
            keys.mac(alias, data)
        } catch (_: Exception) {
            // 키를 쓸 수 없는 것은 비밀번호가 틀린 것이 아님
            null
        } finally {
            pinBytes.fill(0)
            data.fill(0)
        }
    }

    private fun LockSettings.with(attempts: Attempts?): LockSettings =
        attempts?.let { copy(failedAttempts = it.failed, retryAfter = it.retryAfter) } ?: this

    private fun LockSettings.turnedOff(): LockSettings =
        copy(enabled = false, pinMac = null, pinSalt = null, failedAttempts = 0, retryAfter = null)

    private fun decode(text: String): ByteArray? = runCatching { Base64.decode(text) }.getOrNull()

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private data class Attempts(val failed: Int, val retryAfter: Instant?)

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}

/**
 * 16바이트 임의값
 * `Uuid.random()`은 암호용 난수로 만듦
 */
@OptIn(ExperimentalUuidApi::class)
private fun randomSalt(): ByteArray = Uuid.random().toByteArray()
