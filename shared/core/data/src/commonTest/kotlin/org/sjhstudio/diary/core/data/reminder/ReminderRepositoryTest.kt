package org.sjhstudio.diary.core.data.reminder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.platform.ReminderScheduler
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.TestClock

// 매일 알림 다음 시각(스펙 001 T066).
class ReminderRepositoryTest {
    private val seoul = TimeZone.of("Asia/Seoul")

    @Test
    fun today_if_not_passed_else_tomorrow() {
        val ten = 22 * 60
        assertEquals(
            Instant.parse("2026-11-02T13:00:00Z"),
            ReminderRepository.nextReminder(Instant.parse("2026-11-02T12:59:00Z"), seoul, ten)
        )
        assertEquals(
            Instant.parse("2026-11-03T13:00:00Z"),
            ReminderRepository.nextReminder(Instant.parse("2026-11-02T13:00:00Z"), seoul, ten)
        )
    }

    /** 시간대가 바뀌면 그 시간대의 같은 시각으로 맞춘다. */
    @Test
    fun follows_the_current_time_zone() {
        val now = Instant.parse("2026-11-02T00:00:00Z")
        assertEquals(Instant.parse("2026-11-02T13:00:00Z"), ReminderRepository.nextReminder(now, seoul, 22 * 60))
        assertEquals(Instant.parse("2026-11-02T22:00:00Z"), ReminderRepository.nextReminder(now, TimeZone.UTC, 22 * 60))
    }

    @Test
    fun schedules_when_on_and_cancels_when_off() = runTest {
        val scheduled = mutableListOf<Instant?>()
        val scheduler =
            object : ReminderScheduler {
                override fun schedule(at: Instant) {
                    scheduled += at
                }

                override fun cancel() {
                    scheduled += null
                }
            }
        val settings = FakeSettingsRepository()
        val repository = ReminderRepository(settings, scheduler, TestClock(Instant.parse("2026-11-02T00:00:00Z"))) { seoul }

        repository.reschedule()
        settings.setReminder(enabled = true, minuteOfDay = 7 * 60 + 30)
        repository.reschedule()

        assertEquals(listOf(null, Instant.parse("2026-11-02T22:30:00Z")), scheduled)
    }
}
