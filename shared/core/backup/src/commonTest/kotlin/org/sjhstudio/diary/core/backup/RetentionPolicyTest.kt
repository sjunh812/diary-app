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
}
