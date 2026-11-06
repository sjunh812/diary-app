package org.sjhstudio.diary.core.backup

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Timeout
import okio.buffer
import okio.openZip
import okio.use
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.testing.FakeDrive

/** 묶음 파일이 잘리거나 깨지거나 남이 만든 것이어도 기기가 그대로인지 본다. */
class BundleDamageTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bundle-damage-${Random.nextLong()}"
        val drive = FakeDrive(fs)
        a = TestPhone(dir / "a", drive, name = "A")
        b = TestPhone(dir / "b", drive, name = "B")
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    private val incoming get() = b.work / "incoming"

    private fun copiesLeft() = fs.listOrNull(incoming).orEmpty()

    /** 일기 둘과 사진 둘을 묶음 파일로 낸다. */
    private suspend fun export(): Path {
        a.write("one")
        a.write("two")
        a.photo("one", 0, "첫째 사진")
        a.photo("two", 0, "둘째 사진")
        val zip = dir / "export.zip"
        a.exportTo(zip)
        return zip
    }

    private fun bytes(zip: Path) = fs.read(zip) { readByteArray() }

    private fun fileOf(bytes: ByteArray): Path {
        val path = dir / "made-${Random.nextLong()}"
        fs.write(path) { write(bytes) }
        return path
    }

    private fun unpack(zip: Path, name: String) = fs.openZip(zip).read("/".toPath() / name) { readByteArray() }

    /** 정상 묶음에서 항목을 고르거나 더해 다시 묶는다. */
    private fun rebuilt(zip: Path, extra: Map<String, ByteArray> = emptyMap(), without: Set<String> = emptySet()): Path {
        val archive = fs.openZip(zip)
        val photos = archive.list("/media".toPath()).map { "media/${it.name}" }
        val names = listOf(BundleExporter.MANIFEST, BundleExporter.SNAPSHOT) + photos
        val items =
            (names.associateWith { unpack(zip, it) } + extra).filterKeys { it !in without }.map { (name, content) ->
                BundleItem(name, fileOf(content))
            }
        val out = dir / "rebuilt-${Random.nextLong()}.zip"
        fs.sink(out).buffer().use { BundleWriter(fs).write(it, items) }
        return out
    }

    private suspend fun RestoreController.pick(path: Path) = prepareFile(fs.source(path))

    private suspend fun assertRefused(path: Path, keep: Set<String> = emptySet()) {
        val failed = assertIs<RestorePreparation.Failed>(b.controller(scope).pick(path), "받아서는 안 되는 파일")
        assertEquals(RestoreFailure.MISMATCH, failed.reason)
        assertEquals(keep, b.ids())
        assertTrue(copiesLeft().isEmpty(), "거절한 파일의 사본이 남았다")
    }

    @Test
    fun a_cut_file_is_refused_wherever_it_ends() = runTest {
        scope = backgroundScope
        val whole = bytes(export())
        b.write("mine")

        for (end in listOf(4, 30, 100, whole.size / 4, whole.size / 2, whole.size * 3 / 4, whole.size - 23, whole.size - 1)) {
            assertRefused(fileOf(whole.copyOf(end)), setOf("mine"))
        }
    }

    @Test
    fun files_too_short_to_tell_are_refused() = runTest {
        scope = backgroundScope

        assertRefused(fileOf(ByteArray(0)))
        assertRefused(fileOf(byteArrayOf(0x50, 0x4b)))
        assertRefused(fileOf(byteArrayOf(0x50, 0x4b, 0x03, 0x04)))
    }

    @Test
    fun an_empty_zip_is_refused() = runTest {
        scope = backgroundScope
        val zip = dir / "empty.zip"
        fs.sink(zip).buffer().use { BundleWriter(fs).write(it, emptyList()) }

        assertRefused(zip)
    }

    @Test
    fun a_zip_of_something_else_is_refused() = runTest {
        scope = backgroundScope
        val zip = dir / "other.zip"
        fs.sink(zip).buffer().use { BundleWriter(fs).write(it, listOf(BundleItem("휴가 사진.jpg", fileOf(ByteArray(10))))) }

        assertRefused(zip)
    }

    @Test
    fun names_climbing_out_of_the_folder_write_nothing_outside() = runTest {
        scope = backgroundScope
        val zip = export()
        val evil = "밖으로".encodeToByteArray()
        val made =
            rebuilt(
                zip,
                mapOf(
                    "../evil.jpg" to evil,
                    "../../evil.jpg" to evil,
                    "media/../../evil.jpg" to evil,
                    "/evil.jpg" to evil,
                    "..\\evil.jpg" to evil
                )
            )
        val controller = b.controller(scope)

        val result = controller.pick(made)
        if (result is RestorePreparation.Ready) {
            controller.merge()
            controller.finished()
        }

        val found = fs.listRecursively(dir).filter { it.name.contains("evil") }.toList()
        assertEquals(emptyList(), found)
    }

    @Test
    fun a_very_long_entry_name_does_not_crash() = runTest {
        scope = backgroundScope
        val made = rebuilt(export(), mapOf("긴이름".repeat(10_000) to "내용".encodeToByteArray()))

        val controller = b.controller(scope)
        assertIs<RestorePreparation.Ready>(controller.pick(made))
        controller.merge()

        assertIs<RestoreState.Done>(controller.finished())
        assertEquals(setOf("one", "two"), b.ids())
    }

    private fun withSnapshot(zip: Path, snapshot: ByteArray): Path {
        val manifest = assertNotNull(ManifestCodec.decode(unpack(zip, BundleExporter.MANIFEST).decodeToString()))
        val hash = snapshot.toByteString().sha256().hex()
        val text = ManifestCodec.encode(manifest.copy(snapshot = manifest.snapshot.copy(sha256 = hash)))
        return rebuilt(zip, mapOf(BundleExporter.MANIFEST to text.encodeToByteArray(), BundleExporter.SNAPSHOT to snapshot))
    }

    @Test
    fun a_snapshot_that_is_no_database_is_refused_even_with_the_right_hash() = runTest {
        scope = backgroundScope
        val zip = export()
        b.write("mine")

        assertRefused(withSnapshot(zip, "일기 DB가 아니다".encodeToByteArray()), setOf("mine"))
    }

    @Test
    fun a_database_without_diary_tables_is_refused() = runTest {
        scope = backgroundScope
        val zip = export()
        val path = dir / "other.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.execSQL("CREATE TABLE other (id INTEGER)")
        connection.close()

        assertRefused(withSnapshot(zip, bytes(path)))
    }

    @Test
    fun an_empty_snapshot_file_is_refused() = runTest {
        scope = backgroundScope

        assertRefused(withSnapshot(export(), ByteArray(0)))
    }

    @Test
    fun a_snapshot_that_differs_from_its_hash_is_refused() = runTest {
        scope = backgroundScope
        val zip = export()
        val snapshot = unpack(zip, BundleExporter.SNAPSHOT)
        snapshot[snapshot.size / 2] = (snapshot[snapshot.size / 2].toInt() xor 1).toByte()

        assertRefused(rebuilt(zip, mapOf(BundleExporter.SNAPSHOT to snapshot)))
    }

    @Test
    fun a_bundle_without_its_snapshot_is_refused() = runTest {
        scope = backgroundScope

        assertRefused(rebuilt(export(), without = setOf(BundleExporter.SNAPSHOT)))
    }

    @Test
    fun a_broken_list_file_inside_is_refused() = runTest {
        scope = backgroundScope

        assertRefused(rebuilt(export(), mapOf(BundleExporter.MANIFEST to "{".encodeToByteArray())))
    }

    @Test
    fun a_bundle_from_a_newer_app_asks_for_an_update() = runTest {
        scope = backgroundScope
        val zip = export()
        val manifest = assertNotNull(ManifestCodec.decode(unpack(zip, BundleExporter.MANIFEST).decodeToString()))
        val future = ManifestCodec.encode(manifest.copy(formatVersion = 99)).encodeToByteArray()

        val failed = assertIs<RestorePreparation.Failed>(b.controller(scope).pick(rebuilt(zip, mapOf(BundleExporter.MANIFEST to future))))

        assertEquals(RestoreFailure.NEEDS_UPDATE, failed.reason)
        assertTrue(copiesLeft().isEmpty())
    }

    @Test
    fun a_photo_missing_from_the_zip_stays_waiting_and_the_rest_arrive() = runTest {
        scope = backgroundScope
        val zip = export()
        val first = TestPhone.hashOf("첫째 사진")
        val second = TestPhone.hashOf("둘째 사진")
        val controller = b.controller(scope)
        assertIs<RestorePreparation.Ready>(controller.pick(rebuilt(zip, without = setOf("media/$first.jpg"))))

        controller.merge()
        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(1, result.photosRestored)
        assertEquals(1, result.photosPending)
        assertTrue(result.fromFile, "파일에 없는 사진은 앱을 다시 열어도 오지 않는다고 알린다")
        assertEquals(mapOf("one-0" to "pending", "two-0" to "normal"), b.photoStates())
        assertEquals(second, fs.hashes(b.media / "$second.jpg").sha256)
    }

    @Test
    fun a_photo_with_a_damaged_header_goes_missing_alone() = runTest {
        scope = backgroundScope
        val zip = export()
        val first = TestPhone.hashOf("첫째 사진")
        val second = TestPhone.hashOf("둘째 사진")
        val whole = bytes(zip)
        val at = indexOf(whole, "media/$first.jpg".encodeToByteArray())
        // 이름 앞 30바이트가 머리이고 그 첫 네 바이트가 표시다.
        for (index in at - 30 until at - 26) whole[index] = 0
        val controller = b.controller(scope)
        assertIs<RestorePreparation.Ready>(controller.pick(fileOf(whole)))

        controller.merge()
        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(1, result.photosRestored)
        assertEquals(1, result.photosMissing)
        assertEquals(mapOf("one-0" to "missing", "two-0" to "normal"), b.photoStates())
        assertEquals(second, fs.hashes(b.media / "$second.jpg").sha256)
    }

    private fun indexOf(data: ByteArray, part: ByteArray): Int =
        (0..data.size - part.size).first { start -> part.indices.all { data[start + it] == part[it] } }

    @Test
    fun a_flipped_photo_byte_goes_missing_alone() = runTest {
        scope = backgroundScope
        val zip = export()
        val first = TestPhone.hashOf("첫째 사진")
        val damaged = unpack(zip, "media/$first.jpg")
        damaged[0] = (damaged[0].toInt() xor 1).toByte()
        val controller = b.controller(scope)
        controller.pick(rebuilt(zip, mapOf("media/$first.jpg" to damaged)))

        controller.merge()
        val result = assertIs<RestoreState.Done>(controller.finished()).result

        assertEquals(1, result.photosMissing)
        assertFalse(fs.exists(b.media / "$first.jpg"))
    }

    @Test
    fun the_same_bundle_twice_adds_nothing() = runTest {
        scope = backgroundScope
        val zip = export()
        val controller = b.controller(scope)
        controller.pick(zip)
        controller.merge()
        assertEquals(2, assertIs<RestoreState.Done>(controller.finished()).result.entriesAdded)

        val second = b.controller(scope)
        val preview = assertIs<RestorePreparation.Ready>(second.pick(zip)).preview

        assertEquals(0, preview.entriesAdded)
        assertEquals(2, preview.entriesUnchanged)
        assertEquals(0, preview.photosToDownload)
    }

    @Test
    fun a_full_disk_while_copying_the_file_keeps_the_device_as_it_was() = runTest {
        scope = backgroundScope
        val zip = export()
        b.write("mine")
        val full = JammedFiles(fs) { it.name == "picked" }

        assertIs<RestorePreparation.Failed>(b.controller(scope, files = full).pick(zip))

        assertEquals(setOf("mine"), b.ids())
        assertTrue(copiesLeft().isEmpty())
    }

    @Test
    fun an_export_cut_midway_leaves_no_copy_and_the_diary_as_it_was() = runTest {
        scope = backgroundScope
        a.write("one")
        a.photo("one", 0, "사진".repeat(50_000))
        val full =
            object : Sink {
                var written = 0L

                override fun write(source: Buffer, byteCount: Long) {
                    written += byteCount
                    if (written > 10_000) throw IOException("저장 공간이 없다")
                    source.skip(byteCount)
                }

                override fun flush() = Unit

                override fun timeout() = Timeout.NONE

                override fun close() = Unit
            }

        assertFailsWith<IOException> { a.exporter().write(full.buffer()) }

        assertEquals(emptyList(), fs.listOrNull(a.work / "bundle").orEmpty())
        assertEquals(setOf("one"), a.ids())
        assertEquals("normal", a.db.backupDao().allMedia().single().status)
    }

    @Test
    fun a_device_with_nothing_exports_a_file_that_changes_nothing() = runTest {
        scope = backgroundScope
        val zip = dir / "nothing.zip"
        a.exportTo(zip)
        b.write("mine")
        val controller = b.controller(scope)
        val preview = assertIs<RestorePreparation.Ready>(controller.pick(zip)).preview
        assertEquals(0, preview.entriesAdded)

        controller.merge()
        assertIs<RestoreState.Done>(controller.finished())

        assertEquals(setOf("mine"), b.ids())
    }

    @Test
    fun a_diary_file_of_this_app_picked_directly_is_refused() = runTest {
        scope = backgroundScope
        assertRefused(fileOf(unpack(export(), BundleExporter.SNAPSHOT)))
    }

    private fun noteDb(rows: String = ""): ByteArray {
        val path = dir / "note-${Random.nextLong()}.db"
        val connection = BundledSQLiteDriver().open(path.toString())
        connection.execSQL(
            "CREATE TABLE Note (_id INTEGER PRIMARY KEY, weather INTEGER DEFAULT -1, address TEXT DEFAULT '', " +
                "location_x TEXT DEFAULT '', location_y TEXT DEFAULT '', contents TEXT DEFAULT '', mood INTEGER DEFAULT -1, " +
                "picture TEXT DEFAULT '', " +
                "create_date TIMESTAMP, modify_date TIMESTAMP, year INTEGER DEFAULT 1900, month INTEGER DEFAULT 1, star INTEGER DEFAULT 0)"
        )
        if (rows.isNotEmpty()) connection.execSQL(rows)
        connection.close()
        return bytes(path)
    }

    private fun legacyZip(content: ByteArray): Path {
        val zip = dir / "legacy-${Random.nextLong()}.zip"
        fs.sink(zip).buffer().use { BundleWriter(fs).write(it, listOf(BundleItem(BundleExporter.LEGACY_DB, fileOf(content)))) }
        return zip
    }

    @Test
    fun an_old_diary_file_comes_in_alone_or_inside_a_zip() = runTest {
        scope = backgroundScope
        val note =
            noteDb("INSERT INTO Note(_id, contents, create_date, modify_date) VALUES(1, '글', '2026-10-01 08:00:00', '2026-10-01 08:00:00')")

        val first = b.controller(scope)
        assertEquals(1, assertIs<RestorePreparation.Ready>(first.pick(fileOf(note))).preview.entriesAdded)
        first.merge()
        assertIs<RestoreState.Done>(first.finished())

        val second = b.controller(scope)
        assertEquals(0, assertIs<RestorePreparation.Ready>(second.pick(legacyZip(note))).preview.entriesAdded)
        assertEquals(1, b.ids().size)
    }

    @Test
    fun a_zip_with_a_broken_old_diary_file_is_refused() = runTest {
        scope = backgroundScope

        assertRefused(legacyZip("DB가 아니다".encodeToByteArray()))
        assertRefused(legacyZip(ByteArray(0)))
    }

    @Test
    fun an_old_diary_file_cut_in_half_adds_only_what_can_be_read() = runTest {
        scope = backgroundScope
        b.write("mine")
        val rows = (1..2000).joinToString(",") { "($it, '글 $it', '2026-10-01 08:00:00', '2026-10-01 08:00:00')" }
        val note = noteDb("INSERT INTO Note(_id, contents, create_date, modify_date) VALUES $rows")
        val controller = b.controller(scope)

        val result = controller.pick(fileOf(note.copyOf(note.size / 2)))

        // 읽히는 만큼 살리거나 거절한다.
        // 어느 쪽이든 기기 일기는 그대로다.
        if (result is RestorePreparation.Ready) {
            controller.merge()
            assertIs<RestoreState.Done>(controller.finished())
        }
        assertTrue("mine" in b.ids())
    }
}
