package org.sjhstudio.diary.app.ads

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
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
data class AdRemovalState(val sheet: AdRemovalSheet? = null, val watching: Boolean = false) {
    /** 시트도 영상도 없음 */
    val idle: Boolean get() = sheet == null && !watching
}

/** 광고 제거 흐름에서 앱 틀이 쓰는 것. 첫 광고 안내의 [광고 없애기]가 열고 끝나길 기다림 */
interface AdRemovalFlow {
    val state: StateFlow<AdRemovalState>

    fun open()
}

/**
 * 광고 없애기 시트와 보상 광고 흐름. 앱 범위라 앱 틀과 광고 자리가 같은 상태를 봄
 * 가로 영상에서 돌아와 화면이 다시 만들어져도 결과 안내가 남음
 * [locked]가 참이 되면(앱이 잠김) 시트를 닫고 받던 영상을 멈춤
 */
class AdRemoval(private val scope: CoroutineScope, locked: Flow<Boolean> = flowOf(false), private val watchReward: suspend () -> Boolean) :
    AdRemovalFlow {
    private val _state = MutableStateFlow(AdRemovalState())
    override val state: StateFlow<AdRemovalState> = _state.asStateFlow()

    /** 받거나 보는 중인 보상 영상. 받는 동안만 멈출 수 있고, 띄운 뒤로는 저장소가 끝까지 감 */
    private var watching: Job? = null

    init {
        scope.launchSafely { locked.filter { it }.collect { dismiss() } }
    }

    override fun open() = _state.update { it.copy(sheet = AdRemovalSheet.Remove) }

    fun offerReward() = _state.update { it.copy(sheet = AdRemovalSheet.Reward) }

    /**
     * 닫으면 받던 영상도 멈춤. 받은 뒤 다른 화면 위에 뜨지 않게
     * 이미 띄운 영상은 끝까지 가서, 보는 중 표시는 영상이 정말 끝날 때 내림
     */
    fun dismiss() {
        watching?.cancel()
        _state.update { it.copy(sheet = null) }
    }

    /** 못 받았거나 끝까지 보지 않으면 그렇다고 알림. 말없이 닫으면 받은 줄 오해함 */
    fun watch() {
        if (watching?.isActive == true) return
        _state.update { it.copy(watching = true) }
        watching =
            scope.launchSafely(onFailure = { _state.update { it.copy(sheet = AdRemovalSheet.RewardFailed) } }) {
                val earned = watchReward()
                _state.update { it.copy(sheet = if (earned) null else AdRemovalSheet.RewardFailed) }
            }.also { job -> job.invokeOnCompletion { _state.update { it.copy(watching = false) } } }
    }
}

/** 광고 자리와 시트가 쓰는 화면 모델. 상태와 일은 [AdRemoval]이 가짐 */
class AdRemovalViewModel(private val removal: AdRemoval) : ViewModel() {
    val state: StateFlow<AdRemovalState> = removal.state

    fun open() = removal.open()

    fun offerReward() = removal.offerReward()

    fun dismiss() = removal.dismiss()

    fun watch() = removal.watch()
}
