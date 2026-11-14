package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.NoBackupScheduler
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/** contracts/backup-flow.md 권하기 카드 조건 */
class BackupNudgeTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase
    private lateinit var store: BackupStore
    private var now = Instant.parse("2026-11-05T01:00:00Z")
    private val clock = object : Clock {
        override fun now() = this@BackupNudgeTest.now
    }

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "nudge-${Random.nextLong()}"
        fs.createDirectories(dir)
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
        store = BackupStore(InMemoryDataStore(emptyPreferences()))
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun controller(migrated: Boolean = false, account: String? = null) = BackupController(
        store, lazy { db }, lazy { error("쓰지 않는다") }, FakeDriveAuthorizer(), NoBackupScheduler, FakeBackupNotifier(),
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), clock, { migrated }, { account }
    )

    private suspend fun entries(count: Int) = db.entryDao().insertAll(
        List(count) {
            EntryEntity("e$it", "2026-10-31", "21:40", "<p>$it</p>", "$it", null, null, null, false, 1, 1, null, null, null, null)
        }
    )

    private suspend fun photo() = db.mediaDao().insertAll(listOf(MediaEntity("m", "e0", 0, "normal", "a.jpg", "a", 1, 1, 1, null, 1)))

    @Test
    fun two_entries_are_not_enough() = runTest {
        entries(2)

        assertNull(controller().nudge.first())
    }

    @Test
    fun three_entries_show_the_card() = runTest {
        entries(3)

        assertEquals(3, controller().nudge.first()?.entryCount)
    }

    @Test
    fun one_entry_with_a_photo_shows_the_card() = runTest {
        entries(1)
        photo()

        assertEquals(1, controller().nudge.first()?.photoCount)
    }

    // 백업·복원 화면처럼 사진 파일 수로 계산. 같은 사진이 두 일기에 붙어도 한 장
    @Test
    fun same_photo_in_two_entries_counts_once() = runTest {
        entries(2)
        db.mediaDao().insertAll(
            listOf(
                MediaEntity("m0", "e0", 0, "normal", "a.jpg", "a", 1, 1, 1, null, 1),
                MediaEntity("m1", "e1", 0, "normal", "a.jpg", "a", 1, 1, 1, null, 1)
            )
        )

        assertEquals(1, controller().nudge.first()?.photoCount)
    }

    @Test
    fun migrated_user_sees_it_right_away_with_the_old_account() = runTest {
        entries(1)
        assertEquals("old@gmail.com", controller(migrated = true, account = "old@gmail.com").nudge.first()?.suggestedAccount)
    }

    // 옮긴 일기를 모두 지우면 "일기 0편이 이 휴대폰에만 있어요"가 뜨면 안 됨
    // 지킬 것이 없으면 권하지 않음
    @Test
    fun migrated_user_with_nothing_left_does_not_see_it() = runTest {
        assertNull(controller(migrated = true, account = "old@gmail.com").nudge.first())
    }

    @Test
    fun later_hides_for_fourteen_days() = runTest {
        entries(3)
        val controller = controller()
        controller.dismissNudge()

        now += 13.days
        assertNull(controller().nudge.first())
        now += 2.days
        assertEquals(3, controller().nudge.first()?.entryCount)
    }

    @Test
    fun third_later_stops_it_for_good() = runTest {
        entries(3)
        repeat(3) { controller().dismissNudge() }

        now += 30.days

        assertNull(controller().nudge.first())
    }

    @Test
    fun connected_or_self_disconnected_users_do_not_see_it() = runTest {
        entries(3)
        store.update { it.copy(accountKey = "a") }
        assertNull(controller().nudge.first())

        store.update { it.copy(accountKey = null, userDisconnected = true) }
        assertNull(controller().nudge.first())
    }
}
