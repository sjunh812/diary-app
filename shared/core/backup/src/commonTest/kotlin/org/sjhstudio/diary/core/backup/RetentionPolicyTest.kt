package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertEquals

class RetentionPolicyTest {
    private fun backup(i: Long, month: String) = RetainedBackup("b$i", i, month)

    @Test
    fun five_or_fewer_stay() = assertEquals(emptySet(), RetentionPolicy.toDelete((1L..5).map { backup(it, "2026-11") }))

    @Test
    fun older_ones_keep_the_latest_of_each_month() {
        val backups =
            listOf(backup(1, "2026-09"), backup(2, "2026-09"), backup(3, "2026-10"), backup(4, "2026-10")) +
                (5L..9).map { backup(it, "2026-11") }

        assertEquals(setOf("b1", "b3"), RetentionPolicy.toDelete(backups))
    }

    @Test
    fun nothing_is_kept_twice_for_the_same_month() {
        val backups = (1L..8).map { backup(it, "2026-11") }

        assertEquals(setOf("b1", "b2"), RetentionPolicy.toDelete(backups))
    }

    /** 달마다 하나는 백업이 있는 최근 12개 달까지만. 한 기기에 많아야 17개 */
    @Test
    fun monthly_backups_stop_at_twelve_months() {
        val months = (1..20).map { "m${it.toString().padStart(2, '0')}" }
        val backups = months.mapIndexed { i, month -> backup(i.toLong() + 1, month) } + (21L..25).map { backup(it, "now") }

        val deleted = RetentionPolicy.toDelete(backups)

        assertEquals(17, backups.size - deleted.size)
        assertEquals((1L..8).map { "b$it" }.toSet(), deleted)
    }
}
