package org.sjhstudio.diary.app.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.ui.launchSafely

/** 광고의 ×를 누른 뒤 차례로 뜨는 시트 */
enum class AdRemovalSheet { Remove, Reward, RewardFailed }

/** [watching]은 보상 영상을 받거나 보는 중 */
data class AdRemovalState(val sheet: AdRemovalSheet? = null, val watching: Boolean = false)

/**
 * 광고 없애기 시트와 보상 광고 흐름
 * 가로 영상에서 돌아와 화면이 다시 만들어져도 결과 안내가 남도록 화면 밖에 둠
 * [locked]가 참이 되면(앱이 잠김) 시트를 닫고 받던 영상을 멈춤
 */
class AdRemovalViewModel(locked: Flow<Boolean> = flowOf(false), private val watchReward: suspend () -> Boolean) : ViewModel() {
    private val _state = MutableStateFlow(AdRemovalState())
    val state: StateFlow<AdRemovalState> = _state.asStateFlow()

    /** 받거나 보는 중인 보상 영상. 받는 동안만 멈출 수 있고, 띄운 뒤로는 저장소가 끝까지 감 */
    private var watching: Job? = null

    init {
        viewModelScope.launchSafely { locked.filter { it }.collect { dismiss() } }
    }

    fun open() = _state.update { it.copy(sheet = AdRemovalSheet.Remove) }

    fun offerReward() = _state.update { it.copy(sheet = AdRemovalSheet.Reward) }

    /** 닫으면 받던 영상도 멈춤. 받은 뒤 다른 화면 위에 뜨지 않게 */
    fun dismiss() {
        watching?.cancel()
        _state.value = AdRemovalState()
    }

    /** 못 받았거나 끝까지 보지 않으면 그렇다고 알림. 말없이 닫으면 받은 줄 오해함 */
    fun watch() {
        if (_state.value.watching) return
        _state.update { it.copy(watching = true) }
        watching =
            viewModelScope.launchSafely(onFailure = { _state.value = AdRemovalState(AdRemovalSheet.RewardFailed) }) {
                val earned = watchReward()
                _state.value = AdRemovalState(if (earned) null else AdRemovalSheet.RewardFailed)
            }
    }
}
