package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.PhotoStop

/**
 * `backup.preferences_pb`의 값(data-model.md 표)
 * 기기 것이라 스냅샷에도 기기 간 이전에도 넣지 않음
 */
data class BackupPrefs(
    val accountKey: String? = null,
    val accountLabel: String? = null,
    val rootFolderId: String? = null,
    /** [rootFolderId]가 어느 계정의 폴더인지 */
    val rootFolderAccount: String? = null,
    /**
     * 드라이브에서 지워진 백업 폴더를 새로 만듦
     * 사용자가 확인하면 내림
     */
    val folderRecreated: Boolean = false,
    /**
     * 드라이브에서 찾은 백업을 사용자가 아직 고르지 않음
     * 고르기 전에는 이 기기 일기를 올리지 않음
     */
    val foundBackupWaiting: Boolean = false,
    /** 기기에 둔 복원 목록이 어느 계정의 것인지 */
    val listedAccount: String? = null,
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
    val rollbackUntil: Instant? = null,
    /**
     * 마지막 성공 때의 일기·사진·설정 지문
     * 같으면 자동 백업을 건너뜀
     */
    val lastSuccessFingerprint: String? = null,
    /** 사진을 마저 올리려면 드라이브에 더 필요한 양 */
    val bytesShort: Long = 0,
    /**
     * 이 기기로 드라이브 백업을 복원했을 때 그 백업을 만든 시각
     * 이 기기에서 백업한 적은 없어도 드라이브에는 백업이 있다는 뜻
     */
    val restoredBackupAt: Instant? = null,
    /** 사진을 다 꺼내지 못한 복원 파일을 이때까지 둠 */
    val unpackUntil: Instant? = null,
    /** 복원 사진을 받다 멈춘 까닭 */
    val photoStop: PhotoStop? = null,
    /**
     * 대기 중인 사진 가운데 백업에 없어 받을 수 없는 장수
     * 남은 장수와 이어 받기에서 뺌
     */
    val photosUnreachable: Int = 0,
    /** 묶음 파일을 만드는 중. 앱이 꺼진 뒤에도 남아 있으면 끝나지 못한 것 */
    val bundleWriting: Boolean = false,
    /**
     * 기기에서 파일이 깨지거나 없어져 올릴 수 없는 사진의 내용 확인값
     * 기다려도 올라가지 않으니 남은 사진으로 세지 않음
     */
    val unusablePhotos: Set<String> = emptySet()
)

class BackupStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<BackupPrefs> = dataStore.data.map { it.toPrefs() }

    suspend fun current(): BackupPrefs = prefs.first()

    suspend fun update(transform: (BackupPrefs) -> BackupPrefs) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toPrefs())) }
    }

    /**
     * 기기 구분값
     * 처음 읽을 때 만듦
     * 앱을 삭제했다 다시 설치하면 새로 만듦
     */
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
        rootFolderAccount = this[Keys.ROOT_FOLDER_ACCOUNT],
        folderRecreated = this[Keys.FOLDER_RECREATED] ?: false,
        foundBackupWaiting = this[Keys.FOUND_BACKUP_WAITING] ?: false,
        listedAccount = this[Keys.LISTED_ACCOUNT],
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
        lastSuccessFingerprint = this[Keys.LAST_SUCCESS_FINGERPRINT],
        bytesShort = this[Keys.BYTES_SHORT] ?: 0,
        restoredBackupAt = this[Keys.RESTORED_BACKUP_AT]?.let(Instant::fromEpochMilliseconds),
        rollbackUntil = instant(Keys.ROLLBACK_UNTIL),
        unpackUntil = instant(Keys.UNPACK_UNTIL),
        photoStop = PhotoStop.fromName(this[Keys.PHOTO_STOP]),
        photosUnreachable = this[Keys.PHOTOS_UNREACHABLE] ?: 0,
        bundleWriting = this[Keys.BUNDLE_WRITING] ?: false,
        unusablePhotos = this[Keys.UNUSABLE_PHOTOS].orEmpty()
    )

    private fun Preferences.instant(key: Preferences.Key<Long>): Instant? = this[key]?.let(Instant::fromEpochMilliseconds)

    private fun MutablePreferences.write(prefs: BackupPrefs) {
        setOrRemove(Keys.ACCOUNT_KEY, prefs.accountKey)
        setOrRemove(Keys.ACCOUNT_LABEL, prefs.accountLabel)
        setOrRemove(Keys.ROOT_FOLDER_ID, prefs.rootFolderId)
        setOrRemove(Keys.ROOT_FOLDER_ACCOUNT, prefs.rootFolderAccount)
        this[Keys.FOLDER_RECREATED] = prefs.folderRecreated
        this[Keys.FOUND_BACKUP_WAITING] = prefs.foundBackupWaiting
        setOrRemove(Keys.LISTED_ACCOUNT, prefs.listedAccount)
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
        setOrRemove(Keys.LAST_SUCCESS_FINGERPRINT, prefs.lastSuccessFingerprint)
        this[Keys.BYTES_SHORT] = prefs.bytesShort
        prefs.restoredBackupAt?.let { this[Keys.RESTORED_BACKUP_AT] = it.toEpochMilliseconds() } ?: remove(Keys.RESTORED_BACKUP_AT)
        setOrRemove(Keys.UNPACK_UNTIL, prefs.unpackUntil?.toEpochMilliseconds())
        setOrRemove(Keys.PHOTO_STOP, prefs.photoStop?.name)
        this[Keys.PHOTOS_UNREACHABLE] = prefs.photosUnreachable
        this[Keys.BUNDLE_WRITING] = prefs.bundleWriting
        this[Keys.UNUSABLE_PHOTOS] = prefs.unusablePhotos
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private object Keys {
        val ACCOUNT_KEY = stringPreferencesKey("account_key")
        val ACCOUNT_LABEL = stringPreferencesKey("account_label")
        val ROOT_FOLDER_ID = stringPreferencesKey("root_folder_id")
        val ROOT_FOLDER_ACCOUNT = stringPreferencesKey("root_folder_account")
        val FOLDER_RECREATED = booleanPreferencesKey("folder_recreated")
        val FOUND_BACKUP_WAITING = booleanPreferencesKey("found_backup_waiting")
        val LISTED_ACCOUNT = stringPreferencesKey("listed_account")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTO_ENABLED = booleanPreferencesKey("auto_enabled")
        val PHOTOS_ON_CELLULAR = booleanPreferencesKey("photos_on_cellular")
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_SUCCESS_SNAPSHOT = stringPreferencesKey("last_success_snapshot")
        val LAST_SUCCESS_FINGERPRINT = stringPreferencesKey("last_success_fingerprint")
        val BYTES_SHORT = longPreferencesKey("bytes_short")
        val RESTORED_BACKUP_AT = longPreferencesKey("restored_backup_at")
        val LAST_ATTEMPT_AT = longPreferencesKey("last_attempt_at")
        val FAILING_SINCE = longPreferencesKey("failing_since")
        val LAST_FAILURE = stringPreferencesKey("last_failure")
        val FAILURE_NOTIFIED_AT = longPreferencesKey("failure_notified_at")
        val PHOTOS_NOT_BACKED_UP = intPreferencesKey("photos_not_backed_up")
        val NUDGE_DISMISS_COUNT = intPreferencesKey("nudge_dismiss_count")
        val NUDGE_HIDDEN_UNTIL = longPreferencesKey("nudge_hidden_until")
        val USER_DISCONNECTED = booleanPreferencesKey("user_disconnected")
        val ROLLBACK_UNTIL = longPreferencesKey("rollback_until")
        val UNPACK_UNTIL = longPreferencesKey("unpack_until")
        val PHOTO_STOP = stringPreferencesKey("photo_stop")
        val PHOTOS_UNREACHABLE = intPreferencesKey("photos_unreachable")
        val BUNDLE_WRITING = booleanPreferencesKey("bundle_writing")
        val UNUSABLE_PHOTOS = stringSetPreferencesKey("unusable_photos")
    }
}
