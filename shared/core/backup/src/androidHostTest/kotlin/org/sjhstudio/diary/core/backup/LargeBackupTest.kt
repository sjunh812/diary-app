package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.ChunkResult
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.StorageQuota
import org.sjhstudio.diary.core.drive.UploadSession
import org.sjhstudio.diary.core.model.BackupStep
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/**
 * 일기 1만 편, 사진 1만 장(30MB 석 장 포함)이 메모리 상한 안에서 끝까지 간다(스펙 003 T038, SC-005).
 * 파일 크기만큼 메모리를 쓰지 않는지 재야 해서 내용을 쌓아 두지 않는 대역을 쓴다.
 * 힙을 재는 시험이라 JVM에서만 돈다.
 */
class LargeBackupTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var db: DiaryDatabase

    @BeforeTest
    fun open() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "large-${Random.nextLong()}"
        fs.createDirectories(dir / "media")
        db = Room.databaseBuilder<DiaryDatabase>((dir / "diary.db").toString()).configured().build()
    }

    @AfterTest
    fun close() {
        db.close()
        fs.deleteRecursively(dir)
    }

    private suspend fun seed(): Long {
        var total = 0L
        (0 until ENTRIES).chunked(1000).forEach { ids ->
            db.entryDao().insertAll(
                ids.map {
                    val text = "일기 $it"
                    EntryEntity("e$it", "2026-01-01", "21:40", "<p>$text</p>", text, null, null, null, false, 1, 7, null, null, null, null)
                }
            )
            db.mediaDao().insertAll(
                ids.map {
                    val size = if (it < BIG_PHOTOS) BIG_SIZE else SMALL_SIZE
                    val hash = writePhoto(it, size)
                    total += size
                    MediaEntity("m$it", "e$it", 0, "normal", "$hash.jpg", hash, 1, 1, size.toLong(), null, 1)
                }
            )
        }
        return total
    }

    private fun writePhoto(seed: Int, size: Int): String {
        val temp = dir / "photo.tmp"
        val block = ByteArray(BLOCK) { (it + seed).toByte() }
        fs.write(temp) {
            var left = size
            writeInt(seed)
            left -= Int.SIZE_BYTES
            while (left > 0) {
                val count = minOf(left, BLOCK)
                write(block, 0, count)
                left -= count
            }
        }
        val hash = hashOf(temp)
        fs.atomicMove(temp, dir / "media" / "$hash.jpg")
        return hash
    }

    private fun hashOf(path: Path): String {
        val sink = HashingSink.sha256(blackholeSink())
        fs.source(path).buffer().use { it.readAll(sink) }
        return sink.hash.hex()
    }

    @Test
    fun ten_thousand_entries_and_photos_finish_within_the_memory_limit() = runTest(timeout = 10.minutes) {
        val total = seed()
        var peak = 0L
        var lastBytes = 0L
        var movedBackward = false
        val drive = StreamingDrive(fs) {}
        val store = BackupStore(InMemoryDataStore(emptyPreferences()))
        val runner =
            BackupRunner(
                db, drive, FakeDriveAuthorizer(), store, FakeNetworkState(), DeviceName { "시험" }, fs, dir / "media", dir / "backup",
                BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings(font = "pretendard") }
            )
        drive.onFile = { count ->
            runner.progress.value?.takeIf { it.step == BackupStep.UploadingPhotos }?.let {
                if (it.bytesDone < lastBytes) movedBackward = true
                lastBytes = it.bytesDone
            }
            if (count % SAMPLE_EVERY == 0) peak = maxOf(peak, retainedBytes())
        }
        val before = retainedBytes()

        val outcome = assertIs<BackupOutcome.Done>(runner.run())

        assertEquals(ENTRIES, outcome.photosUploaded)
        assertEquals(0, outcome.photosLeft)
        assertEquals(total, drive.photoBytes)
        assertTrue(!movedBackward, "진행 숫자가 뒤로 갔다")
        assertTrue(lastBytes > 0, "진행 숫자가 나아가지 않았다")
        val grown = peak - before
        assertTrue(grown < MEMORY_LIMIT, "메모리가 ${grown / MEGABYTE}MB 늘었다")
    }

    private fun retainedBytes(): Long {
        val runtime = Runtime.getRuntime()
        System.gc()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /** 내용은 흘려 보내며 확인값만 남긴다. 조각도 실제 드라이브처럼 나눠 받는다. */
    private class StreamingDrive(private val fileSystem: FileSystem, var onFile: suspend (Int) -> Unit) : DriveApi {
        private val files = LinkedHashMap<String, DriveFile>()
        private val pending = HashMap<String, NewFile>()
        private var nextId = 1
        private var count = 0
        var photoBytes = 0L
            private set

        override suspend fun about() = StorageQuota(null, 0)

        override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage {
            val matched =
                files.values.filter {
                    (query.name == null || it.name == query.name) &&
                        (query.parentId == null || query.parentId in it.parents) &&
                        query.props.all { (key, value) -> it.props[key] == value }
                }
            return DrivePage(matched, null)
        }

        override suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>) =
            add(NewFile(name, parentId, "folder", props), null, 0)

        override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile = accept(meta, body)

        override suspend fun startResumable(meta: NewFile, size: Long): UploadSession {
            val uri = "session-${nextId++}"
            pending[uri] = meta
            return UploadSession(uri, size)
        }

        override suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult {
            val end = minOf(from + CHUNK, session.size)
            fileSystem.source(body).buffer().use {
                it.skip(from)
                val sink = Buffer()
                it.readFully(sink, end - from)
            }
            if (end < session.size) return ChunkResult.Progress(end)
            return ChunkResult.Done(accept(pending.getValue(session.uri), body))
        }

        override suspend fun queryResumable(session: UploadSession) = ChunkResult.Expired

        override suspend fun get(fileId: String) = files.getValue(fileId)

        override suspend fun download(fileId: String, to: Path, fromByte: Long) = 0L

        override suspend fun setProperties(fileId: String, props: Map<String, String?>) = files.getValue(fileId)

        override suspend fun trash(fileId: String) {
            files.remove(fileId)
        }

        private suspend fun accept(meta: NewFile, body: Path): DriveFile {
            val hasher = HashingSink.sha256(blackholeSink())
            val size = fileSystem.source(body).buffer().use { it.readAll(hasher) }
            if (meta.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_PHOTO) photoBytes += size
            val file = add(meta, hasher.hash.hex(), size)
            onFile(++count)
            return file
        }

        private fun add(meta: NewFile, sha256: String?, size: Long): DriveFile {
            val id = "f${nextId++}"
            val file = DriveFile(id, meta.name, size, listOfNotNull(meta.parentId), sha256, null, meta.props, 1)
            files[id] = file
            return file
        }
    }

    private companion object {
        const val ENTRIES = 10_000
        const val BIG_PHOTOS = 3
        const val BIG_SIZE = 30 * 1024 * 1024
        const val SMALL_SIZE = 2_048
        const val BLOCK = 8 * 1024
        const val CHUNK = 8L * 1024 * 1024
        const val SAMPLE_EVERY = 1_000
        const val MEGABYTE = 1024 * 1024
        const val MEMORY_LIMIT = 64L * MEGABYTE
    }
}
