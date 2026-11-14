package org.sjhstudio.diary.app.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 백업에 담는 설정과 복원할 때 적용하는 설정(contracts/merge-rules.md "설정")
class BackupSettingsBridgeTest {
    @Test
    fun current_settings_are_all_written() = runTest {
        val settings = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK, reminderEnabled = true, reminderMinuteOfDay = 1_290))
        val bridge = BackupSettingsBridge(settings, lockEnabled = { true }, reschedule = {})

        val written = bridge.current()

        assertEquals(ThemeMode.DARK.id, written.themeMode)
        assertEquals(true, written.reminderEnabled)
        assertEquals(1_290, written.reminderMinuteOfDay)
        assertEquals(true, written.lockEnabled)
    }

    @Test
    fun known_values_are_applied_and_unknown_ones_skipped_then_reminders_rescheduled() = runTest {
        val settings = FakeSettingsRepository()
        var rescheduled = 0
        val bridge = BackupSettingsBridge(settings, lockEnabled = { false }, reschedule = { rescheduled++ })

        bridge.apply(
            ManifestSettings(
                font = "없는 글꼴",
                bodyTextSize = BodyTextSize.entries.last().id,
                themeMode = ThemeMode.LIGHT.id,
                reminderEnabled = true,
                reminderMinuteOfDay = 540,
                lockEnabled = true
            )
        )

        val now = settings.settings.value
        assertEquals(DiaryFont.DEFAULT, now.font)
        assertEquals(BodyTextSize.entries.last(), now.bodyTextSize)
        assertEquals(ThemeMode.LIGHT, now.themeMode)
        assertEquals(true, now.reminderEnabled)
        assertEquals(540, now.reminderMinuteOfDay)
        assertEquals(1, rescheduled)
    }
}
