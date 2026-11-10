package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 설정 목록(S6)과 본문 글자 크기(G2)·화면 모드(G3)가 보는 값. */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val lock: LockSettings = LockSettings(),
    /** 일부만 옮겨진 기기에만 "기록 이전 결과" 줄이 있다(FR-011). */
    val showsMigrationReport: Boolean = false,
    /**
     * 설정을 읽었다.
     * 읽기 전의 기본값(잠금 꺼짐 같은)을 그리면 켜 둔 것이 잠깐 꺼진 것으로 보인다.
     */
    val loaded: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    lock: LockRepository,
    showsMigrationReport: suspend () -> Boolean,
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    /**
     * 이전 상태 파일을 읽지 못하면 줄을 두지 않는다.
     * 줄 하나 때문에 설정 전체를 못 쓰게 하지 않는다.
     */
    private val migrationReport = flow { emit(orElseOnFailure(false) { showsMigrationReport() }) }

    val state: StateFlow<SettingsUiState> =
        combine(settings.settings, lock.settings, migrationReport) { app, lockSettings, report ->
            SettingsUiState(app, lockSettings, report, loaded = true)
        }
            .stateInSafely(viewModelScope, WhileUiSubscribed, SettingsUiState())

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
