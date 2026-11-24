package org.sjhstudio.diary.app.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
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

    /** 스펙 010 US7: 테마 색이 다른 기기로 따라옴 */
    @Test
    fun theme_color_round_trips_with_the_remembered_custom_color() = runTest {
        val from = FakeSettingsRepository(AppSettings(theme = ThemeChoice(colorId = "sea", customHex = "#C2185B")))
        val written = BackupSettingsBridge(from, lockEnabled = { false }, reschedule = {}).current()
        val to = FakeSettingsRepository()

        BackupSettingsBridge(to, lockEnabled = { false }, reschedule = {}).apply(written)

        assertEquals(ThemeChoice(colorId = "sea", customHex = "#C2185B"), to.settings.value.theme)
    }

    @Test
    fun custom_theme_color_is_restored_as_current() = runTest {
        val to = FakeSettingsRepository()

        BackupSettingsBridge(to, lockEnabled = { false }, reschedule = {})
            .apply(ManifestSettings(themeColor = "custom", customThemeColor = "#2A9D8F"))

        assertEquals(ThemeColor.CUSTOM, to.settings.value.theme.color)
        assertEquals(0x2A9D8F, to.settings.value.theme.customRgb)
    }

    /** 옛 백업(칸 없음)과 모르는 값은 기기 테마를 덮지 않음 */
    @Test
    fun missing_or_unknown_theme_keeps_the_device_theme() = runTest {
        val device = ThemeChoice(colorId = "forest", customHex = "#123456")
        val to = FakeSettingsRepository(AppSettings(theme = device))
        val bridge = BackupSettingsBridge(to, lockEnabled = { false }, reschedule = {})

        bridge.apply(ManifestSettings())
        bridge.apply(ManifestSettings(themeColor = "mauve", customThemeColor = "broken"))
        bridge.apply(ManifestSettings(themeColor = "custom"))

        assertEquals(device, to.settings.value.theme)
    }

    /** 다음 판의 새 정한 색 이름을 모르면 직접 고른 색이 함께 와도 기기 색 그대로 */
    @Test
    fun unknown_color_with_a_custom_color_keeps_the_device_color() = runTest {
        val to = FakeSettingsRepository(AppSettings(theme = ThemeChoice(colorId = "forest")))

        BackupSettingsBridge(to, lockEnabled = { false }, reschedule = {})
            .apply(ManifestSettings(themeColor = "mauve", customThemeColor = "#2A9D8F"))

        assertEquals(ThemeColor.FOREST, to.settings.value.theme.color)
    }

    /** 색을 한 번도 고르지 않았어도 백업에는 라벤더를 적음. 다른 기기에 복원하면 라벤더로 돌아옴 */
    @Test
    fun the_default_color_is_written_to_the_backup() = runTest {
        val written = BackupSettingsBridge(FakeSettingsRepository(), lockEnabled = { false }, reschedule = {}).current()

        assertEquals(ThemeColor.LAVENDER.id, written.themeColor)
    }

    /** 스펙 008: 모르는 목록 값은 기기 값을 덮지 않음 */
    @Test
    fun unknown_list_values_keep_the_device_settings() = runTest {
        val settings =
            FakeSettingsRepository(AppSettings(list = ListPreferences(listStyle = ListStyle.CARD, previewLength = PreviewLength.FULL)))
        val bridge = BackupSettingsBridge(settings, lockEnabled = { false }, reschedule = {})

        bridge.apply(ManifestSettings(listStyle = "carousel", previewLength = "expanded", showListFormatting = true))

        val list = settings.settings.value.list
        assertEquals(ListStyle.CARD, list.listStyle)
        assertEquals(PreviewLength.FULL, list.previewLength)
        assertEquals(false, list.previewChosen)
        assertEquals(true, list.showFormatting)
    }

    @Test
    fun list_values_round_trip() = runTest {
        val from = FakeSettingsRepository(AppSettings(list = ListPreferences(previewLength = PreviewLength.FULL, showFormatting = true)))
        val to = FakeSettingsRepository()

        BackupSettingsBridge(to, lockEnabled = {
            false
        }, reschedule = {}).apply(BackupSettingsBridge(from, lockEnabled = { false }, reschedule = {}).current())

        assertEquals(PreviewLength.FULL, to.settings.value.list.previewLength)
        assertEquals(true, to.settings.value.list.showFormatting)
    }

    @Test
    fun five_lines_round_trip() = runTest {
        val from = FakeSettingsRepository(AppSettings(list = ListPreferences(previewLength = PreviewLength.FIVE_LINES)))
        val to = FakeSettingsRepository()

        BackupSettingsBridge(to, lockEnabled = {
            false
        }, reschedule = {}).apply(BackupSettingsBridge(from, lockEnabled = { false }, reschedule = {}).current())

        assertEquals(PreviewLength.FIVE_LINES, to.settings.value.list.previewLength)
    }
}
