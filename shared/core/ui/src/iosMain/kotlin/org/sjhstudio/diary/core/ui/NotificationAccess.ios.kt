package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberNotificationAccess(): NotificationAccess = remember {
    object : NotificationAccess {
        override fun request(onResult: (Boolean) -> Unit) {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound
            ) { granted, _ ->
                // 결과는 다른 스레드로 온다.
                // 화면 상태는 메인 스레드에서 바꾼다.
                dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
            }
        }

        // iOS는 처음 켤 때 창을 띄우지 않고 안내만 보이는 때가 있어서(앱 처음 실행 전에 알림을 켠 경우 등),
        // [허용하기]가 곧장 설정으로 가면 묻는 창을 한 번도 못 본다(2026-11-08 사용자 지적).
        override fun allow(onResult: (Boolean) -> Unit) {
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (settings?.authorizationStatus == UNAuthorizationStatusNotDetermined) request(onResult) else openSettings()
                }
            }
        }

        // 알림 설정 바로가기 주소는 설정 앱이 앞서 머물던 화면이나 설정 첫 화면을 여는 때가 있어(2026-11-08 사용자 지적),
        // 늘 이 앱의 설정 화면이 열리는 주소를 쓴다. 그 화면 맨 위에 [알림]이 있다.
        override fun openSettings() {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
