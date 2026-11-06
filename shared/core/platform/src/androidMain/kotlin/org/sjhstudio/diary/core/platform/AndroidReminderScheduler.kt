package org.sjhstudio.diary.core.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlin.time.Instant

/**
 * `AlarmManager.setWindow`로 정해진 시각부터 10분 안에 알린다(research R15).
 * 정확한 알람 권한(`SCHEDULE_EXACT_ALARM`) 없이 된다.
 * 일기 알림은 몇 분 늦어도 된다.
 *
 * 옛 앱과 같은 수신기 이름(`helper.AlarmReceiver`)과 요청 번호 0을 써서, 업데이트 전에 걸어 둔 옛 예약을 그대로 덮어쓴다.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun schedule(at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), WINDOW_MILLIS, pendingIntent())
    }

    override fun cancel() {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent().setClassName(context, RECEIVER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        /**
         * 옛 앱이 예약에 쓴 수신기.
         * 이름을 바꾸면 업데이트 직후 옛 예약이 갈 곳을 잃는다.
         */
        const val RECEIVER = "org.sjhstudio.diary.helper.AlarmReceiver"
        private const val WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
