package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.sjhstudio.diary.core.ui.openAppInfo

/** 기본 카메라 앱에 촬영을 맡겨 권한 불필요 */
@Composable
internal actual fun rememberCameraAccess(): CameraAccess {
    val context = LocalContext.current
    return remember(context) {
        object : CameraAccess {
            override fun blocked() = false

            override fun openSettings() = context.openAppInfo()
        }
    }
}
