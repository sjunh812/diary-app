package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.FakeDrive

class PhotoFetcherTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase

    /**
     * 사진을 받는 데 잠깐 걸리는 드라이브.
     * 동시에 몇 장을 받았는지 잰다.
     */
    private inner class SlowDrive(private val photos: Map<String, ByteArray>, private val failing: Map<String, Exception> = emptyMap()) :
        DriveApi by FakeDrive(fs) {
        private val lock = Mutex()
        private var flying = 0
        var most = 0
            private set
        val asked = mutableListOf<String>()

        override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage =
            DrivePage(photos.keys.map { DriveFile(it, "$it.jpg", null, emptyList(), null, null, emptyMap(), 0) }, null)

        override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
            lock.withLock {
                most = maxOf(most, ++flying)
                asked += fileId
            }
            delay(WAIT_MILLIS)
            lock.withLock { flying-- }
            failing[fileId]?.let { error ->
                // 받다가 끊기면 반쯤 쓴 파일이 남는다.
                fs.write(to) { writeUtf8("쓰다 만") }
                throw error
            }
            fs.write(to) { write(photos.getValue(fileId)) }
            return photos.getValue(fileId).size.toLong()
        }
    }

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fetcher-${Random.nextLong()}"
        fs.createDirectories(dir)
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
    }

    @AfterTest
    fun tearDown() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private fun entry() = EntryEntity("e", "2026-10-31", "21:40", "<p>글</p>", "글", null, null, null, false, 1, 7, null, null, null, null)

    private suspend fun waiting(count: Int): Map<String, ByteArray> {
        val photos = List(count) { "사진 $it".encodeToByteArray() }.associateBy { it.toByteString().sha256().hex() }
        db.entryDao().insertAll(listOf(entry()))
        db.mediaDao().insertAll(
            photos.entries.mapIndexed { index, (hash, bytes) ->
                MediaEntity("m$index", "e", index, "pending", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1)
            }
        )
        return photos
    }

    private fun fetcher(drive: DriveApi) = PhotoFetcher(db, drive, fs, dir / "media", dir / "backup")

    @Test
    fun photos_come_several_at_a_time_and_all_land() = runTest {
        val photos = waiting(COUNT)
        val drive = SlowDrive(photos)
        val progress = mutableListOf<Int>()

        val (fetched, broken) = fetcher(drive).fetch { done, _ -> progress += done }

        assertEquals(COUNT to 0, fetched to broken)
        assertTrue(drive.most in 2..PARALLEL, "동시에 받은 수 ${drive.most}")
        assertEquals((1..COUNT).toList(), progress)
        assertTrue(db.backupDao().allMedia().all { it.status == "normal" })
        photos.keys.forEach { assertTrue(fs.exists(dir / "media" / "$it.jpg")) }
    }

    @Test
    fun a_cut_leaves_the_rest_waiting_for_next_time() = runTest {
        val photos = waiting(COUNT)
        val failing = photos.keys.first()
        val cut = SlowDrive(photos, mapOf(failing to DriveException(BackupFailure.OFFLINE)))

        assertFailsWith<DriveException> { fetcher(cut).fetch() }
        assertTrue(failing in db.backupDao().pendingFiles().map { it.sha256 })
        assertTrue(fs.list(dir / "backup").isEmpty(), "반쯤 받은 파일이 남았다")

        val (fetched, _) = fetcher(SlowDrive(photos)).fetch()

        assertTrue(db.backupDao().pendingFiles().isEmpty())
        assertTrue(fetched >= 1)
        photos.keys.forEach { assertTrue(fs.exists(dir / "media" / "$it.jpg")) }
    }

    @Test
    fun a_cut_from_the_connection_itself_also_stops_the_rest() = runTest {
        val photos = waiting(COUNT)

        assertFailsWith<IOException> { fetcher(SlowDrive(photos, mapOf(photos.keys.first() to IOException("끊김")))).fetch() }

        assertTrue(fs.list(dir / "backup").isEmpty(), "반쯤 받은 파일이 남았다")
    }

    @Test
    fun an_expired_login_stops_the_rest() = runTest {
        // 동시에 받는 몇 장은 멈추기 전에 더 나갈 수 있어서 충분히 많이 둔다.
        val photos = waiting(COUNT * 10)
        // 받는 순서는 정해져 있지 않다.
        // 어느 장이 먼저여도 같도록 모든 장이 거절당하게 둔다.
        val drive = SlowDrive(photos, photos.keys.associateWith { DriveException(BackupFailure.AUTH) })

        assertFailsWith<DriveException> { fetcher(drive).fetch() }

        assertTrue(drive.asked.size < COUNT * 10, "${drive.asked.size}장을 받으러 갔다")
    }

    @Test
    fun a_file_gone_from_drive_stays_waiting_and_the_rest_land() = runTest {
        val photos = waiting(COUNT)
        val gone = photos.keys.first()

        val (fetched, broken) = fetcher(SlowDrive(photos, mapOf(gone to DriveNotFound()))).fetch()

        assertEquals(COUNT - 1 to 0, fetched to broken)
        assertEquals(listOf(gone), db.backupDao().pendingFiles().map { it.sha256 })
        assertTrue(fs.list(dir / "backup").isEmpty(), "반쯤 받은 파일이 남았다")
    }

    @Test
    fun a_file_listed_twice_is_fetched_once() = runTest {
        val bytes = "같은 사진".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        db.entryDao().insertAll(listOf(entry()))
        db.mediaDao().insertAll(
            listOf(
                MediaEntity("a", "e", 0, "pending", "$hash.jpg", hash, 1, 1, bytes.size.toLong(), null, 1),
                MediaEntity("b", "e", 1, "pending", "$hash.jpg", hash, 1, 1, null, null, 1)
            )
        )
        val drive = SlowDrive(mapOf(hash to bytes))

        val (fetched, _) = fetcher(drive).fetch()

        assertEquals(listOf(hash), drive.asked)
        assertEquals(1, fetched)
        assertTrue(db.backupDao().allMedia().all { it.status == "normal" })
    }

    @Test
    fun rows_with_another_name_keep_their_own_state() = runTest {
        val bytes = "같은 사진".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        db.entryDao().insertAll(listOf(entry()))
        db.mediaDao().insertAll(
            listOf(
                MediaEntity("jpg", "e", 0, "pending", "$hash.jpg", hash, 1, 1, null, null, 1),
                MediaEntity("png", "e", 1, "pending", "$hash.png", hash, 1, 1, null, null, 1)
            )
        )
        val photo = dir / "photo"
        fs.write(photo) { write(bytes) }
        val zip = dir / "bundle.zip"
        fs.sink(zip).buffer().use { BundleWriter(fs).write(it, listOf(BundleItem("media/$hash.jpg", photo))) }

        fetcher(FakeDrive(fs)).fetchFrom(zip)

        assertEquals(mapOf("jpg" to "normal", "png" to "pending"), db.backupDao().allMedia().associate { it.id to it.status })
    }

    @Test
    fun a_name_with_dots_cannot_leave_the_photo_folder() = runTest {
        val bytes = "남의 파일을 덮는 내용".encodeToByteArray()
        val hash = bytes.toByteString().sha256().hex()
        db.entryDao().insertAll(listOf(entry()))
        db.mediaDao().insertAll(listOf(MediaEntity("m", "e", 0, "pending", "../evil.jpg", hash, 1, 1, null, null, 1)))

        val (fetched, broken) = fetcher(SlowDrive(mapOf(hash to bytes))).fetch()

        assertEquals(0 to 1, fetched to broken)
        assertFalse(fs.exists(dir / "evil.jpg"))
        assertEquals("missing", db.backupDao().allMedia().single().status)
    }

    private companion object {
        const val COUNT = 12
        const val PARALLEL = 6
        const val WAIT_MILLIS = 20L
    }
}
