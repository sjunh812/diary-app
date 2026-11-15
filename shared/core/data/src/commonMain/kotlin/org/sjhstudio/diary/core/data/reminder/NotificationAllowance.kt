package org.sjhstudio.diary.core.data.reminder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.platform.NotificationPermission

/**
 * 휴대폰 설정의 알림 허용. 아직 모르면 null
 * 설정 목록 줄과 쓰기 알림 화면이 같은 값을 봄(태블릿 두 칸에서 함께 보임)
 * 앱 밖에서 바뀌어 화면에 돌아올 때마다 [refresh]
 */
class NotificationAllowance(private val permission: NotificationPermission) {
    private val current = MutableStateFlow<Boolean?>(null)
    val allowed: StateFlow<Boolean?> = current.asStateFlow()

    suspend fun refresh() {
        current.value = permission.isGranted()
    }
}
