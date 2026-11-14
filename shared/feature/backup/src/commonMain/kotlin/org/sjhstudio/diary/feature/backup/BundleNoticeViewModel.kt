package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

/**
 * 파일을 만들다 앱이 꺼진 일을 한 번 알림
 * 꺼내면 지워지는 값이라 화면 모델이 한 번만 꺼내 둠. 화면이 다시 만들어져도 안내를 잃지 않음
 */
class BundleNoticeViewModel(restore: RestoreRepository) : ViewModel() {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    init {
        viewModelScope.launchSafely { _pending.value = orElseOnFailure(false) { restore.takeStoppedBundle() } }
    }

    /** 안내를 띄움 */
    fun shown() {
        _pending.value = false
    }
}
