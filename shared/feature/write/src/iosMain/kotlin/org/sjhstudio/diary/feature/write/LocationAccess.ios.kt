package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

@Composable
internal actual fun rememberLocationAccess(): LocationAccess = remember { IosLocationAccess() }

private class IosLocationAccess : LocationAccess {
    private val manager = CLLocationManager()

    // 관리자는 대리자를 약하게 잡으니 여기서 들고 있는다.
    private var waiter: AuthorizationWaiter? = null

    override fun request(onDone: () -> Unit) {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onDone()
            return
        }
        waiter =
            AuthorizationWaiter {
                waiter = null
                manager.delegate = null
                onDone()
            }
        manager.delegate = waiter
        manager.requestWhenInUseAuthorization()
    }

    override fun openSettings(locationOff: Boolean) {
        // iOS는 앱 설정만 바로 열 수 있다. 위치 서비스도 그 안내를 따라간다.
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

private class AuthorizationWaiter(private val onDecided: () -> Unit) :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        // 대리자를 달자마자 지금 상태로 한 번 불린다. 고른 뒤에만 넘어간다.
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) onDecided()
    }
}
