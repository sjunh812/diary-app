package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.launchWrite

/** 설정 목록(S6)과 본문 글자 크기(G2)·화면 모드(G3)가 보는 값. */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val lock: LockSettings = LockSettings(),
    /** 일부만 옮겨진 기기에만 "기록 이전 결과" 줄이 있다(FR-011). */
    val showsMigrationReport: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    lock: LockRepository,
    showsMigrationReport: () -> Boolean,
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    private val migrationReport = showsMigrationReport()

    val state: StateFlow<SettingsUiState> =
        combine(settings.settings, lock.settings) { app, lockSettings -> SettingsUiState(app, lockSettings, migrationReport) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(showsMigrationReport = migrationReport))

    fun setBodyTextSize(size: BodyTextSize) {
        viewModelScope.launchWrite { settings.setBodyTextSize(size) }
        changed("text_size", size.id)
    }

    fun setListStyle(style: ListStyle) {
        viewModelScope.launchWrite { settings.setListStyle(style) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launchWrite { settings.setThemeMode(mode) }
        changed("theme", mode.id)
    }

    // 설정 이름과 고른 값의 ID만 보낸다(contracts/app-shell.md 사용 통계 고정 표).
    private fun changed(name: String, value: String) = telemetry.log(
        AnalyticsEvents.SETTINGS_CHANGED,
        mapOf(
            "name" to name,
            "value" to value
        )
    )
}
