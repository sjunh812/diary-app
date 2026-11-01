package org.sjhstudio.diary.core.migration

import androidx.sqlite.execSQL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.Path

// 옛 DB 사본 읽기(스펙 001 T031, T040, docs/migration.md "시험").
class LegacyDbReaderTest {
    private fun readAll(path: Path): Pair<List<LegacyNote>, LegacyReadSummary> {
        val rows = mutableListOf<LegacyNote>()
        val summary = LegacyDbReader.open(path.toString()).use { it.readAll { row -> rows += row } }
        return rows to summary
    }

    @Test
    fun version_one_upgraded_to_two_has_null_star_on_old_rows() {
        TempDir().use { dir ->
            val db = dir.root / "note.db"
            LegacyFixtures.create(db, listOf(LegacyFixtures.Row("2020-01-01 10:00:00")), version = 1).use { c ->
                c.execSQL(LegacyFixtures.UPGRADE_V1_TO_V2)
                LegacyFixtures.insert(c, listOf(LegacyFixtures.Row("2020-01-02 10:00:00", star = 1)))
            }
            val (rows, summary) = readAll(db)
            assertEquals(LegacyReadSummary(2, 0), summary)
            assertNull(rows[0].star)
            assertEquals(1L, rows[1].star)
        }
    }

    @Test
    fun version_one_without_star_column_reads_null_star() {
        TempDir().use { dir ->
            val db = dir.root / "note.db"
            LegacyFixtures.create(db, listOf(LegacyFixtures.Row("2020-01-01 10:00:00")), version = 1).close()
            assertNull(readAll(db).first.single().star)
        }
    }

    @Test
    fun empty_file_and_table_only_have_nothing_to_move() {
        TempDir().use { dir ->
            val empty = dir.root / "empty.db"
            LegacyFixtures.open(empty).close()
            LegacyDbReader.open(empty.toString()).use { reader ->
                assertFalse(reader.hasNoteTable)
                assertEquals(0L, reader.count())
            }
            val tableOnly = dir.root / "table.db"
            LegacyFixtures.create(tableOnly, emptyList()).close()
            LegacyDbReader.open(tableOnly.toString()).use { assertEquals(0L, it.count()) }
            assertEquals(LegacyReadSummary(0, 0), readAll(tableOnly).second)
        }
    }

    @Test
    fun ten_thousand_rows_are_read_in_id_order() {
        TempDir().use { dir ->
            val db = dir.root / "note.db"
            LegacyFixtures.create(db, LegacyFixtures.manyRows(10_000)).close()
            val (rows, summary) = readAll(db)
            assertEquals(LegacyReadSummary(10_000, 0), summary)
            assertEquals((1L..10_000L).toList(), rows.map { it.id })
        }
    }

    @Test
    fun rows_only_in_the_wal_are_read_from_the_copied_wal() {
        TempDir().use { dir ->
            val original = dir.root / "databases" / "note.db"
            // 옛 앱이 연결을 닫지 않은 채로 둔 상태: 줄이 아직 WAL에만 있다.
            val open = LegacyFixtures.create(original, LegacyFixtures.manyRows(50), wal = true)
            val copy = dir.root / "legacy-snapshot"
            dir.fs.createDirectories(copy)
            for (name in listOf("note.db", "note.db-wal", "note.db-shm")) {
                if (dir.fs.exists(original.parent!! / name)) dir.fs.copy(original.parent!! / name, copy / name)
            }
            assertTrue((dir.fs.metadata(original.parent!! / "note.db-wal").size ?: 0) > 0)
            assertEquals(50, readAll(copy / "note.db").first.size)
            open.close()
        }
    }

    @Test
    fun broken_pages_are_skipped_and_counted() {
        TempDir().use { dir ->
            val db = dir.root / "note.db"
            LegacyFixtures.create(db, LegacyFixtures.manyRows(2_000, contentLength = 300)).close()
            LegacyFixtures.corruptMiddlePages(db)
            LegacyDbReader.open(db.toString()).use { assertFalse(it.integrityOk()) }

            val (rows, summary) = readAll(db)

            assertTrue(summary.read in 1 until 2_000, "읽은 줄 ${summary.read}")
            assertTrue(summary.unreadable > 0)
            assertEquals(2_000, summary.read + summary.unreadable)
            assertEquals(rows.map { it.id }, rows.map { it.id }.sorted())
        }
    }

    @Test
    fun text_in_a_number_column_is_not_read_as_zero() {
        TempDir().use { dir ->
            val db = dir.root / "note.db"
            LegacyFixtures.create(db, listOf(LegacyFixtures.Row("2020-01-01 10:00:00", mood = "기쁨", weather = " 3 "))).close()
            val row = readAll(db).first.single()
            assertNull(row.mood)
            assertEquals(3L, row.weather)
        }
    }
}
