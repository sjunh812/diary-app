package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 설정 목록(S6)과 본문 글자 크기(G2)·화면 모드(G3)가 보는 값 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val lock: LockSettings = LockSettings(),
    /** 일부만 옮겨진 기기에만 "기록 이전 결과" 줄이 있음(FR-011) */
    val showsMigrationReport: Boolean = false,
    val trashCount: Int = 0,
    val plan: Plan = Plan.FREE,
    /** 마지막으로 끝까지 올린 백업 시각. 한 번도 없으면 null */
    val lastBackupAt: Instant? = null,
    /**
     * 설정을 읽었음
     * 읽기 전 기본값(잠금 꺼짐 등)을 그리면 켜 둔 것이 잠깐 꺼진 것으로 보임
     */
    val loaded: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    lock: LockRepository,
    trashCount: Flow<Int>,
    plan: Flow<Plan>,
    showsMigrationReport: suspend () -> Boolean,
    fonts: FontInstaller,
    lastBackupAt: Flow<Instant?> = flowOf(null),
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    /** 본문 글자 크기 견본이 고른 글꼴(받은 글꼴 포함)로 그림 */
    val look: StateFlow<BodyLook> = settings.bodyLookIn(viewModelScope, fonts)

    /**
     * 이전 상태 파일을 읽지 못하면 줄을 두지 않음
     * 줄 하나 때문에 설정 전체를 못 쓰게 하지 않음
     */
    private val migrationReport = flow { emit(orElseOnFailure(false) { showsMigrationReport() }) }

    val state: StateFlow<SettingsUiState> =
        combine(
            settings.settings,
            lock.settings,
            migrationReport,
            // 개수를 읽지 못해 빈 흐름이면 0. 줄 하나 때문에 설정 전체가 멈추지 않게
            trashCount.onEmpty { emit(0) },
            plan
        ) { app, lockSettings, report, trashed, currentPlan ->
            SettingsUiState(app, lockSettings, report, trashed, currentPlan, loaded = true)
        }
            // 백업 상태를 읽지 못해도 설정은 그대로 그림
            .combine(lastBackupAt.onEmpty { emit(null) }) { state, backedUp -> state.copy(lastBackupAt = backedUp) }
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

    // 설정 이름과 고른 값의 ID만 보냄(contracts/app-shell.md 사용 통계 고정 표)
    private fun changed(name: String, value: String) = telemetry.log(
        AnalyticsEvents.SETTINGS_CHANGED,
        mapOf(
            "name" to name,
            "value" to value
        )
    )
}
