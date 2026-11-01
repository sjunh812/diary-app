package org.sjhstudio.diary.core.migration

import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import okio.buffer
import org.sjhstudio.diary.core.testing.TestClock

class SupportBundleTest {
    private fun paths(dir: TempDir) = MigrationPaths(
        legacy =
            LegacyFiles(
                noteDb = dir.root / "databases" / "note.db",
                prefXml = dir.root / "shared_prefs" / "pref.xml",
                pref2Xml = dir.root / "shared_prefs" / "pref2.xml"
            ),
        snapshotDir = dir.root / "files" / "legacy-snapshot",
        migrationDir = dir.root / "files" / "migration",
        mediaDir = dir.root / "files" / "media",
        databasePath = dir.root / "databases" / "diary.db"
    )

    /** 일기 파일 사본과 비밀번호를 뺀 설정만 담는다. 사진과 다른 설정 파일은 담지 않는다(FR-010a). */
    @Test
    fun bundle_holds_only_the_diary_file_and_settings() {
        TempDir().use { dir ->
            val paths = paths(dir)
            dir.write("files/legacy-snapshot/note.db", "db")
            dir.write("files/legacy-snapshot/note.db-wal", "wal")
            dir.write("files/legacy-snapshot/pref.xml", "<map />")
            dir.write("files/legacy-snapshot/pref2.xml", "<map />")
            dir.write("files/legacy-snapshot/snapshot.json", "{}")
            dir.write("files/media/abc.jpg", "jpg")

            val zip = SupportBundle(dir.fs, paths, SupportFiles(dir.fs, paths.migrationDir, TestClock())).create("support.zip")

            val names = mutableListOf<String>()
            ZipInputStream(dir.fs.source(zip).buffer().inputStream()).use { input ->
                generateSequence { input.nextEntry }.forEach { names += it.name }
            }
            assertEquals(listOf("legacy-snapshot/note.db", "legacy-snapshot/note.db-wal", "legacy-snapshot/pref.xml"), names)
        }
    }

    @Test
    fun no_snapshot_no_bundle() {
        TempDir().use { dir ->
            val paths = paths(dir)
            val files = SupportFiles(dir.fs, paths.migrationDir, TestClock())
            assertFailsWith<IllegalStateException> { SupportBundle(dir.fs, paths, files).create("support.zip") }
            assertFalse(dir.fs.exists(files.directory))
        }
    }
}
