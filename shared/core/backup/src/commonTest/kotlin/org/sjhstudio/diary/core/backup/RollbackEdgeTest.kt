package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.testing.FakeDrive

/** 바꾸기가 실패하거나 시계가 틀어져도 바꾸기 전 기록으로 돌아갈 수 있는지 확인 */
class RollbackEdgeTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var phone: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "rollback-edge-${Random.nextLong()}"
        drive = FakeDrive(fs)
        phone = TestPhone(dir / "phone", drive)
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        fs.deleteRecursively(dir)
    }

    private fun backup(vararg ids: String) = ids.map { MergeEntry(TestPhone.entry(it), emptyList()) }

    private val rollbackDir get() = phone.work / "rollback"

    private fun copyFolder() = fs.list(rollbackDir).single()

    private suspend fun replaced(vararg ids: String) {
        phone.write("before")
        phone.photo("before", 0, "바꾸기 전 사진")
        phone.manager().replace(backup(*ids), "drive")
    }

    @Test
    fun a_second_replace_that_cannot_copy_keeps_the_earlier_rollback() = runTest {
        replaced("first")
        phone.clock.advance(1.days)
        val jammed = JammedFiles(fs) { it.name == "settings.json" }

        assertFails { phone.manager(jammed).replace(backup("second"), "drive") }

        assertEquals(setOf("first"), phone.ids())
        assertTrue(phone.manager().rollBack())
        assertEquals(setOf("before"), phone.ids())
    }

    @Test
    fun a_replace_that_fails_while_writing_changes_nothing_and_keeps_the_earlier_rollback() = runTest {
        replaced("first")
        phone.clock.advance(1.days)
        val broken = listOf(
            MergeEntry(TestPhone.entry("x", legacyId = 1), emptyList()),
            MergeEntry(TestPhone.entry("y", legacyId = 1), emptyList())
        )

        assertFails { phone.manager().replace(broken, "drive") }

        assertEquals(setOf("first"), phone.ids())
        assertTrue(phone.manager().rollBack())
        assertEquals(setOf("before"), phone.ids())
    }

    @Test
    fun a_failed_first_replace_leaves_no_copy_of_the_diary() = runTest {
        phone.write("before")
        val broken = listOf(
            MergeEntry(TestPhone.entry("x", legacyId = 1), emptyList()),
            MergeEntry(TestPhone.entry("y", legacyId = 1), emptyList())
        )

        assertFails { phone.manager().replace(broken, "drive") }

        assertEquals(setOf("before"), phone.ids())
        assertNull(phone.store.current().rollbackUntil)
        assertEquals(emptyList(), fs.listOrNull(rollbackDir).orEmpty())
    }

    @Test
    fun a_finished_second_replace_keeps_only_the_newest_copy() = runTest {
        replaced("first")
        phone.clock.advance(1.days)

        phone.manager().replace(backup("second"), "drive")

        assertEquals(1, fs.list(rollbackDir).size)
        assertTrue(phone.manager().rollBack())
        assertEquals(setOf("first"), phone.ids())
    }

    @Test
    fun room_for_the_copy_is_checked_to_the_byte() = runTest {
        phone.write("before")
        val size = fs.metadata(phone.root / "diary.db").size ?: 0
        assertTrue(size > 0)

        phone.freeBytes = 1
        assertFailsWith<NoRoomForRollback> { phone.manager().replace(backup("new"), "drive") }
        assertEquals(setOf("before"), phone.ids())

        phone.freeBytes = Long.MAX_VALUE
        phone.manager().replace(backup("new"), "drive")
        assertEquals(setOf("new"), phone.ids())
    }

    @Test
    fun an_entry_written_with_the_clock_set_back_survives_the_rollback() = runTest {
        replaced("new")
        val replacedAt = phone.clock.now()
        // 바꾼 뒤에 기기 시계가 뒤로 감
        phone.clock.setWallClock(replacedAt - 2.days)
        phone.write("after", updated = phone.clock.now().toEpochMilliseconds())

        assertTrue(phone.manager().rollBack())

        assertEquals(setOf("before", "after"), phone.ids())
    }

    @Test
    fun an_entry_edited_with_the_clock_set_back_survives_the_rollback() = runTest {
        replaced("new")
        phone.db.entryDao().update(TestPhone.entry("new", updated = 11, body = "바꾼 뒤에 고침"))

        assertTrue(phone.manager().rollBack())

        assertEquals(setOf("before", "new"), phone.ids())
        assertEquals("바꾼 뒤에 고침", phone.entries().first { it.id == "new" }.plainText)
    }

    @Test
    fun an_entry_from_the_backup_left_as_it_was_does_not_stay_after_the_rollback() = runTest {
        replaced("new")

        assertTrue(phone.manager().rollBack())

        assertEquals(setOf("before"), phone.ids())
    }

    @Test
    fun an_entry_edited_on_both_sides_of_the_replace_keeps_both() = runTest {
        phone.write("same", updated = 10, body = "바꾸기 전")
        phone.db.backupDao().setBase(listOf(org.sjhstudio.diary.core.database.SyncBaseEntity("same", 5)))
        phone.manager().replace(listOf(MergeEntry(TestPhone.entry("same", updated = 7, body = "백업"), emptyList())), "drive")
        val after = phone.clock.now().toEpochMilliseconds() + 1
        phone.clock.advance(1.days)
        phone.db.entryDao().update(TestPhone.entry("same", updated = after, body = "바꾼 뒤"))

        assertTrue(phone.manager().rollBack())

        assertEquals(setOf("바꾸기 전", "바꾼 뒤"), phone.entries().map { it.plainText }.toSet())
    }

    @Test
    fun an_old_number_used_on_both_sides_does_not_stop_the_rollback() = runTest {
        phone.write("before", legacyId = 7)
        phone.manager().replace(backup("new"), "drive")
        phone.clock.advance(1.days)
        phone.write("after", updated = phone.clock.now().toEpochMilliseconds(), legacyId = 7)

        assertTrue(phone.manager().rollBack())

        assertEquals(setOf("before", "after"), phone.ids())
    }

    @Test
    fun a_replace_with_an_empty_backup_can_be_undone() = runTest {
        phone.write("before")
        phone.photo("before", 0, "사진")

        phone.manager().replace(emptyList(), "drive")
        assertTrue(phone.ids().isEmpty())

        assertTrue(phone.manager().rollBack())
        assertEquals(setOf("before"), phone.ids())
        assertEquals("normal", phone.db.backupDao().allMedia().single().status)
    }

    @Test
    fun photos_of_the_old_entries_stay_on_disk_and_their_rows_return_in_order() = runTest {
        phone.write("before")
        val first = phone.photo("before", 0, "첫째")
        val second = phone.photo("before", 1, "둘째")
        phone.manager().replace(backup("new"), "drive")
        assertTrue(fs.exists(phone.media / "$first.jpg"))

        phone.manager().rollBack()

        assertEquals(listOf(first, second), phone.db.mediaDao().forEntry("before").map { it.sha256 })
    }

    @Test
    fun trash_comes_back_with_the_rollback() = runTest {
        val deleted = phone.clock.now().toEpochMilliseconds()
        phone.write("thrown", deletedAt = deleted)
        phone.manager().replace(backup("new"), "drive")

        phone.manager().rollBack()

        assertEquals(deleted, phone.entries().single().deletedAt)
    }

    @Test
    fun a_draft_survives_replace_and_rollback() = runTest {
        phone.write("before")
        phone.db.draftDao().upsert(DraftEntity("new", "2026-11-05", "09:00", "<p>쓰다 만 글</p>", null, null, null, false, false, 1))

        phone.manager().replace(backup("new"), "drive")
        assertNotNull(phone.db.draftDao().get("new"))

        phone.manager().rollBack()
        assertNotNull(phone.db.draftDao().get("new"))
    }

    @Test
    fun rolling_back_twice_does_nothing_more() = runTest {
        replaced("new")
        assertTrue(phone.manager().rollBack())
        phone.write("later")

        assertFalse(phone.manager().rollBack())

        assertEquals(setOf("before", "later"), phone.ids())
    }

    @Test
    fun a_damaged_copy_changes_nothing() = runTest {
        replaced("new")
        fs.write(copyFolder() / "diary.db") { writeUtf8("깨진 파일") }

        assertFails { phone.manager().rollBack() }

        assertEquals(setOf("new"), phone.ids())
    }

    @Test
    fun a_lost_copy_says_it_cannot_and_changes_nothing() = runTest {
        replaced("new")
        fs.delete(copyFolder() / "diary.db")

        assertFalse(phone.manager().rollBack())

        assertEquals(setOf("new"), phone.ids())
        assertNull(phone.manager().info.first())
    }

    @Test
    fun the_last_millisecond_still_counts_and_the_seven_day_mark_does_not() = runTest {
        replaced("new")
        val until = assertNotNull(phone.store.current().rollbackUntil)

        phone.clock.setWallClock(until - 1.milliseconds)
        assertNotNull(phone.manager().info.first())
        phone.manager().expire()
        assertNotNull(phone.store.current().rollbackUntil)
        assertTrue(fs.exists(copyFolder() / "diary.db"))

        phone.clock.setWallClock(until)
        assertNull(phone.manager().info.first())
        phone.manager().expire()
        assertNull(phone.store.current().rollbackUntil)
        assertFalse(fs.exists(rollbackDir))
        assertEquals(setOf("new"), phone.ids())
    }

    @Test
    fun expire_with_nothing_to_expire_does_nothing() = runTest {
        phone.write("here")

        phone.manager().expire()

        assertEquals(setOf("here"), phone.ids())
    }

    @Test
    fun a_failed_restore_of_settings_never_loses_the_rollback_diary() = runTest {
        replaced("new")
        fs.write(copyFolder() / "settings.json") { writeUtf8("깨진 설정") }

        assertFails { phone.manager().rollBack() }

        assertEquals(setOf("before"), phone.ids())
    }

    @Test
    fun replace_through_the_screen_then_undo_gives_back_everything() = runTest {
        val other = TestPhone(dir / "other", drive, name = "Other")
        try {
            other.write("from-backup")
            other.photo("from-backup", 0, "백업 사진")
            assertIs<BackupOutcome.Done>(other.runner().run())
            phone.write("before")
            val hash = phone.photo("before", 0, "내 사진")
            val controller = phone.controller(backgroundScope)
            assertIs<RestorePreparation.Ready>(controller.prepare(controller.backups()!!.single().snapshotName))

            controller.replace()
            val done = assertIs<RestoreState.Done>(controller.finished())

            assertTrue(done.result.replaced)
            assertEquals(setOf("from-backup"), phone.ids())
            assertEquals("normal", phone.db.backupDao().allMedia().single().status)

            controller.dismiss()
            assertTrue(controller.rollBack())

            assertEquals(setOf("before"), phone.ids())
            assertEquals(hash, phone.db.backupDao().allMedia().single().sha256)
            assertEquals("before", phone.appliedSettings.last().font)
        } finally {
            other.close()
        }
    }

    @Test
    fun a_cut_while_fetching_after_a_replace_still_leaves_the_rollback() = runTest {
        val other = TestPhone(dir / "other", drive, name = "Other")
        try {
            other.write("from-backup")
            other.photo("from-backup", 0, "백업 사진")
            assertIs<BackupOutcome.Done>(other.runner().run())
            phone.write("before")
            val cut: DriveApi = CutDrive(drive, after = 0)
            val controller = phone.controller(backgroundScope, photos = cut)
            controller.prepare(controller.backups()!!.single().snapshotName)

            controller.replace()
            val done = assertIs<RestoreState.Done>(controller.finished())

            assertEquals(1, done.result.photosPending)
            assertNotNull(phone.manager().info.first())
            controller.dismiss()
            assertTrue(controller.rollBack())
            assertEquals(setOf("before"), phone.ids())
        } finally {
            other.close()
        }
    }
}
