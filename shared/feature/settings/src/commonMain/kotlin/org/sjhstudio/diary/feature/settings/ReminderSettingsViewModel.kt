package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.reminder.ReminderRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 매일 알림(R1) 상태.
 * [notificationsAllowed]는 아직 모르면 null이다.
 * 알림을 꺼 두면 허용 안내를 보이지 않는다.
 */
data class ReminderUiState(
    val enabled: Boolean = false,
    val minuteOfDay: Int = AppSettings.DEFAULT_REMINDER_MINUTE_OF_DAY,
    val notificationsAllowed: Boolean? = null
) {
    val showsPermissionNotice: Boolean
        get() = enabled && notificationsAllowed == false
}

/**
 * 매일 알림 켜기·끄기와 시각.
 * 바꿀 때마다 다시 예약한다(스펙 001 FR-022).
 */
class ReminderSettingsViewModel(
    private val settings: SettingsRepository,
    private val reminder: ReminderRepository,
    private val permission: NotificationPermission,
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    private val allowed = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<ReminderUiState> =
        combine(settings.settings, allowed) { app, granted -> ReminderUiState(app.reminderEnabled, app.reminderMinuteOfDay, granted) }
            .stateInSafely(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderUiState())

    /** 화면에 돌아올 때마다 휴대폰 설정의 알림 허용을 다시 읽는다. */
    fun refreshPermission() {
        viewModelScope.launchSafely { allowed.value = permission.isGranted() }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launchWrite {
            settings.setReminder(enabled, state.value.minuteOfDay)
            reminder.reschedule()
        }
        // 켜고 끈 것만 보낸다.
        // 시각은 보내지 않는다(contracts/app-shell.md 사용 통계 고정 표).
        telemetry.log(AnalyticsEvents.SETTINGS_CHANGED, mapOf("name" to "reminder", "value" to enabled.toString()))
    }

    fun setTime(minuteOfDay: Int) {
        viewModelScope.launchWrite {
            settings.setReminder(enabled = true, minuteOfDay = minuteOfDay)
            reminder.reschedule()
        }
    }
}
