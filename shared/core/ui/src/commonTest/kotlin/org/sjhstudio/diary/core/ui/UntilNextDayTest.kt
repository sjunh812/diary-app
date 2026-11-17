package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// 자정 다시 읽기는 보인 날짜 기준
class UntilNextDayTest {
    private val zone = TimeZone.of("Asia/Seoul")

    @Test
    fun waits_until_the_midnight_after_the_shown_day() {
        val now = LocalDateTime(2026, 11, 17, 23, 0).toInstant(zone)
        assertEquals(1.hours + 1.seconds, untilNextDay(LocalDate(2026, 11, 17), now, zone))
    }

    @Test
    fun midnight_passed_after_reading_rereads_at_once() {
        val now = LocalDateTime(2026, 11, 18, 0, 0, 5).toInstant(zone)
        assertEquals(Duration.ZERO, untilNextDay(LocalDate(2026, 11, 17), now, zone))
    }

    @Test
    fun just_after_midnight_does_not_wait_the_margin() {
        val now = LocalDateTime(2026, 11, 18, 0, 0, 0, 500_000_000).toInstant(zone)
        assertEquals(Duration.ZERO, untilNextDay(LocalDate(2026, 11, 17), now, zone))
    }
}
