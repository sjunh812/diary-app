package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.testing.FakeDrive

/** 목록 파일이 어떻게 깨져도 앱이 죽지 않고, 깨진 백업만 고를 수 없게 보이는지 본다. */
class ManifestDamageTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var phone: TestPhone

    private val props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_MANIFEST, DriveLayout.KEY_DEVICE to "someone")

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "manifest-${Random.nextLong()}"
        drive = FakeDrive(fs)
        phone = TestPhone(dir / "phone", drive)
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        fs.deleteRecursively(dir)
    }

    private suspend fun goodBackup() {
        phone.write("diary")
        assertIs<BackupOutcome.Done>(phone.runner().run())
    }

    private val format = """"format":"${Manifest.FORMAT}""""

    @Test
    fun whatever_is_not_a_list_file_reads_as_broken_and_never_throws() {
        val broken =
            listOf(
                "",
                " ",
                "null",
                "[]",
                "123",
                "\"글자\"",
                "{",
                "{\"format\"",
                """{"format":"other"}""",
                """{$format,"formatVersion":"둘"}""",
                """{$format,"createdAt":"어제"}""",
                """{$format,"media":[{"sha256":null}]}""",
                """{$format,"media":"사진"}""",
                """{$format,"snapshot":[]}""",
                """{$format,"formatVersion":99999999999999999999}""",
                "\u0000\u0001\u0002",
                "<html>로그인이 필요합니다</html>"
            )

        for (text in broken) assertEquals(ManifestCheck.BROKEN, ManifestCodec.check(ManifestCodec.decode(text)), text)
    }

    @Test
    fun half_of_a_real_list_file_is_broken() {
        val text = ManifestCodec.encode(Manifest(media = List(20) { ManifestMedia("h$it", "jpg", 1, true) }))

        for (cut in listOf(1, text.length / 4, text.length / 2, text.length - 1)) {
            assertNull(ManifestCodec.decode(text.take(cut)), "$cut 글자에서 잘림")
        }
    }

    @Test
    fun a_repeated_key_does_not_crash_and_the_last_value_counts() {
        assertNull(ManifestCodec.decode("""{$format,"format":"other"}"""))
        assertEquals(1, ManifestCodec.decode("""{$format,"formatVersion":2,"formatVersion":1}""")?.formatVersion)
    }

    @Test
    fun an_older_format_is_read() {
        val read = ManifestCodec.decode("""{$format,"formatVersion":1}""")

        assertEquals(ManifestCheck.OK, ManifestCodec.check(read))
    }

    @Test
    fun the_highest_numbers_a_future_app_may_write_ask_for_an_update() {
        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(ManifestCodec.decode("""{$format,"formatVersion":2147483647}""")))
        val text = """{$format,"snapshot":{"dbVersion":3}}"""

        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(ManifestCodec.decode(text)))
    }

    @Test
    fun a_null_value_falls_back_to_the_default() {
        val read = assertNotNull(ManifestCodec.decode("""{$format,"counts":null,"media":null,"createdAt":null}"""))

        assertEquals(ManifestCounts(), read.counts)
        assertTrue(read.media.isEmpty())
    }

    @Test
    fun names_that_climb_out_of_the_folder_are_refused() {
        for (name in listOf("", ".", "..", "../a.db", "a/b.db", "a\\b.db", "/etc/passwd", "a\u0000.db")) {
            assertFalse(RestoreNames.isSingleName(name), name)
        }
        assertTrue(RestoreNames.isSingleName("2026-11-05_01-00-00_갤럭시 S24_ab12.db"))
    }

    @Test
    fun photo_names_must_be_their_own_hash() {
        val hash = "a".repeat(64)

        assertTrue(RestoreNames.isPhoto("$hash.jpg", hash))
        assertTrue(RestoreNames.isPhoto("$hash.bin", null))
        assertFalse(RestoreNames.isPhoto("$hash.jpg", "b".repeat(64)))
        assertFalse(RestoreNames.isPhoto(hash, hash))
        assertFalse(RestoreNames.isPhoto("../$hash.jpg", hash))
        assertFalse(RestoreNames.isPhoto("$hash.jpg/../x", hash))
        assertFalse(RestoreNames.isPhoto("${hash.uppercase()}.jpg", hash))
        assertFalse(RestoreNames.isPhoto("$hash.", hash))
    }

    @Test
    fun damaged_list_files_stay_on_the_list_with_a_reason_beside_the_good_one() = runTest {
        goodBackup()
        phone.plant("empty.json", ByteArray(0), props)
        phone.plant("half.json", "{\"format\"".encodeToByteArray(), props)
        phone.plant("page.json", "<html>".encodeToByteArray(), props)

        val listed = phone.catalog().list()

        assertEquals(1, listed.count { it.entry.state == BackupEntryState.Ready })
        assertEquals(3, listed.count { it.entry.state == BackupEntryState.Damaged(DamageReason.MANIFEST) })
    }

    @Test
    fun a_damaged_backup_cannot_be_prepared_by_name() = runTest {
        phone.plant("half.json", "{".encodeToByteArray(), props)
        val controller = phone.controller(backgroundScope)

        val name = controller.backups()!!.single().snapshotName

        assertIs<RestorePreparation.Failed>(controller.prepare(name))
    }

    @Test
    fun an_empty_object_is_a_backup_without_a_snapshot() = runTest {
        phone.plant("nothing.json", "{}".encodeToByteArray(), props)

        assertEquals(BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISSING), phone.catalog().list().single().entry.state)
    }

    @Test
    fun a_snapshot_whose_drive_checksum_differs_is_listed_as_damaged() = runTest {
        goodBackup()
        val file = phone.driveFiles(DriveLayout.KIND_SNAPSHOT).single()
        drive.deleteByUser(file.id)
        phone.plant(file.name, "다른 내용".encodeToByteArray(), file.props)

        assertEquals(BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISMATCH), phone.catalog().list().single().entry.state)
    }

    @Test
    fun a_list_file_that_cannot_be_fetched_does_not_hide_the_others() = runTest {
        goodBackup()
        val lost = phone.plant("lost.json", "{}".encodeToByteArray(), props)
        val gone = object : DriveApi by drive {
            override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
                if (fileId == lost.id) throw DriveNotFound()
                return drive.download(fileId, to, fromByte)
            }
        }

        val listed = phone.catalog(gone).list()

        assertEquals(1, listed.count { it.entry.state == BackupEntryState.Ready })
    }

    @Test
    fun a_list_with_the_same_photo_twice_still_restores() = runTest {
        phone.write("diary")
        val hash = phone.photo("diary", 0, "사진")
        assertIs<BackupOutcome.Done>(phone.runner().run())
        val file = phone.driveFiles(DriveLayout.KIND_MANIFEST).single()
        val manifest = assertNotNull(ManifestCodec.decode(drive.bytes(file.id).decodeToString()))
        drive.deleteByUser(file.id)
        phone.plant(file.name, ManifestCodec.encode(manifest.copy(media = manifest.media + manifest.media)).encodeToByteArray(), file.props)
        val other = TestPhone(dir / "other", drive, name = "Other")
        try {
            val prepared = other.planner().prepare(other.catalog().list().single())
            other.applier().apply(prepared.plan, "drive")
            assertEquals(PhotoTally(fetched = 1, broken = 0), other.fetcher().fetch())
            assertEquals(hash, fs.hashes(other.media / "$hash.jpg").sha256)
        } finally {
            other.close()
        }
    }

    @Test
    fun a_list_file_never_carries_a_photo_path_or_diary_text() = runTest {
        phone.write("diary", body = "아주 비밀스러운 일기")
        phone.photo("diary", 0, "사진")
        assertIs<BackupOutcome.Done>(phone.runner().run())

        val text = drive.bytes(phone.driveFiles(DriveLayout.KIND_MANIFEST).single().id).decodeToString()

        assertFalse("비밀" in text)
        assertFalse("망원동" in text)
        assertFalse(phone.media.toString() in text)
    }
}
