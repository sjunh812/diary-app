package org.sjhstudio.diary.core.data.lock

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 잠금 비밀번호 확인(스펙 001 T052, contracts/lock.md).
class LockRepositoryTest {
    private val salt = ByteArray(16) { it.toByte() }
    private val dataStore = InMemoryDataStore<Preferences>(emptyPreferences())
    private val keys = FakePinKeyStore()
    private val repository = DefaultLockRepository(LockStore(dataStore), keys, newSalt = { salt.copyOf() })
    private val now = Instant.parse("2026-11-02T09:00:00Z")

    private fun pin(text: String) = checkNotNull(Pin.of(text))

    @Test
    fun pin_accepts_only_four_ascii_digits() {
        assertNull(Pin.of("123"))
        assertNull(Pin.of("12345"))
        assertNull(Pin.of("12a4"))
        assertNull(Pin.of("１２３４"))
        assertEquals("Pin(****)", pin("0000").toString())
    }

    /** 확인값은 HMAC(키, salt ‖ PIN의 ASCII 바이트)를 Base64로 적은 것이고 PIN 평문은 어디에도 없다. */
    @Test
    fun mac_format_is_salt_then_ascii_pin_in_base64() = runTest {
        repository.setPin(pin("2580"))

        val lock = repository.settings.first()
        assertTrue(lock.enabled)
        assertEquals(Base64.encode(salt), lock.pinSalt)
        assertContentEquals(salt + "2580".encodeToByteArray(), keys.lastInput)
        assertEquals(Base64.encode(checkNotNull(keys.mac(LockSettings.DEFAULT_KEY_ALIAS, salt + "2580".encodeToByteArray()))), lock.pinMac)
        assertFalse("2580" in dataStore.data.first().toString())
    }

    @Test
    fun right_and_wrong() = runTest {
        repository.setPin(pin("2580"))

        assertEquals(VerifyResult.Wrong(remainingBeforeWait = 4), repository.verify(pin("1111"), now))
        assertEquals(VerifyResult.Ok, repository.verify(pin("2580"), now))
        assertEquals(0, repository.settings.first().failedAttempts)
    }

    /** 5번째에 30초 기다림. 저장해 두어서 앱을 다시 켜도(저장소를 새로 만들어도) 이어진다. */
    @Test
    fun fifth_wrong_waits_thirty_seconds_and_survives_restart() = runTest {
        repository.setPin(pin("2580"))
        repeat(4) { assertEquals(VerifyResult.Wrong(4 - it), repository.verify(pin("0000"), now)) }

        val until = now + 30.seconds
        assertEquals(VerifyResult.Waiting(until), repository.verify(pin("0000"), now))

        val restarted = DefaultLockRepository(LockStore(dataStore), keys)
        assertEquals(VerifyResult.Waiting(until), restarted.verify(pin("2580"), now + 10.seconds))
        assertEquals(VerifyResult.Ok, restarted.verify(pin("2580"), until))
    }

    /** 기기 시각을 뒤로 돌려도 30초보다 오래 묶이지 않는다. */
    @Test
    fun clock_moved_back_does_not_extend_the_wait() = runTest {
        repository.setPin(pin("2580"))
        repeat(5) { repository.verify(pin("0000"), now) }

        val earlier = now - 600.seconds
        assertEquals(VerifyResult.Waiting(earlier + 30.seconds), repository.verify(pin("2580"), earlier))
    }

    /** 다른 기기로 옮겨 키가 없으면 대어 볼 수 없다. 폰 화면 잠금 흐름으로 간다. */
    @Test
    fun missing_key_or_mac_is_key_missing() = runTest {
        repository.setPin(pin("2580"))
        keys.lose(LockSettings.DEFAULT_KEY_ALIAS)

        assertTrue(repository.needsNewPin())
        assertEquals(VerifyResult.KeyMissing, repository.verify(pin("2580"), now))
        assertTrue(repository.settings.first().enabled)
    }

    @Test
    fun disable_only_with_the_right_pin() = runTest {
        repository.setPin(pin("2580"))

        assertEquals(VerifyResult.Wrong(4), repository.disable(pin("1111"), now))
        assertTrue(repository.settings.first().enabled)

        assertEquals(VerifyResult.Ok, repository.disable(pin("2580"), now))
        val lock = repository.settings.first()
        assertFalse(lock.enabled)
        assertFalse(lock.hasPin)
    }

    /** 확인할 수 있는 비밀번호가 있으면 비밀번호 없이 끌 수 없다. */
    @Test
    fun turn_off_without_pin_only_when_it_cannot_be_checked() = runTest {
        repository.setPin(pin("2580"))
        assertFalse(repository.turnOffWithoutPin())
        assertTrue(repository.settings.first().enabled)

        keys.lose(LockSettings.DEFAULT_KEY_ALIAS)
        assertTrue(repository.turnOffWithoutPin())
        assertFalse(repository.settings.first().enabled)
    }

    @Test
    fun pin_is_cleared_after_use() = runTest {
        val digits = pin("2580")
        repository.setPin(digits)
        assertContentEquals(ByteArray(4), digits.asciiBytes())
    }
}
