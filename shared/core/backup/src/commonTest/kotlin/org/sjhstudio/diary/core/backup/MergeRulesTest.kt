package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.database.EntryEntity

class MergeRulesTest {
    private fun entry(updated: Long, body: String = "글") = MergeEntry(
        EntryEntity("a", "2026-10-31", "21:40", "<p>$body</p>", body, null, null, null, false, 1, updated, null, null, null, null),
        emptyList()
    )

    @Test
    fun case1_new_entry_is_added() = assertEquals(MergeAction.ADD, MergeRules.decide(entry(5), null, null))

    @Test
    fun case2_same_time_and_content_stays() = assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5), entry(5), 5))

    @Test
    fun case3_backup_not_newer_than_base_keeps_local() = assertEquals(MergeAction.KEEP, MergeRules.decide(entry(4), entry(9), 5))

    @Test
    fun case4_only_backup_changed_replaces() = assertEquals(MergeAction.REPLACE, MergeRules.decide(entry(9, "새"), entry(5), 5))

    @Test
    fun case5_both_changed_keeps_both() = assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(9, "백업"), entry(8, "기기"), 5))

    @Test
    fun case6_no_base_later_one_wins() {
        assertEquals(MergeAction.REPLACE, MergeRules.decide(entry(9), entry(5), null))
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5), entry(9), null))
    }

    @Test
    fun case7_same_time_different_content_keeps_both() =
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(5, "백업"), entry(5, "기기"), null))
}
