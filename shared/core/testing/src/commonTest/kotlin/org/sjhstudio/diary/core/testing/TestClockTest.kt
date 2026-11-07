package org.sjhstudio.diary.core.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TestClockTest {
    @Test
    fun advance_moves_wall_and_monotonic_together() {
        val clock = TestClock(Instant.parse("2026-11-01T00:00:00Z"))
        val mark = clock.markNow()

        clock.advance(61.seconds)

        assertEquals(Instant.parse("2026-11-01T00:01:01Z"), clock.now())
        assertEquals(61.seconds, mark.elapsedNow())
    }

    @Test
    fun setWallClock_keeps_monotonic_time() {
        val clock = TestClock()
        val mark = clock.markNow()

        clock.setWallClock(Instant.parse("2020-01-01T00:00:00Z"))

        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), clock.now())
        assertEquals(Duration.ZERO, mark.elapsedNow())
    }

    @Test
    fun advance_rejects_negative_duration() {
        assertFailsWith<IllegalArgumentException> { TestClock().advance((-1).minutes) }
    }
}
