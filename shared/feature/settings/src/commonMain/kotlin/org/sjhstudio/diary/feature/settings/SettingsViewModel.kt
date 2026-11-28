package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.data.reminder.NotificationAllowance
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.model.withCustom
import org.sjhstudio.diary.core.model.withPreset
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 설정 목록(S6)과 본문 글자 크기(G2)·화면 모드(G3)가 보는 값 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val lock: LockSettings = LockSettings(),
    /** 일부만 옮겨진 기기에만 "기록 이전 결과" 줄이 있음(FR-011) */
    val showsMigrationReport: Boolean = false,
    val trashCount: Int = 0,
    /** 확정된 요금제. 재설치 직후 스토어 답이 오기 전에는 null이고 그동안 Plus 카드를 그리지 않음 */
    val plan: Plan? = null,
    /** 마지막으로 끝까지 올린 백업 시각. 한 번도 없으면 null */
    val lastBackupAt: Instant? = null,
    /**
     * 쓰기 알림은 켰지만 기기 설정에서 알림을 막아 오지 않음
     * 알림을 켰는데 허용을 아직 읽지 않았으면 null. 시각을 먼저 그리면 곧 꺼짐으로 바뀜
     */
    val reminderBlocked: Boolean? = false,
    /**
     * 목록에서 서식 그대로 보기를 켤 수 있음
     * 요금제를 아직 모르면 null. 끝내 모르면 잠시 뒤 지금 값으로 정함(사진 한도와 같음)
     */
    val canFormat: Boolean? = null,
    /** 테마 색을 직접 고를 수 있음(스펙 010). 요금제를 아직 모르면 null */
    val canCustomTheme: Boolean? = null,
    /** Plus 기분 테마를 쓸 수 있음(스펙 013). 요금제를 아직 모르면 null */
    val canMoodTheme: Boolean? = null,
    /** 광고 동의가 필요한 지역이라 "광고 개인정보 설정" 줄이 있음 */
    val showsAdPrivacy: Boolean = false,
    /**
     * 설정을 읽었음
     * 읽기 전 기본값(잠금 꺼짐 등)을 그리면 켜 둔 것이 잠깐 꺼진 것으로 보임
     */
    val loaded: Boolean = false
)

/** 테마 화면 상태. 설정과 요금제만 봐서 다른 읽기를 기다리지 않음 */
data class ThemeUiState(val mode: ThemeMode, val theme: ThemeChoice, val listStyle: ListStyle, val canCustom: Boolean?)

class SettingsViewModel(
    private val settings: SettingsRepository,
    lock: LockRepository,
    trashCount: Flow<Int>,
    private val plans: PlanRepository,
    showsMigrationReport: suspend () -> Boolean,
    fonts: FontInstaller,
    private val notifications: NotificationAllowance,
    lastBackupAt: Flow<Instant?> = flowOf(null),
    adPrivacyRequired: Flow<Boolean> = flowOf(false),
    private val telemetry: Telemetry
) : ViewModel() {
    /** 본문 글자 크기 견본이 고른 글꼴(받은 글꼴 포함)로 그림 */
    val look: StateFlow<BodyLook> = settings.bodyLookIn(viewModelScope, fonts)

    /**
     * 이전 상태 파일을 읽지 못하면 줄을 두지 않음
     * 줄 하나 때문에 설정 전체를 못 쓰게 하지 않음
     */
    private val migrationReport = flow { emit(orElseOnFailure(false) { showsMigrationReport() }) }

    /**
     * 이미 아는 요금제부터, 모르면 기다리다 끝내 모르면 잠시 뒤 지금 값
     * 들어올 때마다 모름으로 돌아가 Plus 표시가 깜빡이지 않게
     */
    private val settledPlan: Flow<Plan?> =
        merge<Plan?>(plans.confirmed.filterNotNull(), flow { emit(plans.awaitConfirmed()) }).onStart { emit(plans.confirmedNow) }

    val theme: StateFlow<ThemeUiState?> =
        combine(settings.settings, settledPlan) { app, plan ->
            ThemeUiState(app.themeMode, app.theme, app.list.listStyle, plan?.customTheme)
        }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    val state: StateFlow<SettingsUiState> =
        combine(
            settings.settings,
            lock.settings,
            migrationReport,
            // 개수를 읽지 못해 빈 흐름이면 0. 줄 하나 때문에 설정 전체가 멈추지 않게
            trashCount.onEmpty { emit(0) },
            plans.confirmed
        ) { app, lockSettings, report, trashed, currentPlan ->
            SettingsUiState(app, lockSettings, report, trashed, currentPlan, loaded = true)
        }
            // 백업 상태를 읽지 못해도 설정은 그대로 그림
            .combine(lastBackupAt.onEmpty { emit(null) }) { state, backedUp -> state.copy(lastBackupAt = backedUp) }
            .combine(notifications.allowed) { state, allowed ->
                state.copy(reminderBlocked = if (state.settings.reminderEnabled) allowed?.not() else false)
            }
            .combine(settledPlan) { state, plan ->
                state.copy(canFormat = plan?.listFormatting, canCustomTheme = plan?.customTheme, canMoodTheme = plan?.moodThemes)
            }
            .combine(adPrivacyRequired) { state, required -> state.copy(showsAdPrivacy = required) }
            // 요금제는 기기에 남긴 값이라 바로 앎. 첫 화면부터 Plus 카드를 그려 늦게 끼어들지 않게
            .stateInSafely(viewModelScope, WhileUiSubscribed, SettingsUiState(plan = plans.confirmedNow))

    /** 기기 설정에서 바꾸고 돌아올 수 있어 화면에 돌아올 때마다 다시 읽음 */
    fun refreshNotifications() {
        viewModelScope.launchSafely { notifications.refresh() }
    }

    fun setBodyTextSize(size: BodyTextSize) {
        viewModelScope.launchWrite { settings.setBodyTextSize(size) }
        changed("text_size", size.id)
    }

    fun setPreviewLength(length: PreviewLength) {
        viewModelScope.launchWrite { settings.setPreviewLength(length) }
    }

    fun setShowListFormatting(show: Boolean) {
        viewModelScope.launchWrite { settings.setShowListFormatting(show) }
    }

    fun setListStyle(style: ListStyle) {
        viewModelScope.launchWrite { settings.setListStyle(style) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launchWrite { settings.setThemeMode(mode) }
        changed("theme", mode.id)
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launchWrite { settings.updateTheme { it.withPreset(color) } }
        changed("theme_color", color.id)
    }

    // 고른 색 값은 통계에 보내지 않음
    fun setCustomThemeColor(rgb: Int) {
        viewModelScope.launchWrite { settings.updateTheme { it.withCustom(rgb) } }
        changed("theme_color", ThemeColor.CUSTOM.id)
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
