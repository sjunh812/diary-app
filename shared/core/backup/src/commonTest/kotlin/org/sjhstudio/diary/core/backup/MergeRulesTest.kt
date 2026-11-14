package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity

class MergeRulesTest {
    private fun entry(updated: Long, body: String = "글", deletedAt: Long? = null, media: List<MediaEntity> = emptyList()) = MergeEntry(
        EntryEntity("a", "2026-10-31", "21:40", "<p>$body</p>", body, null, null, null, false, 1, updated, deletedAt, null, null, null),
        media
    )

    private fun photo(order: Int, hash: String?, status: String = "normal", legacy: String? = null) =
        MediaEntity("m$order", "a", order, status, hash?.let { "$it.jpg" }, hash, 1, 1, 1, legacy, 1)

    @Test
    fun case1_new_entry_is_added() = assertEquals(MergeAction.ADD, MergeRules.decide(entry(5), null, null))

    @Test
    fun case2_same_time_and_content_stays() = assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5), entry(5), 5))

    @Test
    fun case3_backup_not_newer_than_base_keeps_local() = assertEquals(MergeAction.KEEP, MergeRules.decide(entry(4), entry(9), 5))

    @Test
    fun case4_only_backup_changed_replaces() = assertEquals(MergeAction.REPLACE, MergeRules.decide(entry(9, "새"), entry(5), 5))

    /** 폰이 고쳐 백업한 뒤, 그 수정을 받기 전 판에서 고친 태블릿 백업을 합쳐도 폰 수정이 덮이지 않음 */
    @Test
    fun case4_from_another_device_keeps_both() =
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(9, "태블릿"), entry(5, "폰"), 5, fromOtherDevice = true))

    @Test
    fun case4_from_another_device_with_the_same_content_stays() =
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(9), entry(5), 5, fromOtherDevice = true))

    @Test
    fun case5_both_changed_keeps_both() = assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(9, "백업"), entry(8, "기기"), 5))

    @Test
    fun case6_no_base_from_other_device_keeps_both() {
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(9, "백업"), entry(5, "기기"), null, fromOtherDevice = true))
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(5, "백업"), entry(9, "기기"), null, fromOtherDevice = true))
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(9), entry(5), null, fromOtherDevice = true))
    }

    @Test
    fun case6_no_base_later_one_wins() {
        assertEquals(MergeAction.REPLACE, MergeRules.decide(entry(9), entry(5), null))
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5), entry(9), null))
    }

    @Test
    fun case7_same_time_different_content_keeps_both() =
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(5, "백업"), entry(5, "기기"), null))

    @Test
    fun backup_already_seen_keeps_local_even_when_local_changed() =
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5, "백업"), entry(9, "기기"), 5))

    @Test
    fun an_edit_made_with_the_clock_set_back_is_not_overwritten() =
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(9, "백업"), entry(3, "기기"), 5))

    @Test
    fun the_same_change_on_both_sides_is_one_entry() = assertEquals(MergeAction.KEEP, MergeRules.decide(entry(9), entry(8), 5))

    @Test
    fun both_in_the_trash_with_different_dates_are_the_same_entry() =
        assertEquals(MergeAction.KEEP, MergeRules.decide(entry(5, deletedAt = 100), entry(5, deletedAt = 200), 5))

    @Test
    fun in_the_trash_on_one_side_only_is_a_difference() =
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(entry(5, deletedAt = 100), entry(5), null))

    @Test
    fun moved_photos_are_a_difference() {
        val backup = entry(5, media = listOf(photo(0, "a"), photo(1, "b")))
        val local = entry(5, media = listOf(photo(0, "b"), photo(1, "a")))

        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(backup, local, null))
    }

    @Test
    fun a_photo_still_on_its_way_is_the_same_photo() {
        val backup = entry(5, media = listOf(photo(0, "a")))
        val local = entry(5, media = listOf(photo(0, "a", status = "pending")))

        assertEquals(MergeAction.KEEP, MergeRules.decide(backup, local, 5))
    }

    @Test
    fun a_lost_photo_without_a_hash_is_compared_by_its_old_path() {
        val backup = entry(5, media = listOf(photo(0, null, "missing", "/a.jpg")))

        assertEquals(MergeAction.KEEP, MergeRules.decide(backup, entry(5, media = listOf(photo(0, null, "missing", "/a.jpg"))), 5))
        assertEquals(MergeAction.KEEP_BOTH, MergeRules.decide(backup, entry(5, media = listOf(photo(0, null, "missing", "/b.jpg"))), 5))
    }
}
