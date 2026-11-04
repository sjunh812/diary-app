package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.LegacyBackupLink
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 앱 설정 `settings.preferences_pb`.
 * 키 이름, 타입, 기본값은 data-model.md 설정 표 그대로다.
 * 모르는 값은 기본값으로 읽고, 이 저장소가 모르는 키(스펙 002의 목록 모양 같은)는 건드리지 않는다.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<AppSettings> =
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toAppSettings())) }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val account = this[Keys.LEGACY_BACKUP_ACCOUNT]
        return AppSettings(
            font = DiaryFont.fromId(this[Keys.FONT]) ?: DiaryFont.DEFAULT,
            bodyTextSize = BodyTextSize.fromId(this[Keys.BODY_TEXT_SIZE]) ?: BodyTextSize.DEFAULT,
            themeMode = ThemeMode.fromId(this[Keys.THEME_MODE]) ?: ThemeMode.DEFAULT,
            askLocationAgain = this[Keys.ASK_LOCATION_AGAIN] ?: true,
            reminderEnabled = this[Keys.REMINDER_ENABLED] ?: false,
            reminderMinuteOfDay =
                this[Keys.REMINDER_MINUTE_OF_DAY]?.takeIf { it in MINUTES_OF_DAY }
                    ?: AppSettings.DEFAULT_REMINDER_MINUTE_OF_DAY,
            fontReplacedFrom = this[Keys.FONT_REPLACED_FROM],
            legacyBackup = account?.let { LegacyBackupLink(account = it, fileId = this[Keys.LEGACY_BACKUP_FILE_ID]) },
            list = ListPreferences(ListStyle.fromId(this[Keys.LIST_STYLE]), this[Keys.FAVORITES_ONLY] ?: false)
        )
    }

    private fun MutablePreferences.write(settings: AppSettings) {
        this[Keys.FONT] = settings.font.id
        this[Keys.LIST_STYLE] = settings.list.listStyle.id
        this[Keys.FAVORITES_ONLY] = settings.list.favoritesOnly
        this[Keys.BODY_TEXT_SIZE] = settings.bodyTextSize.id
        this[Keys.THEME_MODE] = settings.themeMode.id
        this[Keys.ASK_LOCATION_AGAIN] = settings.askLocationAgain
        this[Keys.REMINDER_ENABLED] = settings.reminderEnabled
        this[Keys.REMINDER_MINUTE_OF_DAY] = settings.reminderMinuteOfDay
        setOrRemove(Keys.FONT_REPLACED_FROM, settings.fontReplacedFrom)
        setOrRemove(Keys.LEGACY_BACKUP_ACCOUNT, settings.legacyBackup?.account)
        setOrRemove(Keys.LEGACY_BACKUP_FILE_ID, settings.legacyBackup?.fileId)
    }

    private object Keys {
        val FONT = stringPreferencesKey("font")
        val LIST_STYLE = stringPreferencesKey("list_style")
        val FAVORITES_ONLY = booleanPreferencesKey("favorites_only")
        val BODY_TEXT_SIZE = stringPreferencesKey("body_text_size")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ASK_LOCATION_AGAIN = booleanPreferencesKey("ask_location_again")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        val FONT_REPLACED_FROM = stringPreferencesKey("font_replaced_from")
        val LEGACY_BACKUP_ACCOUNT = stringPreferencesKey("legacy_backup_account")
        val LEGACY_BACKUP_FILE_ID = stringPreferencesKey("legacy_backup_file_id")
    }

    private companion object {
        val MINUTES_OF_DAY = 0 until 24 * 60
    }
}

internal fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else this[key] = value
}
