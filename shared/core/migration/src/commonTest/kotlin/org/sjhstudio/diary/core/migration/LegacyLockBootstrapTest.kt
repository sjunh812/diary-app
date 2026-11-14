package org.sjhstudio.diary.core.migration

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.lock.VerifyResult
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 옛 잠금 잇기(스펙 001 T054, contracts/legacy-mapping.md "설정")
class LegacyLockBootstrapTest {
    private val dataStore = InMemoryDataStore<Preferences>(emptyPreferences())
    private val store = LockStore(dataStore)
    private val repository = DefaultLockRepository(store, FakePinKeyStore(), keyWork = Dispatchers.Unconfined)
    private val now = Instant.parse("2026-11-02T09:00:00Z")

    private fun pref(vararg lines: String) =
        """
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
        ${lines.joinToString("\n")}
        </map>
        """.trimIndent()

    private suspend fun TempDir.bootstrap(xml: String?, step: MigrationStep = MigrationStep.NOT_STARTED): Boolean {
        val files = LegacyFiles(root / "databases/note.db", root / "shared_prefs/pref.xml", root / "shared_prefs/pref2.xml")
        if (xml != null) write("shared_prefs/pref.xml", xml)
        return LegacyLockBootstrap(fs, files, { step }, repository, store).run()
    }

    @Test
    fun four_digit_password_turns_lock_on_with_the_same_pin() = runTest {
        TempDir().use { dir ->
            val xml = pref("""<string name="password">0427</string>""", """<boolean name="use_pw" value="true" />""")
            assertTrue(dir.bootstrap(xml))

            val lock = store.lock.first()
            assertTrue(lock.enabled)
            assertTrue(lock.hasPin)
            assertFalse(lock.biometricEnabled)
            assertEquals(VerifyResult.Ok, repository.verify(checkNotNull(Pin.of("0427")), now))
            // 원본은 그대로
            assertEquals(xml, dir.fs.read(dir.root / "shared_prefs/pref.xml") { readUtf8() })
        }
    }

    /** 확인값과 저장소 어디에도 옛 비밀번호 평문이 없음 */
    @Test
    fun stored_lock_has_no_plain_password() = runTest {
        TempDir().use { dir ->
            dir.bootstrap(pref("""<string name="password">0427</string>""", """<boolean name="use_pw" value="true" />"""))
            val stored = dataStore.data.first().toString()
            assertFalse("0427" in stored)
            assertFalse("0427".encodeToByteArray().toByteString().base64() in stored)
        }
    }

    @Test
    fun malformed_password_locks_without_a_pin() = runTest {
        for (password in listOf("123", "12345", "12a4", "")) {
            store.update { LockSettings() }
            TempDir().use { dir ->
                dir.bootstrap(pref("""<string name="password">$password</string>""", """<boolean name="use_pw" value="true" />"""))
                val lock = store.lock.first()
                assertTrue(lock.enabled, password)
                assertFalse(lock.hasPin, password)
                assertTrue(repository.needsNewPin(), password)
            }
        }
    }

    @Test
    fun fingerprint_follows_only_when_lock_turns_on() = runTest {
        TempDir().use { dir ->
            dir.bootstrap(pref("""<boolean name="use_pw" value="false" />""", """<boolean name="finger_print" value="true" />"""))
            assertFalse(store.lock.first().enabled)
            assertFalse(store.lock.first().biometricEnabled)
        }
        TempDir().use { dir ->
            dir.bootstrap(
                pref(
                    """<string name="password">0427</string>""",
                    """<boolean name="use_pw" value="true" />""",
                    """<boolean name="finger_print" value="true" />"""
                )
            )
            assertTrue(store.lock.first().biometricEnabled)
        }
    }

    /**
     * 이전이 시작된 뒤에는 옛 파일로 잠금을 다시 켜지 않음
     * 사용자가 새 앱에서 끈 잠금이 되살아나면 안 됨
     */
    @Test
    fun runs_only_before_migration_starts() = runTest {
        for (step in MigrationStep.entries - MigrationStep.NOT_STARTED) {
            TempDir().use { dir ->
                val on = pref("""<string name="password">0427</string>""", """<boolean name="use_pw" value="true" />""")
                assertFalse(dir.bootstrap(on, step), step.id)
                assertFalse(store.lock.first().enabled, step.id)
            }
        }
    }

    @Test
    fun new_install_without_legacy_prefs_does_nothing() = runTest {
        TempDir().use { dir ->
            assertFalse(dir.bootstrap(xml = null))
            assertFalse(store.lock.first().enabled)
        }
    }
}
