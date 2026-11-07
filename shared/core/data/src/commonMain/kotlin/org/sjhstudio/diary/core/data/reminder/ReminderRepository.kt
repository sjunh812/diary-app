package org.sjhstudio.diary.core.data.reminder

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.platform.ReminderScheduler

/**
 * 설정의 매일 알림을 예약한다(스펙 001 FR-022).
 * 부팅, 앱 업데이트, 시간대 바뀜, 알림이 온 뒤, 설정을 바꾼 뒤에 [reschedule]을 부른다.
 */
class ReminderRepository(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = TimeZone::currentSystemDefault
) {
    suspend fun reschedule() {
        val current = settings.settings.first()
        if (current.reminderEnabled) {
            scheduler.schedule(nextReminder(clock.now(), timeZone(), current.reminderMinuteOfDay))
        } else {
            scheduler.cancel()
        }
    }

    companion object {
        /**
         * 오늘 그 시각이 아직 안 지났으면 오늘, 지났으면 내일.
         * 그 시각이 없는 날(일광 절약 시간)은 그 뒤 첫 시각이다.
         */
        fun nextReminder(now: Instant, zone: TimeZone, minuteOfDay: Int): Instant {
            val time = LocalTime(minuteOfDay / 60, minuteOfDay % 60)
            val today = now.toLocalDateTime(zone).date
            val todayAt = today.atTime(time).toInstant(zone)
            return if (todayAt > now) todayAt else LocalDateTime(today.plus(1, DateTimeUnit.DAY), time).toInstant(zone)
        }
    }
}
