package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** 알림 허용을 묻는 시스템 창과 휴대폰 알림 설정 열기(매일 알림 R1). */
@Stable
interface NotificationAccess {
    /**
     * 묻는 창을 띄운다.
     * 이미 거절해 창이 뜨지 않는 기기도 결과는 온다.
     */
    fun request(onResult: (granted: Boolean) -> Unit)

    /** 이 앱의 알림 설정 화면을 연다. */
    fun openSettings()
}

@Composable
expect fun rememberNotificationAccess(): NotificationAccess
