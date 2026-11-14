package org.sjhstudio.diary.feature.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.sjhstudio.diary.core.data.lock.LockRepository

class WaitLeftTest {
    private val now = Instant.fromEpochSeconds(1_800_000_000)

    @Test
    fun a_normal_wait_counts_down() = assertEquals(12.seconds, waitLeft(now + 12.seconds, now))

    @Test
    fun a_clock_set_back_cannot_block_the_keypad() = assertEquals(LockRepository.WAIT, waitLeft(now + 400.days, now))

    @Test
    fun a_finished_wait_is_not_positive() = assertEquals(-1.seconds, waitLeft(now - 1.seconds, now))
}
