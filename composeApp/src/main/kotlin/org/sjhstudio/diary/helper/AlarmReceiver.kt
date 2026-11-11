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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.MainActivity
import org.sjhstudio.diary.R
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.StartupState
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.reminder.ReminderRepository
import org.sjhstudio.diary.core.platform.R as PlatformR

/**
 * 매일 알림 수신기(스펙 001 FR-022, research R15)
 * 옛 앱(1.7.x)이 예약에 쓴 이름 그대로
 * 이름을 바꾸면 업데이트 직후 옛 예약이 갈 곳을 잃음
 * 부팅, 앱 업데이트, 시각·시간대 바뀜에는 다시 예약만 하고, 예약한 시각에는 알림을 띄운 뒤 다음 날을 예약
 * 알림에는 일기 내용을 넣지 않음(헌법 II)
 */
class AlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 시스템은 수신기를 오래 기다리지 않음
                // 알림 하나 때문에 앱이 응답 없음으로 잡히면 안 됨
                withTimeoutOrNull(RECEIVER_MILLIS) {
                    // 앱을 한 번도 열지 않았어도 옛 설정 잇기가 끝난 뒤의 설정으로 예약
                    val startup = get<Startup>()
                    startup.start()
                    startup.state.first { it is StartupState.Ready }
                    // 옛 앱과 새 앱이 건 예약은 동작 없이 옴
                    // 시스템 알림(부팅 같은)에는 동작이 있음
                    // 복원이나 되돌리기로 알림이 꺼졌으면 남아 있던 예약이 와도 띄우지 않음
                    get<ReminderRepository>().reschedule()
                    if (intent.action == null && get<SettingsRepository>().settings.first().reminderEnabled) showReminder(context)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 공간이 찬 기기에서 설정을 읽지 못해도 앱이 꺼지면 안 됨
                // 다음에 앱을 열면 다시 예약
            } finally {
                pending.finish()
            }
        }
    }

    internal fun showReminder(app: Context) {
        // 앱 언어를 따로 고른 사람에게도 그 언어로 알림. 수신기의 기본 문맥은 시스템 언어
        val context = ContextCompat.getContextForLanguage(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 옛 앱과 같은 채널 ID라 사용자가 바꿔 둔 소리·진동 설정이 이어짐
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        // 쓰기 화면으로 바로 감
        // 앱이 떠 있으면 새로 만들지 않고 그 화면이 받음
        val open =
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_WRITE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(PlatformR.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.reminder_body))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_IMMUTABLE))
                .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "alarm_id"
        const val NOTIFICATION_ID = 1
    }
}

private const val RECEIVER_MILLIS = 8_000L
