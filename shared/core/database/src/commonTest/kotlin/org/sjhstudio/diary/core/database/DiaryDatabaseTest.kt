package org.sjhstudio.diary.core.database

import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

// 번들 드라이버 메모리 DB로 표의 제약과 색인을 봄(스펙 001 T022)
// Android 호스트와 iOS 시뮬레이터에서 같은 시험이 돎
class DiaryDatabaseTest {
    private lateinit var db: DiaryDatabase

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(DiaryDatabaseConstructor::initialize).configured().build()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun entry_round_trips_through_the_table() = runTest {
        val entry = entry("e1", legacyId = 7).copy(mood = Mood.GOOD, weather = Weather.RAIN, placeName = "집")
        db.entryDao().insertAll(listOf(entry.asEntity()))

        assertEquals(entry, db.entryDao().get("e1")?.asModel())
    }

    @Test
    fun deleting_an_entry_deletes_its_media_rows() = runTest {
        db.entryDao().insertAll(listOf(entry("e1").asEntity(), entry("e2").asEntity()))
        db.mediaDao().insertAll(
            listOf(media("m1", "e1", 0), media("m2", "e1", 1), media("m3", "e2", 0)).map { it.asEntity() }
        )

        db.entryDao().deletePermanently("e1")

        assertEquals(emptyList(), db.mediaDao().forEntry("e1"))
        assertEquals(1, db.mediaDao().count())
    }

    @Test
    fun sort_order_is_unique_within_an_entry() = runTest {
        db.entryDao().insertAll(listOf(entry("e1").asEntity(), entry("e2").asEntity()))
        db.mediaDao().insertAll(listOf(media("m1", "e1", 0).asEntity(), media("m2", "e2", 0).asEntity()))

        assertFailsWith<SQLiteException> {
            db.mediaDao().insertAll(listOf(media("m3", "e1", 0).asEntity()))
        }
    }

    @Test
    fun legacy_id_is_unique_but_many_entries_can_have_none() = runTest {
        db.entryDao().insertAll(listOf(entry("e1", legacyId = 1), entry("e2"), entry("e3")).map { it.asEntity() })

        assertFailsWith<SQLiteException> {
            db.entryDao().insertAll(listOf(entry("e4", legacyId = 1).asEntity()))
        }
        assertEquals(3, db.entryDao().countAll())
    }

    @Test
    fun missing_media_keeps_its_row_without_file_columns() = runTest {
        db.entryDao().insertAll(listOf(entry("e1").asEntity()))
        val missing =
            media("m1", "e1", 0).copy(
                status = MediaStatus.MISSING,
                fileName = null,
                sha256 = null,
                width = null,
                height = null,
                byteSize = null,
                legacyPath = "/storage/emulated/0/Pictures/a.jpg"
            )
        db.mediaDao().insertAll(listOf(missing.asEntity()))

        val stored = db.mediaDao().forEntry("e1").single().asModel()
        assertEquals(MediaStatus.MISSING, stored.status)
        assertNull(stored.fileName)
        assertEquals("/storage/emulated/0/Pictures/a.jpg", stored.legacyPath)
    }

    @Test
    fun list_query_uses_the_list_index_without_sorting() = runTest {
        val plan =
            db.useReaderConnection { connection ->
                connection.usePrepared("EXPLAIN QUERY PLAN ${EntryDao.VISIBLE_ENTRIES}") { statement ->
                    buildList { while (statement.step()) add(statement.getText(3)) }
                }
            }.joinToString("\n")

        assertTrue("index_entry_list" in plan, plan)
        assertFalse("TEMP B-TREE" in plan, plan)
    }

    private fun entry(id: String, legacyId: Int? = null) = Entry(
        id = id,
        date = LocalDate(2026, 11, 1),
        time = LocalTime(21, 30),
        bodyHtml = "<p>오늘</p>",
        plainText = "오늘",
        mood = null,
        weather = null,
        placeName = null,
        isFavorite = false,
        createdAt = Instant.fromEpochMilliseconds(1_000),
        updatedAt = Instant.fromEpochMilliseconds(2_000),
        legacyId = legacyId
    )

    private fun media(id: String, entryId: String, sortOrder: Int) = Media(
        id = id,
        entryId = entryId,
        sortOrder = sortOrder,
        status = MediaStatus.NORMAL,
        fileName = "$id.jpg",
        sha256 = id,
        width = 100,
        height = 100,
        byteSize = 10,
        createdAt = Instant.fromEpochMilliseconds(1_000)
    )
}
