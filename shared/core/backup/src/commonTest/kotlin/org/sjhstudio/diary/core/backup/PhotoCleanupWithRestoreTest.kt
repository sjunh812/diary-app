package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.data.photo.DefaultMediaRepository
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.database.DraftMediaEntity
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.testing.FakeDrive

/** 앱을 열 때 도는 사진 정리는 파일을 영영 지우는 유일한 길. 복원과 겹쳐도 쓰는 사진은 지우지 않음(헌법 I) */
class PhotoCleanupWithRestoreTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var phone: TestPhone
    private lateinit var other: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "photo-cleanup-${Random.nextLong()}"
        drive = FakeDrive(fs)
        phone = TestPhone(dir / "phone", drive)
        other = TestPhone(dir / "other", drive, name = "Pixel 9")
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        other.close()
        fs.deleteRecursively(dir)
    }

    private fun TestPhone.cleaner() = DefaultMediaRepository(
        db,
        fs,
        media,
        root / "thumbs",
        keepEverything = { store.current().rollbackUntil != null }
    )

    /** 파일 시각보다 넉넉히 뒤. 하루 넘은 파일만 지우는 규칙에 걸리게 함 */
    private fun later(): Instant = Clock.System.now() + 2.days

    private fun TestPhone.has(hash: String) = fs.exists(media / "$hash.jpg")

    private suspend fun TestPhone.restoreLatest() {
        val latest = catalog().list().first { it.entry.state == BackupEntryState.Ready }
        applier().apply(planner().prepare(latest).plan, "drive")
    }

    @Test
    fun restored_photos_stay_after_cleanup() = runTest {
        phone.write("e1")
        val hash = phone.photo("e1", 0, "사진")
        assertIs<BackupOutcome.Done>(phone.runner().run())

        other.restoreLatest()
        other.fetcher().fetch()
        other.cleaner().cleanUnused(later())

        assertTrue(other.has(hash))
    }

    /** 사진을 다 받기 전에 앱을 닫았다 열어도 받은 사진은 지우지 않음 */
    @Test
    fun photos_fetched_halfway_stay_after_cleanup() = runTest {
        phone.write("e1")
        phone.write("e2")
        val first = phone.photo("e1", 0, "첫째")
        val second = phone.photo("e2", 0, "둘째")
        assertIs<BackupOutcome.Done>(phone.runner().run())

        other.restoreLatest()
        runCatching { other.fetcher(CutDrive(drive, after = 1)).fetch() }
        other.cleaner().cleanUnused(later())
        other.fetcher().fetch()

        assertTrue(other.has(first))
        assertTrue(other.has(second))
    }

    @Test
    fun a_photo_of_a_trashed_entry_stays_until_it_is_gone_for_good() = runTest {
        phone.write("e1")
        val hash = phone.photo("e1", 0, "사진")
        phone.db.entryDao().setDeletedAt("e1", 100, 100)

        phone.cleaner().cleanUnused(later())
        assertTrue(phone.has(hash))

        phone.db.entryDao().deleteAllTrashed()
        phone.cleaner().cleanUnused(later())
        assertFalse(phone.has(hash))
    }

    @Test
    fun a_photo_only_in_a_draft_stays() = runTest {
        val content = "임시 저장본 사진"
        val hash = TestPhone.hashOf(content)
        fs.write(phone.media / "$hash.jpg") { writeUtf8(content) }
        phone.db.draftDao().upsert(DraftEntity("new", "2026-11-01", "09:00", "", null, null, null, false, false, 0))
        phone.db.draftDao().appendMedia(DraftMediaEntity("new", "$hash.jpg", hash, 1, 1, 1, 0))

        phone.cleaner().cleanUnused(later())

        assertTrue(phone.has(hash))
    }

    /** 바꾸기 뒤 7일은 되돌릴 수 있으니 바꾸기 전 사진을 지우지 않음. 되돌리면 그대로 보임 */
    @Test
    fun photos_replaced_away_stay_while_the_rollback_is_open() = runTest {
        phone.write("before")
        val hash = phone.photo("before", 0, "바꾸기 전 사진")
        phone.manager().replace(listOf(MergeEntry(TestPhone.entry("new"), emptyList())), "drive")

        phone.cleaner().cleanUnused(later())
        assertTrue(phone.has(hash))

        assertTrue(phone.manager().rollBack())
        assertEquals("normal", phone.photoStates()["before-0"])
        assertTrue(phone.has(hash))
    }

    @Test
    fun photos_replaced_away_are_cleaned_once_the_rollback_ended() = runTest {
        phone.write("before")
        val hash = phone.photo("before", 0, "바꾸기 전 사진")
        phone.manager().replace(listOf(MergeEntry(TestPhone.entry("new"), emptyList())), "drive")

        phone.clock.advance(8.days)
        phone.manager().expire()
        phone.cleaner().cleanUnused(later())

        assertFalse(phone.has(hash))
    }

    /** 사진 하나를 두 일기가 쓰면 한쪽을 지워도 파일은 남음 */
    @Test
    fun a_photo_shared_with_a_living_entry_stays() = runTest {
        phone.write("e1")
        phone.write("e2")
        val hash = phone.photo("e1", 0, "같은 사진")
        phone.photo("e2", 0, "같은 사진")
        phone.db.entryDao().deletePermanently("e1")

        phone.cleaner().cleanUnused(later())

        assertTrue(phone.has(hash))
    }
}
