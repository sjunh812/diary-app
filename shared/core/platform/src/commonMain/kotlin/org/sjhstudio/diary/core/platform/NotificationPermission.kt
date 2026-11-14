package org.sjhstudio.diary.core.platform

/**
 * 휴대폰 설정에서 이 앱의 알림이 허용됐는지(매일 알림 R1, 스펙 001 FR-022)
 * 허용을 묻는 시스템 창은 화면이 띄우고, 여기서는 지금 상태만 읽음
 */
interface NotificationPermission {
    suspend fun isGranted(): Boolean
}
