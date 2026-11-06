package org.sjhstudio.diary.core.backup

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.backup.TestPhone.Companion.hashOf
import org.sjhstudio.diary.core.backup.TestPhone.Companion.photoRow
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * 끊김, 깨짐, 두 번 복원처럼 일이 꼬였을 때 기기 기록이 그대로인지 본다.
 * 기기 둘이 드라이브 대역을 같이 쓴다.
 */
class RestoreEdgeTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-edge-${Random.nextLong()}"
        drive = FakeDrive(fs)
        a = TestPhone(dir / "a", drive, name = "A")
        b = TestPhone(dir / "b", drive, name = "B")
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    private suspend fun TestPhone.backUp() {
        clock.advance(1.days)
        assertIs<BackupOutcome.Done>(runner().run())
    }

    /**
     * 일기 셋에 사진 한 장씩.
     * 확인값을 돌려준다.
     */
    private suspend fun threeWithPhotos(): List<String> {
        listOf("one", "two", "three").forEach { a.write(it) }
        return listOf(a.photo("one", 0, "첫째 사진"), a.photo("two", 0, "둘째 사진"), a.photo("three", 0, "셋째 사진"))
    }

    private fun latest(list: List<RestoreCandidate>) = list.first { it.entry.state == BackupEntryState.Ready }

    /** 합치고 사진까지 받는다. */
    private suspend fun TestPhone.restoreLatest(api: DriveApi = drive): MergeCounts {
        val prepared = planner().prepare(latest(catalog().list()))
        val counts = applier().apply(prepared.plan, "drive")
        fetcher(api).fetch()
        return counts
    }

    private suspend fun RestoreController.ready(name: String? = null): RestorePreparation.Ready {
        val picked = name ?: backups()!!.first().snapshotName
        return assertIs<RestorePreparation.Ready>(prepare(picked))
    }

    private suspend fun RestoreController.mergedResult() = run {
        merge()
        assertIs<RestoreState.Done>(finished()).result
    }

    // 복원 중 끊김

    @Test
    fun a_cut_during_photos_then_the_same_restore_again_adds_nothing_and_finishes() = runTest {
        val hashes = threeWithPhotos()
        a.backUp()

        val first = b.controller(backgroundScope, photos = CutDrive(drive, after = 1))
        first.ready()
        val cut = first.mergedResult()

        assertEquals(3, b.ids().size)
        assertTrue(cut.photosPending in 1..3, "받지 못한 사진 ${cut.photosPending}장")

        val again = b.controller(backgroundScope)
        val preview = again.ready().preview
        assertEquals(0, preview.entriesAdded)
        assertEquals(0, preview.entriesKeptBoth)
        assertEquals(3, preview.entriesUnchanged)
        assertEquals(cut.photosPending, preview.photosToDownload)
        again.mergedResult()

        assertEquals(setOf("one", "two", "three"), b.ids())
        assertEquals(setOf("normal"), b.photoStates().values.toSet())
        hashes.forEach { assertEquals(it, fs.hashes(b.media / "$it.jpg").sha256) }
        assertTrue(b.partsLeft().isEmpty())
    }

    @Test
    fun opening_the_app_after_a_cut_gets_only_what_is_left() = runTest {
        threeWithPhotos()
        a.backUp()
        b.store.update { it.copy(accountKey = "sub-1") }
        val first = b.controller(backgroundScope, photos = CutDrive(drive, after = 1))
        first.ready()
        val cut = first.mergedResult()
        val left = cut.photosPending

        val counting = CountingDrive(drive)
        b.controller(backgroundScope, photos = counting).resumePhotos()

        assertEquals(left, counting.downloads)
        assertEquals(setOf("normal"), b.photoStates().values.toSet())
    }

    @Test
    fun a_cut_while_downloading_the_snapshot_touches_nothing() = runTest {
        threeWithPhotos()
        a.backUp()
        b.write("mine")
        val cut = b.controller(backgroundScope, api = CutOnlySnapshot(drive))

        assertIs<RestorePreparation.Failed>(cut.prepare(cut.backups()!!.first().snapshotName))

        assertEquals(setOf("mine"), b.ids())
        assertTrue(fs.listOrNull(b.work / "incoming").orEmpty().isEmpty())
    }

    @Test
    fun pressing_merge_twice_merges_once() = runTest {
        threeWithPhotos()
        a.backUp()
        val controller = b.controller(backgroundScope)
        controller.ready()

        controller.merge()
        controller.merge()
        assertIs<RestoreState.Done>(controller.finished())

        assertEquals(3, b.ids().size)
    }

    @Test
    fun a_failed_settings_write_does_not_hide_a_finished_merge() = runTest {
        threeWithPhotos()
        a.backUp()
        b.settingsFailure = IllegalStateException("저장 공간이 없다")
        val controller = b.controller(backgroundScope)
        controller.ready()

        val result = controller.mergedResult()

        assertEquals(3, result.entriesAdded)
        assertEquals(3, result.photosRestored)
        assertEquals(setOf("normal"), b.photoStates().values.toSet())
    }

    @Test
    fun a_merge_that_cannot_finish_leaves_everything_and_can_be_tried_again() = runTest {
        threeWithPhotos()
        a.backUp()
        b.write("mine")
        // 백업의 사진 줄과 같은 번호의 줄이 기기에 있어 넣다가 실패한다.
        b.db.mediaDao().insertAll(listOf(photoRow("mine", 0, "내 사진").copy(id = "one-0")))
        val controller = b.controller(backgroundScope)
        controller.ready()

        controller.merge()
        assertEquals(RestoreFailure.OTHER, assertIs<RestoreState.Failed>(controller.finished()).reason)

        assertEquals(setOf("mine"), b.ids())
        assertEquals(setOf("one-0"), b.photoStates().keys)
    }

    // 양쪽 수정

    @Test
    fun both_sides_edited_then_restored_twice_keeps_two_versions_only() = runTest {
        a.write("diary", updated = 10, body = "처음")
        a.backUp()
        b.restoreLatest()
        a.db.entryDao().update(TestPhone.entry("diary", updated = 30, body = "A가 고침"))
        a.backUp()
        b.db.entryDao().update(TestPhone.entry("diary", updated = 20, body = "B가 고침"))

        assertEquals(1, b.restoreLatest().keptBoth)
        assertEquals(0, b.restoreLatest().keptBoth)
        assertEquals(0, b.restoreLatest().keptBoth)

        assertEquals(setOf("A가 고침", "B가 고침"), b.entries().map { it.plainText }.toSet())
        assertEquals(2, b.entries().size)
    }

    @Test
    fun the_kept_copy_is_still_kept_when_a_newer_backup_changes_again() = runTest {
        a.write("diary", updated = 10, body = "처음")
        a.backUp()
        b.restoreLatest()
        a.db.entryDao().update(TestPhone.entry("diary", updated = 30, body = "A 둘째"))
        a.backUp()
        b.db.entryDao().update(TestPhone.entry("diary", updated = 20, body = "B 고침"))
        b.restoreLatest()
        a.db.entryDao().update(TestPhone.entry("diary", updated = 40, body = "A 셋째"))
        a.backUp()

        b.restoreLatest()

        assertEquals(setOf("A 둘째", "A 셋째", "B 고침"), b.entries().map { it.plainText }.toSet())
    }

    // 휴지통과 영구 삭제

    @Test
    fun an_entry_trashed_here_after_the_backup_stays_in_the_trash() = runTest {
        a.write("diary")
        a.backUp()
        b.restoreLatest()
        b.db.entryDao().setDeletedAt("diary", 500, 500)

        val counts = b.restoreLatest()

        assertEquals(0, counts.added + counts.updated + counts.keptBoth)
        assertEquals(500, b.entries().single().deletedAt)
    }

    @Test
    fun an_entry_trashed_in_the_backup_moves_to_the_trash_here_with_its_photo_row() = runTest {
        val now = a.clock.now().toEpochMilliseconds()
        a.write("diary")
        val hash = a.photo("diary", 0, "사진")
        a.backUp()
        b.restoreLatest()
        a.db.entryDao().setDeletedAt("diary", now, now)
        a.backUp()

        b.restoreLatest()

        val entry = b.entries().single()
        assertEquals(now, entry.deletedAt)
        assertEquals(hash, b.db.backupDao().allMedia().single().sha256)
        assertTrue(fs.exists(b.media / "$hash.jpg"))
    }

    @Test
    fun a_trashed_entry_comes_back_with_its_days_left() = runTest {
        val deleted = (a.clock.now() - 10.days).toEpochMilliseconds()
        a.write("thrown", deletedAt = deleted)
        a.write("kept")
        a.backUp()

        b.restoreLatest()

        assertEquals(deleted, b.entries().first { it.id == "thrown" }.deletedAt)
    }

    @Test
    fun a_trashed_entry_about_to_expire_is_not_doubled_by_a_second_restore() = runTest {
        a.write("thrown", deletedAt = (a.clock.now() - 40.days).toEpochMilliseconds())
        a.write("kept")
        a.backUp()

        b.restoreLatest()
        val first = b.entries().first { it.id == "thrown" }.deletedAt
        val second = b.restoreLatest()

        assertEquals(0, second.keptBoth)
        assertEquals(2, b.entries().size)
        assertEquals(first, b.entries().first { it.id == "thrown" }.deletedAt)
    }

    @Test
    fun a_permanently_deleted_entry_comes_back_and_the_preview_says_so() = runTest {
        a.write("gone")
        a.write("kept")
        a.backUp()
        b.restoreLatest()
        b.db.entryDao().deletePermanently("gone")

        val prepared = b.planner().prepare(latest(b.catalog().list()))

        assertEquals(1, prepared.preview.entriesAdded)
        b.applier().apply(prepared.plan, "drive")
        assertEquals(setOf("gone", "kept"), b.ids())
    }

    @Test
    fun a_permanently_deleted_entry_brings_its_photo_back_from_the_drive() = runTest {
        a.write("gone")
        val hash = a.photo("gone", 0, "사진")
        a.backUp()
        b.restoreLatest()
        b.db.entryDao().deletePermanently("gone")
        fs.delete(b.media / "$hash.jpg")

        b.restoreLatest()

        assertEquals("normal", b.db.backupDao().allMedia().single().status)
        assertEquals(hash, fs.hashes(b.media / "$hash.jpg").sha256)
    }

    // 같은 사진을 여러 일기가 쓸 때

    @Test
    fun a_photo_shared_by_two_entries_is_on_drive_once_and_comes_back_for_both() = runTest {
        a.write("first")
        a.write("second")
        val hash = a.photo("first", 0, "같은 사진")
        a.db.mediaDao().insertAll(listOf(photoRow("second", 0, "같은 사진")))
        a.backUp()

        b.restoreLatest()

        assertEquals(1, a.driveFiles(DriveLayout.KIND_PHOTO).size)
        assertEquals(mapOf("first-0" to "normal", "second-0" to "normal"), b.photoStates())
        assertEquals(hash, fs.hashes(b.media / "$hash.jpg").sha256)
    }

    @Test
    fun the_same_photo_twice_in_one_entry_keeps_both_places() = runTest {
        a.write("diary")
        a.photo("diary", 0, "같은 사진")
        a.db.mediaDao().insertAll(listOf(photoRow("diary", 1, "같은 사진")))
        a.photo("diary", 2, "다른 사진")
        a.backUp()

        b.restoreLatest()

        val rows = b.db.mediaDao().forEntry("diary")
        assertEquals(listOf(hashOf("같은 사진"), hashOf("같은 사진"), hashOf("다른 사진")), rows.map { it.sha256 })
        assertEquals(setOf("normal"), rows.map { it.status }.toSet())
    }

    @Test
    fun deleting_one_of_two_entries_then_restoring_needs_no_download() = runTest {
        a.write("first")
        a.write("second")
        val hash = a.photo("first", 0, "같은 사진")
        a.db.mediaDao().insertAll(listOf(photoRow("second", 0, "같은 사진")))
        a.backUp()
        b.restoreLatest()
        b.db.entryDao().deletePermanently("second")

        val counting = CountingDrive(drive)
        val prepared = b.planner().prepare(latest(b.catalog().list()))
        assertEquals(0, prepared.preview.photosToDownload)
        b.applier().apply(prepared.plan, "drive")
        b.fetcher(counting).fetch()

        assertEquals(0, counting.downloads)
        assertEquals(mapOf("first-0" to "normal", "second-0" to "normal"), b.photoStates())
        assertEquals(hash, fs.hashes(b.media / "$hash.jpg").sha256)
    }

    @Test
    fun replacing_one_entry_does_not_touch_another_that_shares_its_photo() = runTest {
        a.write("first", updated = 10)
        a.write("second", updated = 10)
        a.photo("first", 0, "같은 사진")
        a.db.mediaDao().insertAll(listOf(photoRow("second", 0, "같은 사진")))
        a.backUp()
        b.restoreLatest()
        a.db.mediaDao().deleteForEntry("first")
        a.db.entryDao().update(TestPhone.entry("first", updated = 20, body = "사진을 뺌"))
        a.backUp()

        b.restoreLatest()

        assertEquals(mapOf("second-0" to "normal"), b.photoStates())
        assertTrue(fs.exists(b.media / "${hashOf("같은 사진")}.jpg"))
    }

    // 깨진 사진

    @Test
    fun wrong_empty_and_lost_photos_fail_alone_and_the_rest_arrive() = runTest {
        listOf("wrong", "empty", "lost", "good").forEach { a.write(it) }
        val wrong = a.photo("wrong", 0, "내용이 바뀔 사진")
        val empty = a.photo("empty", 0, "비게 될 사진")
        val lost = a.photo("lost", 0, "없어질 사진")
        val good = a.photo("good", 0, "멀쩡한 사진")
        a.backUp()
        drive.deleteByUser(fileOf(lost).id)
        val damaged = DamagedDrive(drive, fs, mapOf(fileOf(wrong).id to "다른 내용".encodeToByteArray(), fileOf(empty).id to ByteArray(0)))
        val controller = b.controller(backgroundScope, photos = damaged)
        controller.ready()

        val result = controller.mergedResult()

        assertEquals(4, b.ids().size)
        assertEquals(1, result.photosRestored)
        assertEquals(2, result.photosMissing)
        assertEquals(0, result.photosPending, "드라이브에 없는 사진은 기다려도 오지 않는다")
        assertEquals(1, result.photosOnOriginal)
        assertEquals(
            mapOf("wrong-0" to "missing", "empty-0" to "missing", "lost-0" to "pending", "good-0" to "normal"),
            b.photoStates()
        )
        assertFalse(fs.exists(b.media / "$wrong.jpg"), "깨진 사진을 사진 폴더에 두었다")
        assertFalse(fs.exists(b.media / "$empty.jpg"), "빈 사진을 사진 폴더에 두었다")
        assertEquals(good, fs.hashes(b.media / "$good.jpg").sha256)
        assertTrue(b.partsLeft().isEmpty())
    }

    @Test
    fun a_full_disk_while_saving_photos_keeps_the_entries_and_tries_again() = runTest {
        threeWithPhotos()
        a.backUp()
        val full = object : okio.ForwardingFileSystem(fs) {
            override fun atomicMove(source: Path, target: Path) = throw IOException("저장 공간이 없다")
        }
        val controller = b.controller(backgroundScope, files = full)
        controller.ready()

        val result = controller.mergedResult()

        assertEquals(3, b.ids().size)
        assertEquals(3, result.photosPending)
        assertTrue(fs.list(b.media).isEmpty())
        assertTrue(b.partsLeft().isEmpty())
        b.fetcher().fetch()
        assertEquals(setOf("normal"), b.photoStates().values.toSet())
    }

    @Test
    fun a_full_disk_while_downloading_the_snapshot_keeps_the_device_as_it_was() = runTest {
        threeWithPhotos()
        a.backUp()
        b.write("mine")
        val full = object : DriveApi by drive {
            override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
                if (to.name.endsWith(".db")) throw IOException("저장 공간이 없다")
                return drive.download(fileId, to, fromByte)
            }
        }
        val controller = b.controller(backgroundScope, api = full)

        assertIs<RestorePreparation.Failed>(controller.prepare(controller.backups()!!.first().snapshotName))

        assertEquals(setOf("mine"), b.ids())
    }

    @Test
    fun the_preview_shows_how_much_room_there_is_before_anything_changes() = runTest {
        a.write("diary")
        a.photo("diary", 0, "사진 내용")
        a.backUp()
        b.freeBytes = 1

        val preview = b.controller(backgroundScope).ready().preview

        assertEquals(1, preview.deviceBytesFree)
        assertTrue(preview.bytesToDownload > preview.deviceBytesFree)
        assertTrue(b.ids().isEmpty())
    }

    // 복원과 쓰기가 겹칠 때

    @Test
    fun an_entry_written_after_the_preview_survives_the_merge() = runTest {
        a.write("diary")
        a.backUp()
        val controller = b.controller(backgroundScope)
        controller.ready()
        b.write("mine")

        controller.mergedResult()

        assertEquals(setOf("diary", "mine"), b.ids())
    }

    @Test
    fun an_edit_after_the_preview_is_kept_instead_of_overwritten() = runTest {
        a.write("diary", updated = 10, body = "처음")
        a.backUp()
        b.restoreLatest()
        a.db.entryDao().update(TestPhone.entry("diary", updated = 30, body = "A가 고침"))
        a.backUp()
        val controller = b.controller(backgroundScope)
        assertEquals(1, controller.ready().preview.entriesUpdated)
        b.db.entryDao().update(TestPhone.entry("diary", updated = 40, body = "미리보기 뒤에 고침"))

        controller.mergedResult()

        assertEquals(setOf("A가 고침", "미리보기 뒤에 고침"), b.entries().map { it.plainText }.toSet())
    }

    @Test
    fun an_entry_written_while_photos_arrive_survives() = runTest {
        threeWithPhotos()
        a.backUp()
        var written = false
        // 사진은 동시에 받으니 한 번만 쓰도록 잠근다.
        val lock = Mutex()
        val writing = object : DriveApi by drive {
            override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
                lock.withLock {
                    if (!written) {
                        written = true
                        b.write("mine")
                        b.db.entryDao().update(TestPhone.entry("one", updated = 99, body = "받는 중에 고침"))
                    }
                }
                return drive.download(fileId, to, fromByte)
            }
        }
        val controller = b.controller(backgroundScope, photos = writing)
        controller.ready()

        controller.mergedResult()

        assertTrue(written)
        assertEquals(setOf("one", "two", "three", "mine"), b.ids())
        assertEquals("받는 중에 고침", b.entries().first { it.id == "one" }.plainText)
    }

    @Test
    fun undo_waits_while_a_restore_is_running() = runTest {
        a.write("diary")
        a.backUp()
        b.write("before")
        b.manager().replace(listOf(MergeEntry(TestPhone.entry("new"), emptyList())), "drive")
        val controller = b.controller(backgroundScope)
        assertTrue(b.gate.tryEnter())

        assertFalse(controller.rollBack())

        assertEquals(setOf("new"), b.ids())
        b.gate.leave()
        assertTrue(controller.rollBack())
    }

    @Test
    fun photos_do_not_resume_on_mobile_data_or_without_an_account() = runTest {
        threeWithPhotos()
        a.backUp()
        val prepared = b.planner().prepare(latest(b.catalog().list()))
        b.applier().apply(prepared.plan, "drive")

        b.controller(backgroundScope).resumePhotos()
        assertEquals(setOf("pending"), b.photoStates().values.toSet())

        b.store.update { it.copy(accountKey = "sub-1") }
        b.network.isUnmetered = false
        b.controller(backgroundScope).resumePhotos()
        assertEquals(setOf("pending"), b.photoStates().values.toSet())

        b.network.isUnmetered = true
        b.controller(backgroundScope).resumePhotos()
        assertEquals(setOf("normal"), b.photoStates().values.toSet())
    }

    @Test
    fun an_old_backup_cannot_be_used_to_replace_the_device() = runTest {
        b.write("mine")
        val path = dir / "note.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.prepare("CREATE TABLE Note (_id INTEGER PRIMARY KEY, contents TEXT, create_date TEXT, modify_date TEXT)").use {
            it.step()
        }
        connection.close()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.prepareFile(fs.source(path)))

        controller.replace()

        assertIs<RestoreState.Failed>(controller.finished())
        assertEquals(setOf("mine"), b.ids())
    }

    @Test
    fun a_bundle_can_replace_the_device_and_be_undone() = runTest {
        a.write("from-file")
        val hash = a.photo("from-file", 0, "사진")
        val zip = dir / "export.zip"
        a.exportTo(zip)
        b.write("before")
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.prepareFile(fs.source(zip)))

        controller.replace()
        val done = assertIs<RestoreState.Done>(controller.finished()).result

        assertTrue(done.replaced)
        assertEquals(setOf("from-file"), b.ids())
        assertEquals(hash, fs.hashes(b.media / "$hash.jpg").sha256)
        assertTrue(controller.rollBack())
        assertEquals(setOf("before"), b.ids())
    }

    @Test
    fun an_entry_with_more_photos_than_the_limit_keeps_them_all_in_order() = runTest {
        a.write("many")
        val hashes = (0 until 35).map { a.photo("many", it, "사진 $it") }
        a.backUp()

        b.restoreLatest()

        val rows = b.db.mediaDao().forEntry("many")
        assertEquals(hashes, rows.map { it.sha256 })
        assertEquals(setOf("normal"), rows.map { it.status }.toSet())
        hashes.forEach { assertEquals(it, fs.hashes(b.media / "$it.jpg").sha256) }
    }

    @Test
    fun a_snapshot_without_its_list_file_is_not_offered() = runTest {
        a.write("diary")
        a.backUp()
        a.plant("unfinished.db", "반쯤 올라간 스냅샷".encodeToByteArray(), a.driveFiles(DriveLayout.KIND_SNAPSHOT).single().props)

        val listed = b.catalog().list()

        assertEquals(1, listed.size)
        assertEquals(BackupEntryState.Ready, listed.single().entry.state)
    }

    // 고를 수 없는 백업

    @Test
    fun a_backup_from_a_newer_app_is_refused_even_when_named_directly() = runTest {
        a.write("diary")
        a.backUp()
        val file = a.driveFiles(DriveLayout.KIND_MANIFEST).single()
        val manifest = assertNotNull(ManifestCodec.decode(drive.bytes(file.id).decodeToString()))
        drive.deleteByUser(file.id)
        a.plant(file.name, ManifestCodec.encode(manifest.copy(formatVersion = 99)).encodeToByteArray(), file.props)
        val controller = b.controller(backgroundScope)

        val name = controller.backups()!!.single().snapshotName
        val failed = assertIs<RestorePreparation.Failed>(controller.prepare(name))

        assertEquals(RestoreFailure.NEEDS_UPDATE, failed.reason)
        assertTrue(b.ids().isEmpty())
    }

    @Test
    fun a_snapshot_that_lies_about_its_entries_is_refused() = runTest {
        a.write("diary")
        a.backUp()
        val listed = b.catalog().list().single()
        val manifest = assertNotNull(listed.manifest)
        val wrong = listed.copy(manifest = manifest.copy(counts = manifest.counts.copy(entries = 2)))

        assertFailsWith<RestoreMismatch> { b.planner().prepare(wrong) }

        assertTrue(b.ids().isEmpty())
        assertTrue(fs.listOrNull(b.work / "incoming").orEmpty().isEmpty())
    }

    @Test
    fun a_file_that_matches_its_hash_but_is_no_database_is_refused() = runTest {
        a.write("diary")
        a.backUp()
        b.write("mine")
        val manifestFile = a.driveFiles(DriveLayout.KIND_MANIFEST).single()
        val snapshotFile = a.driveFiles(DriveLayout.KIND_SNAPSHOT).single()
        val manifest = assertNotNull(ManifestCodec.decode(drive.bytes(manifestFile.id).decodeToString()))
        val fake = "일기 DB가 아니다".encodeToByteArray()
        drive.deleteByUser(manifestFile.id)
        drive.deleteByUser(snapshotFile.id)
        a.plant(snapshotFile.name, fake, snapshotFile.props)
        val hash = hashOf("일기 DB가 아니다")
        a.plant(
            manifestFile.name,
            ManifestCodec.encode(manifest.copy(snapshot = manifest.snapshot.copy(sha256 = hash))).encodeToByteArray(),
            manifestFile.props
        )
        val controller = b.controller(backgroundScope)

        assertIs<RestorePreparation.Failed>(controller.prepare(controller.backups()!!.single().snapshotName))

        assertEquals(setOf("mine"), b.ids())
        assertTrue(fs.listOrNull(b.work / "incoming").orEmpty().isEmpty())
    }

    @Test
    fun a_snapshot_named_with_an_emoji_on_another_device_still_restores() = runTest {
        a.write("diary")
        a.backUp()
        val manifestFile = a.driveFiles(DriveLayout.KIND_MANIFEST).single()
        val snapshotFile = a.driveFiles(DriveLayout.KIND_SNAPSHOT).single()
        val manifest = assertNotNull(ManifestCodec.decode(drive.bytes(manifestFile.id).decodeToString()))
        val named = "민수의 폰 😀.db"
        val copy = drive.bytes(snapshotFile.id)
        drive.deleteByUser(manifestFile.id)
        drive.deleteByUser(snapshotFile.id)
        a.plant(named, copy, snapshotFile.props)
        a.plant(
            manifestFile.name,
            ManifestCodec.encode(manifest.copy(snapshot = manifest.snapshot.copy(fileName = named))).encodeToByteArray(),
            manifestFile.props
        )

        b.restoreLatest()

        assertEquals(setOf("diary"), b.ids())
    }

    // 빈 기기와 빈 백업

    @Test
    fun a_device_with_only_trash_backs_up_and_comes_back() = runTest {
        a.write("thrown", deletedAt = a.clock.now().toEpochMilliseconds())
        a.backUp()

        val listed = b.catalog().list().single()
        assertEquals(0, listed.entry.entryCount)
        b.restoreLatest()

        assertEquals(setOf("thrown"), b.ids())
    }

    @Test
    fun an_empty_drive_lists_nothing() = runTest {
        assertEquals(emptyList(), b.controller(backgroundScope).backups())
    }

    @Test
    fun an_unknown_snapshot_name_fails_quietly() = runTest {
        assertIs<RestorePreparation.Failed>(b.controller(backgroundScope).prepare("없는 이름"))
    }

    @Test
    fun merging_without_a_preview_does_nothing() = runTest {
        val controller = b.controller(backgroundScope)

        controller.merge()
        controller.replace()

        assertEquals(RestoreState.Idle, controller.state.value)
    }

    // 시간

    @Test
    fun a_device_in_another_time_zone_gets_the_same_dates() = runTest {
        a.zone = TimeZone.of("Asia/Seoul")
        b.zone = TimeZone.of("America/Los_Angeles")
        a.write("late", date = "2026-12-31")
        val written = a.entries().single()
        a.backUp()

        assertEquals(1, b.restoreLatest().added)

        assertEquals(written, b.entries().single())
        val again = b.restoreLatest()
        assertEquals(0, again.added + again.updated + again.keptBoth)
    }

    @Test
    fun a_device_with_a_wrong_date_gets_the_same_entries() = runTest {
        val deleted = (a.clock.now() - 3.days).toEpochMilliseconds()
        a.write("late", date = "2026-12-31")
        a.write("thrown", deletedAt = deleted)
        val written = a.entries()
        a.backUp()
        // 기기 날짜가 과거로 틀어져 있다.
        b.clock.setWallClock(Instant.parse("2020-01-01T00:00:00Z"))

        b.restoreLatest()

        assertEquals(written, b.entries())
    }

    @Test
    fun a_backup_made_with_a_clock_far_ahead_still_restores() = runTest {
        a.clock.setWallClock(Instant.parse("2031-03-01T00:00:00Z"))
        a.write("diary")
        assertIs<BackupOutcome.Done>(a.runner().run())

        val listed = b.catalog().list().single()
        assertEquals(BackupEntryState.Ready, listed.entry.state)
        b.restoreLatest()

        assertEquals(setOf("diary"), b.ids())
    }

    // 여러 기기를 거칠 때

    @Test
    fun a_device_still_waiting_for_photos_can_back_up_without_losing_them() = runTest {
        val hashes = threeWithPhotos()
        a.backUp()
        val prepared = b.planner().prepare(latest(b.catalog().list()))
        b.applier().apply(prepared.plan, "drive")
        assertEquals(setOf("pending"), b.photoStates().values.toSet())

        b.backUp()
        val c = TestPhone(dir / "c", drive, name = "C")
        try {
            val newest = c.catalog().list().first { it.entry.deviceName == "B" }
            assertEquals(3, newest.entry.photoCount)
            c.applier().apply(c.planner().prepare(newest).plan, "drive")
            c.fetcher().fetch()

            assertEquals(setOf("normal"), c.photoStates().values.toSet())
            hashes.forEach { assertEquals(it, fs.hashes(c.media / "$it.jpg").sha256) }
        } finally {
            c.close()
        }
    }

    @Test
    fun a_missing_photo_keeps_its_place_and_old_path_across_two_devices() = runTest {
        a.write("diary", legacyId = 7)
        val gone = photoRow(
            "diary",
            0,
            "없는 사진"
        ).copy(status = "missing", fileName = null, sha256 = null, byteSize = null, legacyPath = "/cache/a.jpg")
        a.db.mediaDao().insertAll(listOf(gone))
        a.photo("diary", 1, "있는 사진")
        a.backUp()
        b.restoreLatest()
        b.backUp()
        val c = TestPhone(dir / "c", drive, name = "C")
        try {
            val newest = c.catalog().list().first { it.entry.deviceName == "B" }
            c.applier().apply(c.planner().prepare(newest).plan, "drive")
            c.fetcher().fetch()

            val rows = c.db.mediaDao().forEntry("diary")
            assertEquals(gone, rows[0])
            assertEquals("normal", rows[1].status)
            assertEquals(7, c.entries().single().legacyId)
        } finally {
            c.close()
        }
    }

    @Test
    fun the_kept_copy_travels_to_the_next_device_as_a_plain_entry() = runTest {
        a.write("diary", updated = 10, body = "처음")
        a.backUp()
        b.restoreLatest()
        a.db.entryDao().update(TestPhone.entry("diary", updated = 30, body = "A가 고침"))
        a.backUp()
        b.db.entryDao().update(TestPhone.entry("diary", updated = 20, body = "B가 고침"))
        b.restoreLatest()
        b.backUp()

        val manifest = assertNotNull(b.catalog().list().first { it.entry.deviceName == "B" }.manifest)
        assertEquals(2, manifest.counts.entries)
        assertNotEquals(emptySet(), b.db.backupDao().markedIds().toSet())
        assertNull(b.entries().first { it.id == "diary" }.deletedAt)
    }

    private fun fileOf(hash: String) = drive.visible.first { it.name.startsWith(hash) }
}

/** 받은 사진 수를 센다. */
private class CountingDrive(private val real: DriveApi) : DriveApi by real {
    private val lock = Mutex()
    var downloads = 0
        private set

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        lock.withLock { downloads++ }
        return real.download(fileId, to, fromByte)
    }
}

/** 스냅샷을 받을 때만 끊긴다. */
private class CutOnlySnapshot(private val real: DriveApi) : DriveApi by real {
    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        if (to.name.endsWith(".db")) throw DriveNotFound()
        return real.download(fileId, to, fromByte)
    }
}

/** 이름이 맞는 파일 대신 다른 내용을 주는 드라이브. */
private class DamagedDrive(private val real: DriveApi, private val fs: FileSystem, private val damage: Map<String, ByteArray>) :
    DriveApi by real {
    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        val bytes = damage[fileId] ?: return real.download(fileId, to, fromByte)
        fs.write(to) { write(bytes) }
        return bytes.size.toLong()
    }
}
