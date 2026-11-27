package org.sjhstudio.diary.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.LegacyBackupLink
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode

class SettingsStoreTest {
    @Test
    fun empty_file_reads_as_defaults() = runTest {
        TestDataStores().use { stores ->
            assertEquals(AppSettings(), SettingsStore(stores.factory.settings()).settings.first())
        }
    }

    @Test
    fun update_round_trips_and_keeps_keys_from_data_model() = runTest {
        TestDataStores().use { stores ->
            val dataStore = stores.factory.settings()
            val store = SettingsStore(dataStore)
            val changed =
                AppSettings(
                    font = DiaryFont.OWNGLYPH_WISEELIST,
                    bodyTextSize = BodyTextSize.XLARGE,
                    themeMode = ThemeMode.DARK,
                    theme = ThemeChoice(colorId = "custom", customHex = "#C2185B"),
                    askLocationAgain = false,
                    reminderEnabled = true,
                    reminderMinuteOfDay = 8 * 60 + 30,
                    fontReplacedFrom = "교보손글씨 2019",
                    legacyBackup = LegacyBackupLink(account = "diary@example.com", fileId = "file-1")
                )

            store.update { changed }

            assertEquals(changed, store.settings.first())
            val raw = dataStore.data.first().asMap().mapKeys { it.key.name }
            assertEquals(
                setOf(
                    "font",
                    "list_style",
                    "favorites_only",
                    "preview_length",
                    "show_list_formatting",
                    "preview_chosen",
                    "legacy_preview_checked",
                    "body_text_size",
                    "theme_mode",
                    "theme_color",
                    "custom_theme_color",
                    "ask_location_again",
                    "reminder_enabled",
                    "reminder_minute_of_day",
                    "font_replaced_from",
                    "legacy_backup_account",
                    "legacy_backup_file_id"
                ),
                raw.keys
            )
            assertEquals("ownglyph_wiseelist", raw["font"])
        }
    }

    @Test
    fun unknown_theme_values_survive_other_updates() = runTest {
        TestDataStores().use { stores ->
            val dataStore = stores.factory.settings()
            dataStore.edit {
                it[stringPreferencesKey("theme_color")] = "mauve"
                it[stringPreferencesKey("custom_theme_color")] = "broken"
            }
            val store = SettingsStore(dataStore)

            store.update { it.copy(themeMode = ThemeMode.LIGHT) }

            val theme = store.settings.first().theme
            assertEquals(ThemeChoice(colorId = "mauve", customHex = "broken"), theme)
            assertEquals(ThemeColor.LAVENDER, theme.color)
            assertEquals(null, theme.customRgb)
        }
    }

    /** 기분 테마 키 다섯. 모르는 테마·머리 id는 다른 설정을 바꿔도 지우지 않음(스펙 013) */
    @Test
    fun mood_look_round_trips_and_unknown_ids_survive() = runTest {
        TestDataStores().use { stores ->
            val dataStore = stores.factory.settings()
            val store = SettingsStore(dataStore)
            val look = MoodLook(themeId = "clay", clayHair = "bun", claySkin = "skin3", clayGlasses = true, shapeSticker = false)
            store.update { it.copy(moodLook = look) }
            assertEquals(look, store.settings.first().moodLook)
            assertEquals("clay", dataStore.data.first()[stringPreferencesKey("mood_theme")])

            dataStore.edit {
                it[stringPreferencesKey("mood_theme")] = "galaxy"
                it[stringPreferencesKey("clay_hair")] = "mohawk"
            }
            store.update { it.copy(themeMode = ThemeMode.DARK) }

            val kept = store.settings.first().moodLook
            assertEquals("galaxy", kept.themeId)
            assertEquals("mohawk", kept.clayHair)
            assertEquals(MoodTheme.BASIC, kept.theme)
        }
    }

    @Test
    fun clearing_optional_values_removes_their_keys() = runTest {
        TestDataStores().use { stores ->
            val dataStore = stores.factory.settings()
            val store = SettingsStore(dataStore)
            store.update { it.copy(fontReplacedFrom = "옛 글꼴", legacyBackup = LegacyBackupLink("a@example.com", null)) }

            store.update { it.copy(fontReplacedFrom = null, legacyBackup = null) }

            val keys = dataStore.data.first().asMap().keys.map { it.name }
            assertEquals(false, "font_replaced_from" in keys)
            assertEquals(false, "legacy_backup_account" in keys)
            assertEquals(null, store.settings.first().legacyBackup)
        }
    }

    @Test
    fun unknown_values_fall_back_to_defaults_and_other_keys_survive() = runTest {
        TestDataStores().use { stores ->
            val dataStore = stores.factory.settings()
            dataStore.edit {
                it[stringPreferencesKey("font")] = "kyobo_2019"
                it[stringPreferencesKey("theme_mode")] = "sepia"
                it[intPreferencesKey("reminder_minute_of_day")] = 2000
                // 스펙 002가 더할 키
                // 이 저장소가 지우면 안 됨
                it[stringPreferencesKey("list_style")] = "card"
            }
            val store = SettingsStore(dataStore)

            val settings = store.settings.first()
            assertEquals(DiaryFont.PRETENDARD, settings.font)
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(AppSettings.DEFAULT_REMINDER_MINUTE_OF_DAY, settings.reminderMinuteOfDay)

            store.update { it.copy(reminderEnabled = true) }
            assertEquals("card", dataStore.data.first()[stringPreferencesKey("list_style")])
        }
    }
}
