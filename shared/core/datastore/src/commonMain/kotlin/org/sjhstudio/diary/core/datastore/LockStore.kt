package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter

/**
 * 잠금 설정 `lock.preferences_pb`
 * 키 이름, 타입, 기본값은 data-model.md 잠금 표 그대로
 * 백업에 올리지 않고 안드로이드 자동 백업에서도 뺌
 */
class LockStore(private val dataStore: DataStore<Preferences>) {
    // 파일을 읽지 못하면 잠금을 푸는 대신 잠긴 채 "키 없음" 흐름으로 보냄(헌법 II)
    val lock: Flow<LockSettings> =
        dataStore.data
            .catch { if (it is IOException) emit(lockedWithoutKey()) else throw it }
            .map { it.toLockSettings() }

    suspend fun update(transform: (LockSettings) -> LockSettings) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toLockSettings())) }
    }

    private fun Preferences.toLockSettings(): LockSettings = LockSettings(
        enabled = this[Keys.ENABLED] ?: false,
        pinMac = this[Keys.PIN_MAC],
        pinSalt = this[Keys.PIN_SALT],
        keyAlias = this[Keys.KEY_ALIAS] ?: LockSettings.DEFAULT_KEY_ALIAS,
        biometricEnabled = this[Keys.BIOMETRIC_ENABLED] ?: false,
        relockAfter = RelockAfter.fromId(this[Keys.RELOCK_AFTER]) ?: RelockAfter.DEFAULT,
        failedAttempts = this[Keys.FAILED_ATTEMPTS] ?: 0,
        retryAfter = this[Keys.RETRY_AFTER]?.takeIf { it > 0 }?.let(Instant::fromEpochMilliseconds)
    )

    private fun MutablePreferences.write(lock: LockSettings) {
        this[Keys.ENABLED] = lock.enabled
        setOrRemove(Keys.PIN_MAC, lock.pinMac)
        setOrRemove(Keys.PIN_SALT, lock.pinSalt)
        this[Keys.KEY_ALIAS] = lock.keyAlias
        this[Keys.BIOMETRIC_ENABLED] = lock.biometricEnabled
        this[Keys.RELOCK_AFTER] = lock.relockAfter.id
        this[Keys.FAILED_ATTEMPTS] = lock.failedAttempts
        this[Keys.RETRY_AFTER] = lock.retryAfter?.toEpochMilliseconds() ?: 0L
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PIN_MAC = stringPreferencesKey("pin_mac")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_ALIAS = stringPreferencesKey("key_alias")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val RELOCK_AFTER = stringPreferencesKey("relock_after")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val RETRY_AFTER = longPreferencesKey("retry_after")
    }

    companion object {
        /**
         * 잠금만 켜고 확인값이 없는 상태
         * 휴대폰 화면 잠금 확인 뒤 새 비밀번호를 정함
         */
        internal fun lockedWithoutKey(): Preferences = preferencesOf(Keys.ENABLED to true)
    }
}
