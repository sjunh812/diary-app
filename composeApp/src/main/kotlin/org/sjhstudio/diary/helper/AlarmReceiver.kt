package org.sjhstudio.diary.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.R
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.StartupState
import org.sjhstudio.diary.core.data.reminder.ReminderRepository

/**
 * 매일 알림 수신기(스펙 001 FR-022, research R15).
 * 옛 앱(1.7.x)이 예약에 쓴 이름 그대로다. 이름을 바꾸면 업데이트 직후 옛 예약이 갈 곳을 잃는다.
 * 부팅, 앱 업데이트, 시각·시간대 바뀜에는 다시 예약만 하고, 예약한 시각에는 알림을 띄운 뒤 다음 날을 예약한다.
 * 알림에는 일기 내용을 넣지 않는다(헌법 II).
 */
class AlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 앱을 한 번도 열지 않았어도 옛 설정 잇기가 끝난 뒤의 설정으로 예약한다.
                val startup = get<Startup>()
                startup.start()
                startup.state.first { it is StartupState.Ready }
                // 옛 앱과 새 앱이 건 예약은 동작 없이 온다. 시스템 알림(부팅 같은)에는 동작이 있다.
                if (intent.action == null) showReminder(context)
                get<ReminderRepository>().reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private fun showReminder(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 옛 앱과 같은 채널 ID라서 사용자가 바꿔 둔 소리·진동 설정이 이어진다.
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.reminder_body))
                .setAutoCancel(true)
                .apply { if (open != null) setContentIntent(PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_IMMUTABLE)) }
                .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "alarm_id"
        const val NOTIFICATION_ID = 1
    }
}
