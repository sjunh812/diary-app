package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class CurrentDateTest {
    private val clock =
        object : Clock {
            override fun now() = Instant.parse("2026-11-01T15:30:00Z")
        }

    /** 같은 순간이어도 시간대에 따라 날짜가 다름 */
    @Test
    fun date_follows_the_time_zone() {
        assertEquals(LocalDate(2026, 11, 1), currentDate(clock, TimeZone.UTC))
        assertEquals(LocalDate(2026, 11, 2), currentDate(clock, TimeZone.of("Asia/Seoul")))
    }
}
