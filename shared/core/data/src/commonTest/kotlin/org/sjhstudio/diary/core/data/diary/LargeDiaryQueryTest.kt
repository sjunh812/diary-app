package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import okio.Path.Companion.toPath
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter

/**
 * 일기 1만 편, 사진 5천 장에서 첫 화면과 찾기가 1초 안에 끝나는지 본다(SC-003).
 * 메모리 DB라 실제 기기보다 빠르므로, 넘치면 기기에서는 더 느리다고 읽는다.
 */
class LargeDiaryQueryTest {
    private lateinit var db: DiaryDatabase
    private lateinit var entries: DefaultEntryRepository

    @BeforeTest
    fun setUp() {
        db = memoryDatabase()
        entries = DefaultEntryRepository(db, "/media".toPath())
    }

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun seedTenThousand() {
        val start = LocalDate(2026, 10, 31)
        val body = "퇴근길에 한강까지 걸었다. 바람이 차가웠는데 그래서 더 좋았다. ".repeat(HUNDREDS)
        val rows =
            List(ENTRIES) { index ->
                entry(
                    id = "e$index",
                    date = start.minus(DatePeriod(days = index % DAYS)).toString(),
                    text = if (index % RARE == 0) "$body 바닷가" else body,
                    mood = Mood.entries[index % Mood.entries.size]
                )
            }
        rows.chunked(CHUNK).forEach { db.entryDao().insertAll(it.map { row -> row.asEntity() }) }
        val media = List(ENTRIES / 2) { index -> photo("p$index", "e${index * 2}", 0) }
        media.chunked(CHUNK).forEach { db.mediaDao().insertAll(it.map { row -> row.asEntity() }) }
    }

    private suspend fun assertFast(name: String, limitMillis: Long = LIMIT_MILLIS, block: suspend () -> Unit) {
        val mark = TimeSource.Monotonic.markNow()
        block()
        val elapsed = mark.elapsedNow()
        println("$name: $elapsed")
        assertTrue(elapsed.inWholeMilliseconds < limitMillis, "$name 걸린 시간 $elapsed")
    }

    @Test
    fun first_screen_and_search_finish_within_a_second() = runTest {
        seedTenThousand()

        assertFast("첫 화면") {
            val page = db.entryDao().visibleEntries().load(PagingSource.LoadParams.Refresh(null, PAGE, false))
            val rows = (page as PagingSource.LoadResult.Page).data
            db.mediaDao().forRows(rows.map { it.id })
        }
        assertFast("드문 글자 찾기") { entries.searchAll(SearchFilter(query = "바닷가")) }
        // 1만 편이 모두 걸리는 가장 나쁜 경우다.
        assertFast("흔한 글자 찾기", WORST_QUERY_BUDGET_MILLIS) { entries.searchAll(SearchFilter(query = "한강")) }
        assertFast("달력 한 달") { entries.month(YearMonth(2026, 10)).first() }
    }

    private companion object {
        const val ENTRIES = 10_000
        const val DAYS = 3650
        const val HUNDREDS = 3
        const val RARE = 500
        const val CHUNK = 500
        const val PAGE = 60
        const val LIMIT_MILLIS = 1_000L
    }
}
