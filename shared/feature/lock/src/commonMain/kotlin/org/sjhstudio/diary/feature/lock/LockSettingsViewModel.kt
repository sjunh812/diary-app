package org.sjhstudio.diary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.launchSafely

/**
 * 잠금 설정(L4).
 * 켜기·끄기·바꾸기는 비밀번호 화면을 거쳐 [LockRepository]가 한다.
 */
class LockSettingsViewModel(
    private val repository: LockRepository,
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    val settings: StateFlow<LockSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LockSettings()
    )

    fun setBiometric(enabled: Boolean) {
        save { repository.setBiometric(enabled) }
        changed("biometric", enabled.toString())
    }

    fun setRelockAfter(value: RelockAfter) {
        save { repository.setRelockAfter(value) }
        changed("relock_after", value.id)
    }

    /**
     * 공간이 꽉 차 저장하지 못해도 꺼지지 않는다.
     * 화면은 저장된 값을 따라가므로 스위치가 그대로 남는다.
     */
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launchSafely {
            runCatching { block() }.onFailure { if (it is CancellationException) throw it }
        }
    }

    /**
     * 잠금을 켜거나 끈 뒤 부른다.
     * 켜고 끄기는 비밀번호 화면을 거쳐 [LockRepository]가 한다.
     */
    fun onLockChanged(enabled: Boolean) = changed("lock", enabled.toString())

    private fun changed(name: String, value: String) = telemetry.log(
        AnalyticsEvents.SETTINGS_CHANGED,
        mapOf(
            "name" to name,
            "value" to value
        )
    )
}
