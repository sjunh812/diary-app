package org.sjhstudio.diary.core.migration

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 원본 내보내기(FR-009). 사본이 없어도 빈 묶음을 내보냈다고 하면 안 된다.
class OriginalExporterTest {
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

    private val pref = "<map>\n    <string name=\"password\">1234</string>\n    <int name=\"font_key\" value=\"1\" />\n</map>\n"

    private fun export(dir: TempDir): Map<String, String> {
        val out = ByteArrayOutputStream()
        OriginalExporter(dir.fs, paths(dir)).write(out, "readme")
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entries[it.name] = zip.readBytes().decodeToString() }
        }
        return entries
    }

    /** 사본을 만들기 전에 실패한 기기는 원본을 읽기만 해서 담는다. */
    @Test
    fun noCopyPacksTheOriginalsReadOnly() {
        TempDir().use { dir ->
            dir.write("databases/note.db", "db")
            dir.write("databases/note.db-wal", "wal")
            dir.write("shared_prefs/pref2.xml", "<map />")
            val prefPath = dir.write("shared_prefs/pref.xml", pref)

            val entries = export(dir)

            assertEquals("db", entries["legacy-snapshot/note.db"])
            assertEquals("wal", entries["legacy-snapshot/note.db-wal"])
            assertEquals("<map />", entries["legacy-snapshot/pref2.xml"])
            // 묶음에는 비밀번호가 없고 원본은 그대로다.
            assertFalse(checkNotNull(entries["legacy-snapshot/pref.xml"]).contains("1234"))
            assertEquals(pref, dir.fs.read(prefPath) { readUtf8() })
            assertEquals("db", dir.fs.read(dir.root / "databases" / "note.db") { readUtf8() })
        }
    }

    @Test
    fun aCopyWinsOverTheOriginal() {
        TempDir().use { dir ->
            dir.write("databases/note.db", "원본")
            dir.write("files/legacy-snapshot/note.db", "사본")
            dir.write("shared_prefs/pref2.xml", "<map />")

            val entries = export(dir)

            assertEquals("사본", entries["legacy-snapshot/note.db"])
            // 사본에 없는 파일은 원본에서 채운다.
            assertTrue("legacy-snapshot/pref2.xml" in entries)
        }
    }

    /** 담을 것이 없으면 실패다. 안내문 하나만 든 묶음을 내보냈다고 하지 않는다. */
    @Test
    fun nothingToPackFails() {
        TempDir().use { dir ->
            assertFailsWith<IllegalStateException> { export(dir) }
        }
    }
}
