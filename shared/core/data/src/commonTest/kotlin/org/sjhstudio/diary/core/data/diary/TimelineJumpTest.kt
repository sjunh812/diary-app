package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.testing.TestClock

// 달을 골라 먼 곳으로 건너뛸 때 사이 쪽을 다 읽지 않는다.
// 다 읽는 동안 빈 줄만 오래 보인다(스펙 002 FR-015).
class TimelineJumpTest {
    private val db = memoryDatabase()
    private val entries = DefaultEntryRepository(db, MEDIA_DIR, TestClock(T0))

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private class Presenter : PagingDataPresenter<EntryRow>(Dispatchers.Default) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<EntryRow>) = Unit
    }

    private suspend fun seedRows() = db.seed(*Array(ROWS) { entry("e$it", time = time(it)) })

    private fun time(row: Int) = "${two(row / MINUTES)}:${two(row % MINUTES)}"

    private fun two(number: Int) = number.toString().padStart(2, '0')

    @Test
    fun a_far_jump_reads_only_around_the_destination() = runTest {
        seedRows()
        withContext(Dispatchers.Default) {
            val presenter = Presenter()
            val collecting = launch { entries.timeline(false).collectLatest(presenter::collectFrom) }

            withTimeout(WAIT) {
                while (presenter.size == 0) delay(POLL_MILLIS)
                presenter[TARGET]
                while (presenter.peek(TARGET) == null) delay(POLL_MILLIS)
            }

            val loaded = presenter.snapshot()
            collecting.cancel()
            assertTrue(loaded.placeholdersBefore > 0, "처음부터 ${loaded.placeholdersBefore}줄 비움, ${loaded.items.size}줄 읽음")
        }
    }

    /** 찾기 결과도 같은 자리를 쓴다. */
    @Test
    fun every_list_source_can_jump() {
        val timeline = EntryRowPagingSource(db.entryDao().visibleEntries()) { emptyList() }
        val favorites = EntryRowPagingSource(db.entryDao().favoriteEntries()) { emptyList() }

        assertEquals(true, timeline.jumpingSupported)
        assertEquals(true, favorites.jumpingSupported)
        assertTrue(entries.pageConfig.jumpThreshold != PagingSource.LoadResult.Page.COUNT_UNDEFINED)
    }

    private companion object {
        const val ROWS = 900
        const val TARGET = 850
        const val MINUTES = 60
        const val POLL_MILLIS = 5L
        val WAIT = 20.seconds
    }
}
