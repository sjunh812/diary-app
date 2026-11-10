package org.sjhstudio.diary

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.sjhstudio.diary.core.platform.devicePinKeyStore

/**
 * 잠금 확인값을 만드는 Android Keystore 키(스펙 001 T056, research R11).
 * 시험용 별칭을 쓰고 끝나면 지움.
 * 앱의 진짜 키(`diary.pin.v1`)는 건드리지 않음.
 */
@RunWith(AndroidJUnit4::class)
class PinKeyStoreTest {
    private val keys = devicePinKeyStore()
    private val alias = "diary.pin.test"

    @After
    fun tearDown() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun key_is_made_once_and_mac_is_stable() {
        assertFalse(keys.hasKey(alias))
        assertNull(keys.mac(alias, byteArrayOf(1, 2, 3)))

        keys.ensureKey(alias)
        assertTrue(keys.hasKey(alias))
        val first = checkNotNull(keys.mac(alias, "salt1234".encodeToByteArray()))
        assertEquals(32, first.size)

        // 두 번 불러도 키를 바꾸지 않음
        keys.ensureKey(alias)
        assertArrayEquals(first, keys.mac(alias, "salt1234".encodeToByteArray()))
        assertFalse(first.contentEquals(keys.mac(alias, "salt1235".encodeToByteArray())))
    }
}
