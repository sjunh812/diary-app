package org.sjhstudio.diary.core.platform

import android.app.NotificationManager
import android.content.Context

/**
 * 알림 허용 여부.
 * 안드로이드 13부터는 알림 권한, 그 아래는 앱 알림 켜짐을 같이 본다.
 * 시스템이 둘을 `areNotificationsEnabled` 하나로 알려 준다.
 */
class AndroidNotificationPermission(private val context: Context) : NotificationPermission {
    override suspend fun isGranted(): Boolean = context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true
}
