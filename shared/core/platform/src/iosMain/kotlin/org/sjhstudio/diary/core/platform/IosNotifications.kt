package org.sjhstudio.diary.core.platform

import kotlin.coroutines.resume
import kotlin.time.Instant
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** 알림이 허용(임시 허용 포함)되어 있는지. */
class IosNotificationPermission : NotificationPermission {
    override suspend fun isGranted(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(
                status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral
            )
        }
    }
}

/**
 * 매일 알림(스펙 001 FR-022, research R15).
 * 그 시각에 날마다 되풀이하는 달력 알림 하나를 건다.
 * 같은 이름으로 다시 걸면 앞의 것을 바꾼다.
 * 알림 글은 iosApp의 `Localizable.strings`에 있다.
 * 알림에는 일기 내용을 넣지 않는다(헌법 II).
 */
class IosReminderScheduler : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(at: Instant) {
        val date = NSDate.dateWithTimeIntervalSince1970(at.epochSeconds.toDouble())
        val time = NSCalendar.currentCalendar.components(NSCalendarUnitHour or NSCalendarUnitMinute, fromDate = date)
        val components = NSDateComponents().apply {
            hour = time.hour
            minute = time.minute
        }
        val content = UNMutableNotificationContent().apply {
            setTitle(localized(TITLE_KEY))
            setBody(localized(BODY_KEY))
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(REQUEST_ID, content, trigger),
            withCompletionHandler = null
        )
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
    }

    private fun localized(key: String): String = NSBundle.mainBundle.localizedStringForKey(key, value = null, table = null)

    companion object {
        /** 이 이름의 알림을 누르면 쓰기 화면이 열린다. */
        const val REQUEST_ID = "daily-reminder"
        private const val TITLE_KEY = "reminder_title"
        private const val BODY_KEY = "reminder_body"
    }
}
