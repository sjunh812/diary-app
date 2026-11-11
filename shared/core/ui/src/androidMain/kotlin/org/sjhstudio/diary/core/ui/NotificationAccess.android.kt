package org.sjhstudio.diary.core.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
actual fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.invoke(granted)
            pending = null
        }
    return remember(context, launcher) {
        object : NotificationAccess {
            override fun request(onResult: (Boolean) -> Unit) {
                // 안드로이드 12까지는 알림 권한 없이 앱 알림 켜짐만 있음
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pending = onResult
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onResult(context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true)
                }
            }

            override fun askInContext() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                val permission = Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) return
                // 한 번 거절한 뒤에는 이 값이 참. 그때는 묻지 않고 매일 알림 설정의 [허용하기]에 맡김
                val activity = context.findActivity() ?: return
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) return
                launcher.launch(permission)
            }

            override fun openSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
