package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 기분 테마 화면 상태(스펙 013). 요금제를 아직 모르면 [canPlus]가 null이고 잠금 표시를 하지 않음 */
data class MoodThemeUiState(val look: MoodLook, val canPlus: Boolean?)

/** 기분 테마와 클레이 꾸미기 화면 */
class MoodThemeViewModel(private val settings: SettingsRepository, plans: PlanRepository, private val telemetry: Telemetry) : ViewModel() {
    // 아는 요금제를 먼저 쓰고, 모르면 확정될 때까지 기다림
    // 들어올 때 잠금 표시 깜빡임 방지
    private val settledPlan: Flow<Plan?> =
        merge<Plan?>(plans.confirmed.filterNotNull(), flow { emit(plans.awaitConfirmed()) }).onStart { emit(plans.confirmedNow) }

    val state: StateFlow<MoodThemeUiState?> =
        combine(settings.settings, settledPlan) { app, plan -> MoodThemeUiState(app.moodLook, plan?.moodThemes) }
            .stateInSafely(viewModelScope, WhileUiSubscribed, null)

    fun setTheme(theme: MoodTheme) {
        viewModelScope.launchWrite { settings.updateMoodLook { it.withTheme(theme) } }
        // 테마 id만 보냄. 기분 기록은 보내지 않음(헌법 II)
        telemetry.log(AnalyticsEvents.SETTINGS_CHANGED, mapOf("name" to "mood_theme", "value" to theme.id))
    }

    fun setSticker(on: Boolean) = write { it.withSticker(on) }

    fun setHair(hair: ClayHair) = write { it.withClay(hair = hair) }

    fun setSkin(skin: ClaySkin) = write { it.withClay(skin = skin) }

    fun setGlasses(on: Boolean) = write { it.withClay(glasses = on) }

    private fun write(transform: (MoodLook) -> MoodLook) {
        viewModelScope.launchWrite { settings.updateMoodLook(transform) }
    }
}
