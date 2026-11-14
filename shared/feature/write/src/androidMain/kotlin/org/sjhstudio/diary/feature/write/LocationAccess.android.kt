package org.sjhstudio.diary.feature.write

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.sjhstudio.diary.core.ui.openAppInfo
import org.sjhstudio.diary.core.ui.openSystemSettings

@Composable
internal actual fun rememberLocationAccess(): LocationAccess {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            pending?.invoke()
            pending = null
        }
    return remember(context, launcher) {
        object : LocationAccess {
            override fun request(onDone: () -> Unit) {
                pending = onDone
                // 정확한 위치만 물으면 안드로이드 12 일부 버전이 무시해 둘을 함께 물음
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }

            override fun openSettings(locationOff: Boolean) {
                if (locationOff) context.openSystemSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) else context.openAppInfo()
            }
        }
    }
}
