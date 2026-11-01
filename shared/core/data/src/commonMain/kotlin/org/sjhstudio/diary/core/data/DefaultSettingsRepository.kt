package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ThemeMode

class DefaultSettingsRepository(private val store: SettingsStore) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.settings

    override suspend fun setFont(font: DiaryFont) = store.update { it.copy(font = font) }

    override suspend fun setBodyTextSize(size: BodyTextSize) = store.update { it.copy(bodyTextSize = size) }

    override suspend fun setThemeMode(mode: ThemeMode) = store.update { it.copy(themeMode = mode) }

    override suspend fun setAskLocationAgain(ask: Boolean) = store.update { it.copy(askLocationAgain = ask) }

    override suspend fun setReminder(enabled: Boolean, minuteOfDay: Int) {
        require(minuteOfDay in 0 until 24 * 60) { "알림 시각은 0~1439분이어야 한다: $minuteOfDay" }
        store.update { it.copy(reminderEnabled = enabled, reminderMinuteOfDay = minuteOfDay) }
    }

    override suspend fun clearFontReplacedNotice() = store.update { it.copy(fontReplacedFrom = null) }
}
