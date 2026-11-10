package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** 알림 허용을 묻는 시스템 창과 휴대폰 알림 설정 열기(매일 알림 R1) */
@Stable
interface NotificationAccess {
    /**
     * 묻는 창 띄우기
     * 이미 거절해 창이 뜨지 않는 기기도 결과는 옴
     */
    fun request(onResult: (granted: Boolean) -> Unit)

    /** 이 앱의 알림 설정 화면 열기 */
    fun openSettings()

    /**
     * 안내의 [허용하기]를 눌렀을 때
     * 아직 한 번도 묻지 않았으면 묻는 창, 이미 거절했으면 알림 설정
     * 안드로이드는 거절한 뒤로 창이 뜨지 않는 때를 미리 알 수 없어 설정을 띄움
     */
    fun allow(onResult: (granted: Boolean) -> Unit) = openSettings()
}

@Composable
expect fun rememberNotificationAccess(): NotificationAccess
