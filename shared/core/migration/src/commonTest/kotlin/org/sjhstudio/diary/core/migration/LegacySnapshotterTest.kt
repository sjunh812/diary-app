package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.testing.TestClock

// 원본은 바이트 그대로 복사하고 원본 해시는 그대로(SC-002, contracts/migration-flow.md Snapshotting)
class LegacySnapshotterTest {
    private fun legacy(dir: TempDir) = LegacyFiles(
        noteDb = dir.root / "databases" / "note.db",
        prefXml = dir.root / "shared_prefs" / "pref.xml",
        pref2Xml = dir.root / "shared_prefs" / "pref2.xml"
    )

    @Test
    fun copies_bytes_and_keeps_originals_unchanged() {
        TempDir().use { dir ->
            val db = ByteArray(300_000) { (it * 31).toByte() }
            dir.write("databases/note.db", db)
            dir.write("databases/note.db-wal", ByteArray(5_000) { 7 })
            dir.write(
                "shared_prefs/pref.xml",
                "<map>\n    <string name=\"password\">0000</string>\n    <int name=\"font_key\" value=\"1\" />\n</map>\n"
            )
            val files = legacy(dir)
            val snapshotter = LegacySnapshotter(dir.fs, TestClock())
            val before = listOf(files.noteDb, files.noteDbWal, files.prefXml).map(snapshotter::sha256)

            val manifest = snapshotter.snapshot(files, dir.root / "legacy-snapshot")

            assertEquals(before, listOf(files.noteDb, files.noteDbWal, files.prefXml).map(snapshotter::sha256))
            assertEquals(listOf("note.db", "note.db-wal", "pref.xml"), manifest.files.map { it.name })
            assertTrue(dir.fs.read(dir.root / "legacy-snapshot" / "note.db") { readByteArray() }.contentEquals(db))
            assertFalse("0000" in dir.fs.read(dir.root / "legacy-snapshot" / "pref.xml") { readUtf8() })
            assertTrue(dir.fs.exists(dir.root / "legacy-snapshot" / LegacySnapshotter.MANIFEST))
            assertFalse(dir.fs.exists(dir.root / "legacy-snapshot" / "note.db.tmp"))
        }
    }

    @Test
    fun same_originals_reuse_the_snapshot_and_changed_ones_copy_again() {
        TempDir().use { dir ->
            dir.write("databases/note.db", "v1")
            val files = legacy(dir)
            val clock = TestClock()
            val snapshotter = LegacySnapshotter(dir.fs, clock)
            val first = snapshotter.snapshot(files, dir.root / "legacy-snapshot")

            clock.advance(kotlin.time.Duration.parse("1m"))
            assertEquals(first, snapshotter.snapshot(files, dir.root / "legacy-snapshot"))

            dir.write("databases/note.db", "v2 바뀐 원본")
            val third = snapshotter.snapshot(files, dir.root / "legacy-snapshot")
            assertTrue(third.createdAtMillis > first.createdAtMillis)
            assertEquals("v2 바뀐 원본", dir.fs.read(dir.root / "legacy-snapshot" / "note.db") { readUtf8() })
        }
    }

    @Test
    fun no_originals_means_fresh_install() {
        TempDir().use { dir -> assertFalse(legacy(dir).exist(dir.fs)) }
    }
}
