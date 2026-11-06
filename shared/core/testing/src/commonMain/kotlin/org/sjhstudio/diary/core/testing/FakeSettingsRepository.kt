package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 화면 모델 시험용 설정 저장소.
 * 처음 값을 정하고 바뀐 값을 바로 읽는다.
 */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state

    override suspend fun setFont(font: DiaryFont) = state.update { it.copy(font = font) }

    override suspend fun setBodyTextSize(size: BodyTextSize) = state.update { it.copy(bodyTextSize = size) }

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setAskLocationAgain(ask: Boolean) = state.update { it.copy(askLocationAgain = ask) }

    override suspend fun setReminder(enabled: Boolean, minuteOfDay: Int) =
        state.update { it.copy(reminderEnabled = enabled, reminderMinuteOfDay = minuteOfDay) }

    override suspend fun setListStyle(style: ListStyle) = state.update { it.copy(list = it.list.copy(listStyle = style)) }

    override suspend fun clearFontReplacedNotice() = state.update { it.copy(fontReplacedFrom = null) }
}
