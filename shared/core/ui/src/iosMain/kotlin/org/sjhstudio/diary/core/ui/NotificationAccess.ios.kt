package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
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
                // 결과는 다른 스레드로 온다. 화면 상태는 메인 스레드에서 바꾼다.
                dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
            }
        }

        override fun openSettings() {
            val url = NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString) ?: return
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
