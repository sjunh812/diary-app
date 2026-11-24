package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.model.withCustom
import org.sjhstudio.diary.core.model.withPreset
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class DefaultSettingsRepositoryTest {
    private val repository = DefaultSettingsRepository(SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences())))

    @Test
    fun each_setter_changes_only_its_value() = runTest {
        repository.setFont(DiaryFont.NANUM_CHEER)
        repository.setBodyTextSize(BodyTextSize.LARGE)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setReminder(enabled = true, minuteOfDay = 7 * 60)

        val settings = repository.settings.first()
        assertEquals(DiaryFont.NANUM_CHEER, settings.font)
        assertEquals(BodyTextSize.LARGE, settings.bodyTextSize)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(true, settings.reminderEnabled)
        assertEquals(420, settings.reminderMinuteOfDay)
        assertEquals(true, settings.askLocationAgain)
    }

    @Test
    fun picking_a_preset_keeps_the_custom_color() = runTest {
        repository.updateTheme { it.withCustom(0xC2185B) }
        assertEquals(ThemeColor.CUSTOM, repository.settings.first().theme.color)
        assertEquals(0xC2185B, repository.settings.first().theme.customRgb)

        repository.updateTheme { it.withPreset(ThemeColor.SEA) }

        val theme = repository.settings.first().theme
        assertEquals(ThemeColor.SEA, theme.color)
        assertEquals("#C2185B", theme.customHex)
    }

    @Test
    fun reminder_time_outside_a_day_is_rejected() = runTest {
        assertFailsWith<IllegalArgumentException> { repository.setReminder(enabled = true, minuteOfDay = 1440) }
        assertFailsWith<IllegalArgumentException> { repository.setReminder(enabled = true, minuteOfDay = -1) }
    }

    @Test
    fun font_replaced_notice_is_cleared_once_shown() = runTest {
        val store = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))
        store.update { it.copy(fontReplacedFrom = "교보손글씨 2019") }
        val repository = DefaultSettingsRepository(store)

        repository.clearFontReplacedNotice()

        assertNull(repository.settings.first().fontReplacedFrom)
    }
}
