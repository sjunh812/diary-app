package org.sjhstudio.diary.core.backup

import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.testing.FakeDrive

class PhotoFetcherTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase

    /** 사진을 받는 데 잠깐 걸리는 드라이브. 동시에 몇 장을 받았는지 잰다. */
    private inner class SlowDrive(private val photos: Map<String, ByteArray>, private val failing: Set<String> = emptySet()) :
        DriveApi by FakeDrive(fs) {
        private val lock = Mutex()
        private var flying = 0
        var most = 0
            private set

        override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage =
            DrivePage(photos.keys.map { DriveFile(it, "$it.jpg", null, emptyList(), null, null, emptyMap(), 0) }, null)

        override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
            lock.withLock { most = maxOf(most, ++flying) }
            delay(WAIT_MILLIS)
            lock.withLock { flying-- }
            if (fileId in failing) throw IOException("끊김")
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

    private suspend fun waiting(count: Int): Map<String, ByteArray> {
        val photos = List(count) { "사진 $it".encodeToByteArray() }.associateBy { it.toByteString().sha256().hex() }
        db.entryDao().insertAll(
            listOf(EntryEntity("e", "2026-10-31", "21:40", "<p>글</p>", "글", null, null, null, false, 1, 7, null, null, null, null))
        )
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

        assertFailsWith<IOException> { fetcher(SlowDrive(photos, setOf(failing))).fetch() }
        assertTrue(failing in db.backupDao().pendingFiles().map { it.sha256 })

        val (fetched, _) = fetcher(SlowDrive(photos)).fetch()

        assertTrue(db.backupDao().pendingFiles().isEmpty())
        assertTrue(fetched >= 1)
        photos.keys.forEach { assertTrue(fs.exists(dir / "media" / "$it.jpg")) }
    }

    private companion object {
        const val COUNT = 12
        const val PARALLEL = 6
        const val WAIT_MILLIS = 20L
    }
}
