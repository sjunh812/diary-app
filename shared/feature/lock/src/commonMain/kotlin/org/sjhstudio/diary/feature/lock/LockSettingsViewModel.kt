package org.sjhstudio.diary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** 잠금 설정(L4). 켜기·끄기·바꾸기는 비밀번호 화면을 거쳐 [LockRepository]가 한다. */
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
        viewModelScope.launch { repository.setBiometric(enabled) }
        changed("biometric", enabled.toString())
    }

    fun setRelockAfter(value: RelockAfter) {
        viewModelScope.launch { repository.setRelockAfter(value) }
        changed("relock_after", value.id)
    }

    /** 잠금을 켜거나 끈 뒤 부른다. 켜고 끄기는 비밀번호 화면을 거쳐 [LockRepository]가 한다. */
    fun onLockChanged(enabled: Boolean) = changed("lock", enabled.toString())

    private fun changed(name: String, value: String) = telemetry.log(
        AnalyticsEvents.SETTINGS_CHANGED,
        mapOf(
            "name" to name,
            "value" to value
        )
    )
}
