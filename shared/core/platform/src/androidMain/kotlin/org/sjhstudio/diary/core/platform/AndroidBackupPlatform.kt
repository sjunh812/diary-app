package org.sjhstudio.diary.core.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

class AndroidNetworkState(context: Context) : NetworkState {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val capabilities get() = manager.getNetworkCapabilities(manager.activeNetwork)

    override val isConnected: Boolean get() = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    override val isUnmetered: Boolean get() = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
}

class AndroidDeviceName(private val context: Context) : DeviceName {
    override fun current(): String =
        Settings.Global.getString(context.contentResolver, "device_name")?.takeIf { it.isNotBlank() } ?: Build.MODEL
}

class AndroidFreeSpace(private val context: Context) : FreeSpace {
    override fun bytes(): Long = StatFs(context.filesDir.path).availableBytes
}

/** 알림 하나를 띄움. 예약은 앱 모듈의 작업 관리자가 맡음 */
class BackupAlertPoster(context: Context, private val channelName: String) {
    private val context = context.applicationContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun show(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // 허용이 없으면 보내지 않고 홈의 빨간 점이 대신함
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = launch?.let { android.app.PendingIntent.getActivity(context, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE) }
        val builder =
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                android.app.Notification.Builder(context, CHANNEL)
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(context)
            }
        manager.notify(
            NOTIFICATION_ID,
            builder.setSmallIcon(
                R.drawable.ic_notification
            ).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(open).build()
        )
    }

    fun clear() = manager.cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL = "backup"
        const val NOTIFICATION_ID = 2001
    }
}

/** 앱이 화면에 있는지. 있으면 화면이 이미 알려 주니 끝났다는 알림을 보내지 않음 */
fun appIsVisible(): Boolean = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

/** 지금 백업의 진행 알림. 소리 없이 한 줄로 머묾 */
class BackupProgressNotifications(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(NotificationManager::class.java)

    /** 채널은 처음 한 번 이름과 함께 만듦 */
    fun createChannel(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW))
        }
    }

    fun build(title: String, body: String?, done: Int, total: Int): android.app.Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(context, CHANNEL)
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(context)
            }
        // 알림을 누르면 앱으로 돌아와 진행을 봄
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = launch?.let { android.app.PendingIntent.getActivity(context, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE) }
        return builder.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setContentIntent(open)
            .setOngoing(true).setProgress(total, done, total == 0).build()
    }

    fun update(title: String, body: String?, done: Int, total: Int) {
        manager.notify(ID, build(title, body, done, total))
    }

    /** 작업이 끝난 뒤 늦게 올라온 진행 알림이 남지 않게 지움 */
    fun clear() = manager.cancel(ID)

    companion object {
        const val ID = 2002
        private const val CHANNEL = "backup_progress"
    }
}
