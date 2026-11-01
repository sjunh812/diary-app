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
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class DefaultSettingsRepositoryTest {
    private val repository = DefaultSettingsRepository(SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences())))

    @Test
    fun each_setter_changes_only_its_value() = runTest {
        repository.setFont(DiaryFont.KKOMI)
        repository.setBodyTextSize(BodyTextSize.LARGE)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setReminder(enabled = true, minuteOfDay = 7 * 60)

        val settings = repository.settings.first()
        assertEquals(DiaryFont.KKOMI, settings.font)
        assertEquals(BodyTextSize.LARGE, settings.bodyTextSize)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(true, settings.reminderEnabled)
        assertEquals(420, settings.reminderMinuteOfDay)
        assertEquals(true, settings.askLocationAgain)
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
