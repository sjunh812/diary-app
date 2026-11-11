package org.sjhstudio.diary.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.platform.NotificationPermission

/**
 * 끝나면 알림으로 알릴 수 있는지
 * 허용 창이나 휴대폰 설정에서 바꾸고 돌아오면 다시 읽음
 * 모르는 동안은 알린다고 말하지 않음
 */
@Composable
internal fun notificationsAllowed(): Boolean {
    val permission = koinInject<NotificationPermission>()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val allowed by produceState(false, permission, lifecycle) {
        lifecycle.currentStateFlow.collect { state ->
            if (state == Lifecycle.State.RESUMED) value = orElseOnFailure(false) { permission.isGranted() }
        }
    }
    return allowed
}
