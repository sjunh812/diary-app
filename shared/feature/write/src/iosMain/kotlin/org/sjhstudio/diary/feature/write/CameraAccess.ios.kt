package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** 막아 둔 채 촬영 화면을 띄우면 검은 화면만 보여 먼저 확인 */
@Composable
internal actual fun rememberCameraAccess(): CameraAccess = remember { IosCameraAccess() }

private class IosCameraAccess : CameraAccess {
    override fun blocked(): Boolean = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo).let {
        it == AVAuthorizationStatusDenied || it == AVAuthorizationStatusRestricted
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
