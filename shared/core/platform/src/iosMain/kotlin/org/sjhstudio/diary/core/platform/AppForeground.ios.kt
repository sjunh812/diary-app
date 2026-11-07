package org.sjhstudio.diary.core.platform

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

actual object AppForeground {
    actual fun observe(onBackground: () -> Unit, onForeground: () -> Unit): () -> Unit {
        val center = NSNotificationCenter.defaultCenter
        // 생체 인증 창이 뜰 때는 비활성(resign active)만 되고 백그라운드로 가지 않는다.
        val background =
            center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { onBackground() }
        val foreground =
            center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue) { onForeground() }
        return {
            center.removeObserver(background)
            center.removeObserver(foreground)
        }
    }
}
