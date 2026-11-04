package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.sjhstudio.diary.core.model.BackupFailure

/** `backup.preferences_pb`의 값(data-model.md 표). 기기 것이라 스냅샷에도 기기 간 이전에도 넣지 않는다. */
data class BackupPrefs(
    val accountKey: String? = null,
    val accountLabel: String? = null,
    val rootFolderId: String? = null,
    val deviceId: String? = null,
    val autoEnabled: Boolean = false,
    val photosOnCellular: Boolean = false,
    val lastSuccessAt: Instant? = null,
    val lastSuccessSnapshot: String? = null,
    val lastAttemptAt: Instant? = null,
    val failingSince: Instant? = null,
    val lastFailure: BackupFailure? = null,
    val failureNotifiedAt: Instant? = null,
    val photosNotBackedUp: Int = 0,
    val nudgeDismissCount: Int = 0,
    val nudgeHiddenUntil: Instant? = null,
    val userDisconnected: Boolean = false,
    val rollbackUntil: Instant? = null
)

class BackupStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<BackupPrefs> = dataStore.data.map { it.toPrefs() }

    suspend fun current(): BackupPrefs = prefs.first()

    suspend fun update(transform: (BackupPrefs) -> BackupPrefs) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toPrefs())) }
    }

    /** 기기 구분값. 처음 읽을 때 만든다. 앱을 지웠다 깔면 새로 만든다. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun deviceId(): String {
        var id = ""
        dataStore.edit { preferences ->
            id = preferences[Keys.DEVICE_ID] ?: Uuid.random().toString().also { preferences[Keys.DEVICE_ID] = it }
        }
        return id
    }

    private fun Preferences.toPrefs() = BackupPrefs(
        accountKey = this[Keys.ACCOUNT_KEY],
        accountLabel = this[Keys.ACCOUNT_LABEL],
        rootFolderId = this[Keys.ROOT_FOLDER_ID],
        deviceId = this[Keys.DEVICE_ID],
        autoEnabled = this[Keys.AUTO_ENABLED] ?: false,
        photosOnCellular = this[Keys.PHOTOS_ON_CELLULAR] ?: false,
        lastSuccessAt = instant(Keys.LAST_SUCCESS_AT),
        lastSuccessSnapshot = this[Keys.LAST_SUCCESS_SNAPSHOT],
        lastAttemptAt = instant(Keys.LAST_ATTEMPT_AT),
        failingSince = instant(Keys.FAILING_SINCE),
        lastFailure = BackupFailure.fromName(this[Keys.LAST_FAILURE]),
        failureNotifiedAt = instant(Keys.FAILURE_NOTIFIED_AT),
        photosNotBackedUp = this[Keys.PHOTOS_NOT_BACKED_UP] ?: 0,
        nudgeDismissCount = this[Keys.NUDGE_DISMISS_COUNT] ?: 0,
        nudgeHiddenUntil = instant(Keys.NUDGE_HIDDEN_UNTIL),
        userDisconnected = this[Keys.USER_DISCONNECTED] ?: false,
        rollbackUntil = instant(Keys.ROLLBACK_UNTIL)
    )

    private fun Preferences.instant(key: Preferences.Key<Long>): Instant? = this[key]?.let(Instant::fromEpochMilliseconds)

    private fun MutablePreferences.write(prefs: BackupPrefs) {
        setOrRemove(Keys.ACCOUNT_KEY, prefs.accountKey)
        setOrRemove(Keys.ACCOUNT_LABEL, prefs.accountLabel)
        setOrRemove(Keys.ROOT_FOLDER_ID, prefs.rootFolderId)
        this[Keys.AUTO_ENABLED] = prefs.autoEnabled
        this[Keys.PHOTOS_ON_CELLULAR] = prefs.photosOnCellular
        setOrRemove(Keys.LAST_SUCCESS_AT, prefs.lastSuccessAt?.toEpochMilliseconds())
        setOrRemove(Keys.LAST_SUCCESS_SNAPSHOT, prefs.lastSuccessSnapshot)
        setOrRemove(Keys.LAST_ATTEMPT_AT, prefs.lastAttemptAt?.toEpochMilliseconds())
        setOrRemove(Keys.FAILING_SINCE, prefs.failingSince?.toEpochMilliseconds())
        setOrRemove(Keys.LAST_FAILURE, prefs.lastFailure?.name)
        setOrRemove(Keys.FAILURE_NOTIFIED_AT, prefs.failureNotifiedAt?.toEpochMilliseconds())
        this[Keys.PHOTOS_NOT_BACKED_UP] = prefs.photosNotBackedUp
        this[Keys.NUDGE_DISMISS_COUNT] = prefs.nudgeDismissCount
        setOrRemove(Keys.NUDGE_HIDDEN_UNTIL, prefs.nudgeHiddenUntil?.toEpochMilliseconds())
        this[Keys.USER_DISCONNECTED] = prefs.userDisconnected
        setOrRemove(Keys.ROLLBACK_UNTIL, prefs.rollbackUntil?.toEpochMilliseconds())
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private object Keys {
        val ACCOUNT_KEY = stringPreferencesKey("account_key")
        val ACCOUNT_LABEL = stringPreferencesKey("account_label")
        val ROOT_FOLDER_ID = stringPreferencesKey("root_folder_id")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTO_ENABLED = booleanPreferencesKey("auto_enabled")
        val PHOTOS_ON_CELLULAR = booleanPreferencesKey("photos_on_cellular")
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_SUCCESS_SNAPSHOT = stringPreferencesKey("last_success_snapshot")
        val LAST_ATTEMPT_AT = longPreferencesKey("last_attempt_at")
        val FAILING_SINCE = longPreferencesKey("failing_since")
        val LAST_FAILURE = stringPreferencesKey("last_failure")
        val FAILURE_NOTIFIED_AT = longPreferencesKey("failure_notified_at")
        val PHOTOS_NOT_BACKED_UP = intPreferencesKey("photos_not_backed_up")
        val NUDGE_DISMISS_COUNT = intPreferencesKey("nudge_dismiss_count")
        val NUDGE_HIDDEN_UNTIL = longPreferencesKey("nudge_hidden_until")
        val USER_DISCONNECTED = booleanPreferencesKey("user_disconnected")
        val ROLLBACK_UNTIL = longPreferencesKey("rollback_until")
    }
}
