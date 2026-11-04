package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class BundleTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var phoneA: DiaryDatabase
    private lateinit var phoneB: DiaryDatabase

    private fun open(name: String): DiaryDatabase {
        fs.createDirectories(dir / name / "media")
        return Room.databaseBuilder<DiaryDatabase>((dir / name / "diary.db").toString()).configured().build()
    }

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bundle-${Random.nextLong()}"
        fs.createDirectories(dir)
        phoneA = open("a")
        phoneB = open("b")
    }

    @AfterTest
    fun tearDown() {
        phoneA.close()
        phoneB.close()
        fs.deleteRecursively(dir)
    }

    private fun zipOf(vararg items: Pair<String, String>, writer: BundleWriter = BundleWriter(fs)): Path {
        val zip = dir / "t-${Random.nextLong()}.zip"
        val list =
            items.mapIndexed { index, (name, text) ->
                val path = dir / "item$index"
                fs.write(path) { writeUtf8(text) }
                BundleItem(name, path)
            }
        fs.sink(zip).buffer().use { writer.write(it, list) }
        return zip
    }

    private fun read(zip: Path, name: String) = fs.openZip(zip).read("/".toPath() / name) { readUtf8() }

    @Test
    fun small_archive_reads_back() {
        val zip = zipOf("README.txt" to "안내", "media/a.jpg" to "사진")

        assertEquals("안내", read(zip, "README.txt"))
        assertEquals("사진", read(zip, "media/a.jpg"))
    }

    @Test
    fun zip64_records_read_back() {
        val zip = zipOf("a.txt" to "첫째", "b.txt" to "둘째", writer = BundleWriter(fs, zip64From = 1, zip64Count = 1))

        assertEquals("첫째", read(zip, "a.txt"))
        assertEquals("둘째", read(zip, "b.txt"))
    }

    private val photo = "사진 내용".encodeToByteArray()
    private val hash = photo.toByteString().sha256().hex()

    private suspend fun exportFromA(): Path {
        fs.write(dir / "a" / "media" / "$hash.jpg") { write(photo) }
        phoneA.entryDao().insertAll(
            listOf("one", "two").map {
                EntryEntity(it, "2026-10-31", "21:40", "<p>$it</p>", it, null, null, null, false, 1, 7, null, null, null, null)
            }
        )
        phoneA.mediaDao().insertAll(listOf(MediaEntity("m", "one", 0, "normal", "$hash.jpg", hash, 1, 1, photo.size.toLong(), null, 1)))
        val exporter =
            BundleExporter(
                phoneA, BackupStore(InMemoryDataStore(emptyPreferences())), DeviceName { "A" }, fs, dir / "a" / "media",
                dir / "a" / "backup",
                BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings() }
            )
        val zip = dir / "exported.zip"
        fs.sink(zip).buffer().use { exporter.write(it) }
        return zip
    }

    private fun planner() = RestorePlanner(
        FakeDrive(fs),
        MergeApplier(phoneB, { fs.exists(dir / "b" / "media" / it) }),
        FreeSpace { 1_000_000 },
        fs,
        dir / "b" / "backup"
    )

    private fun fetcher() = PhotoFetcher(phoneB, FakeDrive(fs), fs, dir / "b" / "media", dir / "b" / "backup")

    @Test
    fun bundle_comes_back_on_an_empty_device() = runTest {
        val zip = exportFromA()

        val prepared = planner().prepareBundle(zip)
        assertEquals(2, prepared.preview.entriesAdded)
        assertEquals(1, prepared.preview.photosToDownload)
        MergeApplier(phoneB, { false }).apply(prepared.plan, "bundle")
        val (fetched, broken) = fetcher().fetchFrom(zip)

        assertEquals(1 to 0, fetched to broken)
        assertEquals("normal", phoneB.backupDao().allMedia().single().status)
        assertTrue(fs.exists(dir / "b" / "media" / "$hash.jpg"))
    }

    @Test
    fun damaged_photo_alone_becomes_missing() = runTest {
        val good = exportFromA()
        val text = read(good, "manifest.json")
        val snapshot = dir / "s.db"
        fs.openZip(good).source("/snapshot.db".toPath()).use { source -> fs.write(snapshot) { writeAll(source) } }
        val damaged = dir / "fake.jpg"
        fs.write(damaged) { writeUtf8("다른 내용") }
        val manifest = dir / "m.json"
        fs.write(manifest) { writeUtf8(text) }
        val zip = dir / "damaged.zip"
        fs.sink(zip).buffer().use {
            BundleWriter(fs).write(
                it,
                listOf(BundleItem("manifest.json", manifest), BundleItem("snapshot.db", snapshot), BundleItem("media/$hash.jpg", damaged))
            )
        }

        val prepared = planner().prepareBundle(zip)
        MergeApplier(phoneB, { false }).apply(prepared.plan, "bundle")
        val (fetched, broken) = fetcher().fetchFrom(zip)

        assertEquals(0 to 1, fetched to broken)
        assertEquals("missing", phoneB.backupDao().allMedia().single().status)
        assertEquals(2, phoneB.backupDao().allEntries().size)
    }

    @Test
    fun newer_format_is_refused() = runTest {
        val manifest = dir / "m.json"
        fs.write(manifest) { writeUtf8(ManifestCodec.encode(Manifest(formatVersion = 99))) }
        val zip = dir / "future.zip"
        fs.sink(zip).buffer().use { BundleWriter(fs).write(it, listOf(BundleItem("manifest.json", manifest))) }

        assertFailsWith<RestoreNeedsUpdate> { planner().prepareBundle(zip) }
    }
}
