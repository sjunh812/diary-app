package org.sjhstudio.diary.core.platform

/**
 * 휴대폰 설정에서 이 앱의 알림이 허용되어 있는지(매일 알림 R1, 스펙 001 FR-022).
 * 허용을 묻는 시스템 창은 화면이 띄운다. 여기서는 지금 상태만 읽는다.
 */
interface NotificationPermission {
    suspend fun isGranted(): Boolean
}

/** 알림이 없는 자리(미리보기, 시험). 늘 허용된 것으로 본다. */
object GrantedNotificationPermission : NotificationPermission {
    override suspend fun isGranted(): Boolean = true
}
