package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.coroutines.ContinuationInterceptor
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/**
 * 한 기기에서 백업하고 빈 기기에서 복원한다.
 * 드라이브 대역을 두 기기가 같이 쓴다.
 */
class RestoreRoundTripTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var phoneA: DiaryDatabase
    private lateinit var phoneB: DiaryDatabase
    private lateinit var drive: FakeDrive

    private fun open(name: String): DiaryDatabase {
        fs.createDirectories(dir / name / "media")
        return Room.databaseBuilder<DiaryDatabase>((dir / name / "diary.db").toString()).configured().build()
    }

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-${Random.nextLong()}"
        phoneA = open("a")
        phoneB = open("b")
        drive = FakeDrive(fs)
    }

    @AfterTest
    fun tearDown() {
        phoneA.close()
        phoneB.close()
        fs.deleteRecursively(dir)
    }

    private fun entry(id: String) = EntryEntity(
        id, "2026-10-31", "21:40", "<p>$id</p>", id, "good", null, "망원동", true, 1, 7, null, id.first().code, null, null
    )

    private suspend fun backUp(extraRows: List<MediaEntity> = emptyList()): String {
        val bytes = "사진 내용".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        fs.write(dir / "a" / "media" / "$hash.jpg") { write(bytes) }
        phoneA.entryDao().insertAll(listOf(entry("one"), entry("two")))
        phoneA.mediaDao().insertAll(
            listOf(MediaEntity("m", "one", 0, "normal", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1)) + extraRows
        )
        val runner =
            BackupRunner(
                phoneA, drive, FakeDriveAuthorizer(), BackupStore(InMemoryDataStore(emptyPreferences())), FakeNetworkState(),
                DeviceName {
                    "A"
                },
                fs,
                dir / "a" / "media", dir / "a" / "backup", BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings() }
            )
        assertIs<BackupOutcome.Done>(runner.run())
        return hash
    }

    private fun planner(api: DriveApi = drive) = RestorePlanner(
        api,
        MergeApplier(phoneB, {
            fs.exists(dir / "b" / "media" / it)
        }),
        FreeSpace { 1_000_000 },
        fs,
        dir / "b" / "backup"
    )

    private fun catalog() = RestoreCatalog(drive, fs, dir / "b" / "backup")

    @Test
    fun backup_comes_back_on_an_empty_device() = runTest {
        val hash = backUp()

        val candidate = catalog().list().single()
        assertEquals(BackupEntryState.Ready, candidate.entry.state)
        val prepared = planner().prepare(candidate)
        assertEquals(2, prepared.preview.entriesAdded)
        assertEquals(1, prepared.preview.photosToDownload)

        MergeApplier(phoneB, { false }).apply(prepared.plan, "drive")
        assertEquals("pending", phoneB.backupDao().allMedia().single().status)
        val (fetched, broken) = PhotoFetcher(phoneB, drive, fs, dir / "b" / "media", dir / "b" / "backup").fetch()

        assertEquals(1 to 0, fetched to broken)
        assertEquals("normal", phoneB.backupDao().allMedia().single().status)
        assertTrue(fs.exists(dir / "b" / "media" / "$hash.jpg"))
        val restored = phoneB.backupDao().allEntries().first { it.id == "one" }
        assertEquals(entry("one").copy(updatedAt = restored.updatedAt), restored)
    }

    @Test
    fun running_again_changes_nothing() = runTest {
        backUp()
        val applier = MergeApplier(phoneB, { false })
        applier.apply(planner().prepare(catalog().list().single()).plan, "drive")

        val second = planner().prepare(catalog().list().single())

        assertEquals(0, second.preview.entriesAdded)
        assertEquals(2, second.preview.entriesUnchanged)
    }

    private fun controller(
        settings: MutableList<ManifestSettings>,
        scope: kotlinx.coroutines.CoroutineScope,
        gate: BackupGate = BackupGate(),
        api: DriveApi = drive,
        rollback: RollbackManager? = null,
        scheduler: FakeBackupScheduler = FakeBackupScheduler(),
        store: BackupStore = BackupStore(InMemoryDataStore(emptyPreferences()))
    ): RestoreController {
        val media = dir / "b" / "media"
        val work = dir / "b" / "backup"
        val applier = lazy { MergeApplier(phoneB, { fs.exists(media / it) }) }
        return RestoreController(
            lazy { catalog() }, lazy { planner(api) }, applier, lazy { PhotoFetcher(phoneB, drive, fs, media, work) },
            lazy { LegacyBackupImporter(phoneB, drive, { _, _, _ -> null }, fs, work) },
            lazy {
                rollback ?: RollbackManager(phoneB, applier.value, store, FreeSpace { 1_000_000_000 }, fs, work, { ManifestSettings() }, {})
            },
            lazy { error("쓰지 않는다") },
            lazy { phoneB },
            store, FakeNetworkState(), fs, work, { settings += it }, scope, gate, scheduler
        )
    }

    @Test
    fun controller_merges_then_fetches_and_applies_settings() = runTest {
        backUp()
        val settings = mutableListOf<ManifestSettings>()
        val controller = controller(settings, backgroundScope)
        val name = controller.backups()!!.single().snapshotName
        assertIs<RestorePreparation.Ready>(controller.prepare(name))

        controller.merge()
        val done = withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }

        val result = (done as RestoreState.Done).result
        assertEquals(2, result.entriesAdded)
        assertEquals(1, result.photosRestored)
        assertEquals(0, result.photosPending)
        assertEquals(1, settings.size)
    }

    @Test
    fun a_restored_backup_is_remembered_so_it_is_not_called_the_first_backup() = runTest {
        backUp()
        val store = BackupStore(InMemoryDataStore(emptyPreferences()))
        val controller = controller(mutableListOf(), backgroundScope, store = store)
        val entry = controller.backups()!!.single()
        assertIs<RestorePreparation.Ready>(controller.prepare(entry.snapshotName))

        controller.merge()
        withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }

        assertEquals(entry.createdAt, store.current().restoredBackupAt)
        assertEquals(null, store.current().lastSuccessAt)
    }

    @Test
    fun photos_are_protected_while_they_come_and_the_progress_ends() = runTest {
        backUp()
        val scheduler = FakeBackupScheduler()
        val controller = controller(mutableListOf(), backgroundScope, scheduler = scheduler)
        val name = controller.backups()!!.single().snapshotName
        assertIs<RestorePreparation.Ready>(controller.prepare(name))

        controller.merge()
        withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }

        assertEquals(1, scheduler.restoreKept)
        assertEquals(null, controller.photoProgress.value)
    }

    @Test
    fun restore_waits_for_a_running_backup_instead_of_vanishing() = runTest {
        backUp()
        val gate = BackupGate()
        val controller = controller(mutableListOf(), backgroundScope, gate)
        val name = controller.backups()!!.single().snapshotName
        assertIs<RestorePreparation.Ready>(controller.prepare(name))
        assertTrue(gate.tryEnter())

        controller.merge()
        withContext(Dispatchers.Default) { delay(100) }
        assertEquals(RestoreState.Merging, controller.state.value)

        gate.leave()
        withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }
    }

    @Test
    fun missing_snapshot_is_listed_as_damaged() = runTest {
        backUp()
        drive.deleteByUser(drive.visible.first { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_SNAPSHOT }.id)

        assertEquals(BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISSING), catalog().list().single().entry.state)
    }

    @Test
    fun newer_format_cannot_be_picked() = runTest {
        backUp()
        val listed = catalog().list().single()
        val future = listed.manifest!!.copy(formatVersion = 99)

        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(future))
    }

    @Test
    fun wrong_hash_stops_before_any_write() = runTest {
        backUp()
        val listed = catalog().list().single()
        val wrong = listed.copy(manifest = listed.manifest!!.copy(snapshot = listed.manifest.snapshot.copy(sha256 = "0".repeat(64))))

        assertFailsWith<RestoreMismatch> { planner().prepare(wrong) }
        assertEquals(0, phoneB.backupDao().allEntries().size)
    }

    private suspend fun prepareFromDrive() = planner().prepare(catalog().list().single())

    private fun applierB() = MergeApplier(phoneB, { fs.exists(dir / "b" / "media" / it) })

    @Test
    fun every_field_and_photo_row_comes_back_the_same() = runTest {
        val hash = "내용".encodeToByteArray().toByteString().sha256().hex()
        fs.write(dir / "a" / "media" / "$hash.jpg") { write("내용".encodeToByteArray()) }
        // 합치기는 진짜 시계를 쓰니 휴지통 날짜도 오늘에 맞춘다.
        // 고정 날짜면 30일이 지난 뒤부터 이 시험이 깨진다.
        val trashedRecently = (Clock.System.now() - 3.days).toEpochMilliseconds()
        val full = EntryEntity(
            "full", "2026-03-01", "07:05", "<p>글</p>", "글", "good", "sunny", "망원동", true, 11, 22, null, 5, "2024-03-01 07:05:00", "1|2"
        )
        val trashed = EntryEntity(
            "trash", "2026-03-02", "23:59", "<p>지움</p>", "지움", null, null, null, false, 3, 4, trashedRecently, null, null, null
        )
        phoneA.entryDao().insertAll(listOf(full, trashed))
        val rows =
            listOf(
                MediaEntity("m1", "full", 0, "missing", null, null, null, null, null, "/cache/gone.jpg", 9),
                MediaEntity("m2", "full", 1, "normal", "$hash.jpg", hash, 10, 20, 6, "/cache/here.jpg", 9)
            )
        phoneA.mediaDao().insertAll(rows)
        val runner =
            BackupRunner(
                phoneA, drive, FakeDriveAuthorizer(), BackupStore(InMemoryDataStore(emptyPreferences())), FakeNetworkState(),
                DeviceName {
                    "A"
                },
                fs,
                dir / "a" / "media", dir / "a" / "backup", BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings() }
            )
        assertIs<BackupOutcome.Done>(runner.run())

        val applier = applierB()
        val prepared = prepareFromDrive()
        applier.apply(prepared.plan, "drive")
        PhotoFetcher(phoneB, drive, fs, dir / "b" / "media", dir / "b" / "backup").fetch()

        assertEquals(setOf(full, trashed), phoneB.backupDao().allEntries().toSet())
        assertEquals(rows, phoneB.backupDao().allMedia().sortedBy { it.sortOrder })
    }

    @Test
    fun preview_numbers_match_what_merge_does() = runTest {
        backUp()
        phoneB.entryDao().insertAll(listOf(entry("one").copy(updatedAt = 1), entry("mine").copy(legacyId = null)))

        val prepared = prepareFromDrive()
        val counts = applierB().apply(prepared.plan, "drive")

        assertEquals(prepared.preview.entriesAdded, counts.added)
        assertEquals(prepared.preview.entriesUpdated, counts.updated)
        assertEquals(prepared.preview.entriesKeptBoth, counts.keptBoth)
        assertEquals(3, phoneB.backupDao().allEntries().size)
    }

    @Test
    fun same_old_number_from_another_phone_does_not_stop_the_merge() = runTest {
        backUp()
        phoneB.entryDao().insertAll(listOf(entry("mine").copy(legacyId = entry("one").legacyId)))

        applierB().apply(prepareFromDrive().plan, "drive")

        assertEquals(setOf("one", "two", "mine"), phoneB.backupDao().allEntries().map { it.id }.toSet())
    }

    @Test
    fun cut_off_while_fetching_leaves_the_rest_waiting() = runTest {
        backUp()
        val applier = applierB()
        applier.apply(prepareFromDrive().plan, "drive")
        val broken =
            object : DriveApi by drive {
                override suspend fun download(fileId: String, to: Path, fromByte: Long): Long = throw DriveException(BackupFailure.OFFLINE)
            }

        assertFailsWith<DriveException> { PhotoFetcher(phoneB, broken, fs, dir / "b" / "media", dir / "b" / "backup").fetch() }
        assertEquals("pending", phoneB.backupDao().allMedia().single().status)
        assertEquals(2, phoneB.backupDao().allEntries().size)

        PhotoFetcher(phoneB, drive, fs, dir / "b" / "media", dir / "b" / "backup").fetch()

        assertEquals("normal", phoneB.backupDao().allMedia().single().status)
    }

    @Test
    fun failed_merge_changes_nothing() = runTest {
        phoneB.entryDao().insertAll(listOf(entry("mine")))
        val twin = MergeEntry(entry("same"), emptyList())
        val plan = MergePlan(listOf(twin to MergeAction.ADD, twin to MergeAction.ADD), emptyMap())

        assertFails { applierB().apply(plan, "drive") }

        assertEquals(setOf("mine"), phoneB.backupDao().allEntries().map { it.id }.toSet())
    }

    @Test
    fun picture_that_a_later_backup_has_fills_an_unchanged_entry() = runTest {
        val hash = backUp()
        applierB().apply(prepareFromDrive().plan, "drive")
        assertEquals("pending", phoneB.backupDao().allMedia().single().status)

        val again = prepareFromDrive()

        assertEquals(0, again.preview.entriesAdded)
        assertEquals(1, again.preview.photosToDownload)
        PhotoFetcher(phoneB, drive, fs, dir / "b" / "media", dir / "b" / "backup").fetch()
        assertTrue(fs.exists(dir / "b" / "media" / "$hash.jpg"))
    }

    @Test
    fun settings_and_lock_notice_come_with_the_result() = runTest {
        backUp()
        val settings = mutableListOf<ManifestSettings>()
        val controller = controller(settings, backgroundScope)
        controller.prepare(controller.backups()!!.single().snapshotName)

        controller.merge()
        val done = withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }

        assertEquals(1, settings.size)
        assertEquals(false, (done as RestoreState.Done).result.lockWasOn)
    }

    /** 내려받기가 어느 디스패처에서 도는지 적는다. */
    private class WatchingDrive(private val real: DriveApi) : DriveApi by real {
        var seen: ContinuationInterceptor? = null
            private set

        override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
            seen = currentCoroutineContext()[ContinuationInterceptor]
            return real.download(fileId, to, fromByte)
        }
    }

    private val incoming get() = dir / "b" / "backup" / "incoming"

    private fun copiesLeft() = fs.listOrNull(incoming).orEmpty()

    private suspend fun untilTrue(check: suspend () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (!check()) delay(10)
        }
    }

    @Test
    fun a_manifest_pointing_outside_the_app_folder_is_refused() = runTest {
        backUp()
        val listed = catalog().list().single()
        val outside = listed.manifest!!.snapshot.copy(fileName = "../../evil.db")

        assertFailsWith<RestoreMismatch> { planner().prepare(listed.copy(manifest = listed.manifest.copy(snapshot = outside))) }

        assertFalse(fs.exists(dir / "b" / "evil.db"))
    }

    @Test
    fun a_snapshot_row_pointing_outside_the_photo_folder_is_refused() = runTest {
        val bytes = "다른 내용".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        backUp(listOf(MediaEntity("evil", "two", 0, "pending", "../../evil.jpg", hash, 1, 1, null, null, 1)))

        assertFailsWith<RestoreMismatch> { planner().prepare(catalog().list().single()) }
    }

    @Test
    fun a_photo_name_that_is_not_its_own_hash_is_refused() = runTest {
        val bytes = "다른 내용".encodeToByteArray()
        val other = "0".repeat(64)
        backUp(listOf(MediaEntity("wrong", "two", 0, "pending", "$other.jpg", bytes.toByteString().sha256().hex(), 1, 1, null, null, 1)))

        assertFailsWith<RestoreMismatch> { planner().prepare(catalog().list().single()) }
    }

    @Test
    fun a_refused_snapshot_leaves_no_copy_behind() = runTest {
        backUp(listOf(MediaEntity("evil", "two", 0, "pending", "../../evil.jpg", "0".repeat(64), 1, 1, null, null, 1)))
        val controller = controller(mutableListOf(), backgroundScope)

        val failed = assertIs<RestorePreparation.Failed>(controller.prepare(controller.backups()!!.single().snapshotName))

        assertEquals(RestoreFailure.MISMATCH, failed.reason)
        assertTrue(copiesLeft().isEmpty())
    }

    @Test
    fun the_drive_preview_does_not_run_on_the_calling_thread() = runTest {
        backUp()
        val watching = WatchingDrive(drive)
        val controller = controller(mutableListOf(), backgroundScope, api = watching)

        assertIs<RestorePreparation.Ready>(controller.prepare(controller.backups()!!.single().snapshotName))

        assertEquals(Dispatchers.IO, watching.seen)
    }

    @Test
    fun leaving_the_screen_does_not_stop_a_rollback() = runTest {
        phoneB.entryDao().insertAll(listOf(entry("here")))
        val store = BackupStore(InMemoryDataStore(emptyPreferences()))
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val manager =
            RollbackManager(
                phoneB,
                MergeApplier(phoneB, {
                    false
                }),
                store,
                FreeSpace { 1_000_000_000 },
                fs,
                dir / "b" / "backup",
                { ManifestSettings() },
                {
                    started.complete(Unit)
                    release.await()
                }
            )
        manager.replace(listOf(MergeEntry(entry("new"), emptyList())), "drive")
        val controller = controller(mutableListOf(), backgroundScope, rollback = manager)

        val screen = launch { controller.rollBack() }
        withContext(Dispatchers.Default) { withTimeout(5_000) { started.await() } }
        screen.cancelAndJoin()
        release.complete(Unit)

        untilTrue { store.current().rollbackUntil == null }
    }

    @Test
    fun closing_the_preview_leaves_no_copy_of_the_diary() = runTest {
        backUp()
        val controller = controller(mutableListOf(), backgroundScope)
        controller.prepare(controller.backups()!!.single().snapshotName)
        assertTrue(copiesLeft().isNotEmpty())

        controller.dismiss()

        untilTrue { copiesLeft().isEmpty() }
    }

    @Test
    fun the_next_preview_clears_the_last_one() = runTest {
        backUp()
        fs.createDirectories(incoming)
        fs.write(incoming / "picked") { writeUtf8("지난번에 고른 파일") }
        val controller = controller(mutableListOf(), backgroundScope)

        controller.prepare(controller.backups()!!.single().snapshotName)

        assertFalse(fs.exists(incoming / "picked"))
    }

    @Test
    fun a_copy_left_by_a_killed_app_is_cleared_on_the_next_open() = runTest {
        fs.createDirectories(incoming)
        fs.write(incoming / "picked") { writeUtf8("앱이 꺼지며 남은 파일") }

        controller(mutableListOf(), backgroundScope).resumePhotos()

        assertTrue(copiesLeft().isEmpty())
    }

    @Test
    fun dismiss_leaves_a_running_restore_alone() = runTest {
        backUp()
        val gate = BackupGate()
        val controller = controller(mutableListOf(), backgroundScope, gate)
        controller.prepare(controller.backups()!!.single().snapshotName)
        assertTrue(gate.tryEnter())
        controller.merge()

        controller.dismiss()
        withContext(Dispatchers.Default) { delay(100) }

        assertEquals(RestoreState.Merging, controller.state.value)
        assertTrue(copiesLeft().isNotEmpty())
        gate.leave()
        withContext(Dispatchers.Default) { withTimeout(5_000) { controller.state.first { it is RestoreState.Done } } }
    }
}
