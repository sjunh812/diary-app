package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

/**
 * 설정, 잠금, 백업 DataStore 파일 열기(research R16, 스펙 003 data-model)
 * 폴더는 `AppFiles.dataStoreDirectory`
 * 한 파일에 DataStore는 프로세스마다 하나만 둠
 * Koin에서 한 번만 만듦
 */
class DataStoreFactory(private val directory: String, private val scope: CoroutineScope) {
    fun settings(): DataStore<Preferences> = create(SETTINGS_FILE, ReplaceFileCorruptionHandler { emptyPreferences() })

    // 잠금 파일이 깨지면 잠금을 푸는 대신 잠긴 채 "키 없음" 흐름으로 보냄(헌법 II)
    fun lock(): DataStore<Preferences> = create(LOCK_FILE, ReplaceFileCorruptionHandler { LockStore.lockedWithoutKey() })

    // 백업 설정 파일이 깨지면 빈 값으로 시작
    // 드라이브 백업은 그대로라 다시 연결하면 됨
    fun backup(): DataStore<Preferences> = create(BACKUP_FILE, ReplaceFileCorruptionHandler { emptyPreferences() })

    fun welcome(): DataStore<Preferences> = create(WELCOME_FILE, ReplaceFileCorruptionHandler { emptyPreferences() })

    fun ads(): DataStore<Preferences> = create(ADS_FILE, ReplaceFileCorruptionHandler { emptyPreferences() })

    private fun create(fileName: String, corruptionHandler: ReplaceFileCorruptionHandler<Preferences>): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = corruptionHandler,
            scope = scope,
            produceFile = { "$directory/$fileName".toPath() }
        )

    companion object {
        const val SETTINGS_FILE = "settings.preferences_pb"
        const val LOCK_FILE = "lock.preferences_pb"
        const val BACKUP_FILE = "backup.preferences_pb"
        const val WELCOME_FILE = "welcome.preferences_pb"
        const val ADS_FILE = "ads.preferences_pb"
    }
}
