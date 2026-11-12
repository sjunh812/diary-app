package org.sjhstudio.diary.app.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.ui.launchSafely

/** 광고의 ×를 누른 뒤 차례로 뜨는 시트 */
enum class AdRemovalSheet { Remove, Reward, RewardFailed }

/** [watching]은 보상 영상을 받거나 보는 중 */
data class AdRemovalState(val sheet: AdRemovalSheet? = null, val watching: Boolean = false)

/**
 * 광고 없애기 시트와 보상 광고 흐름
 * 가로 영상에서 돌아와 화면이 다시 만들어져도 결과 안내가 남도록 화면 밖에 둠
 */
class AdRemovalViewModel(private val watchReward: suspend () -> Boolean) : ViewModel() {
    private val _state = MutableStateFlow(AdRemovalState())
    val state: StateFlow<AdRemovalState> = _state.asStateFlow()

    fun open() = _state.update { it.copy(sheet = AdRemovalSheet.Remove) }

    fun offerReward() = _state.update { it.copy(sheet = AdRemovalSheet.Reward) }

    fun dismiss() = _state.update { it.copy(sheet = null) }

    /** 못 받았거나 끝까지 보지 않으면 그렇다고 알림. 말없이 닫으면 받은 줄 오해함 */
    fun watch() {
        if (_state.value.watching) return
        _state.update { it.copy(watching = true) }
        viewModelScope.launchSafely {
            var earned = false
            try {
                earned = watchReward()
            } finally {
                _state.value = AdRemovalState(if (earned) null else AdRemovalSheet.RewardFailed)
            }
        }
    }
}
