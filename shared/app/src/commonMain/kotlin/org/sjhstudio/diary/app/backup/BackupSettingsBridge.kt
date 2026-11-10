package org.sjhstudio.diary.app.backup

import kotlinx.coroutines.flow.first
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 백업에 담는 설정과 복원할 때 적용하는 설정
 * 잠금과 백업 설정은 바꾸지 않음(contracts/merge-rules.md "설정")
 */
class BackupSettingsBridge(
    private val settings: SettingsRepository,
    private val lockEnabled: suspend () -> Boolean,
    private val reschedule: suspend () -> Unit
) {
    suspend fun current(): ManifestSettings {
        val now = settings.settings.first()
        return ManifestSettings(
            font = now.font.id,
            bodyTextSize = now.bodyTextSize.id,
            themeMode = now.themeMode.id,
            reminderEnabled = now.reminderEnabled,
            reminderMinuteOfDay = now.reminderMinuteOfDay,
            listStyle = now.list.listStyle.id,
            favoritesOnly = now.list.favoritesOnly,
            askLocationAgain = now.askLocationAgain,
            lockEnabled = lockEnabled()
        )
    }

    /** 모르는 값은 건너뜀. 알림은 적용 뒤 다시 예약 */
    suspend fun apply(backup: ManifestSettings) {
        DiaryFont.fromId(backup.font)?.let { settings.setFont(it) }
        BodyTextSize.fromId(backup.bodyTextSize)?.let { settings.setBodyTextSize(it) }
        ThemeMode.fromId(backup.themeMode)?.let { settings.setThemeMode(it) }
        backup.listStyle?.let { settings.setListStyle(ListStyle.fromId(it)) }
        backup.askLocationAgain?.let { settings.setAskLocationAgain(it) }
        val minute = backup.reminderMinuteOfDay
        val enabled = backup.reminderEnabled
        if (enabled != null && minute != null) settings.setReminder(enabled, minute)
        reschedule()
    }
}
