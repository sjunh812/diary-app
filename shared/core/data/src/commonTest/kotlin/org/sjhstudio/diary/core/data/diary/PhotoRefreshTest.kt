package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.testing.TestClock

private val SHORT = 50.milliseconds
private val WAIT = 10.seconds
private const val BURST = 300

/** 복원이 사진을 받는 동안 보던 화면이 따라오는지(받은 사진이 대기로 남던 문제) */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoRefreshTest {
    private val db = memoryDatabase()
    private val clock = TestClock(T0)
    private val entries = DefaultEntryRepository(db, MEDIA_DIR, clock, photoRefreshEvery = SHORT)
    private val trash = DefaultTrashRepository(db, MEDIA_DIR, clock, photoRefreshEvery = SHORT)

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun waiting(id: String, entryId: String, order: Int = 0) =
        MediaEntity(id, entryId, order, MediaStatus.PENDING.id, "$id.jpg", id, 1, 1, 1, null, 1)

    private suspend fun received(id: String) = db.backupDao().settlePending(id, "$id.jpg", MediaStatus.NORMAL.id)

    /** 화면 대신 쪽을 받아 지금 보이는 줄을 알려 줌 */
    private class Rows(context: CoroutineContext) : PagingDataPresenter<EntryRow>(context) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<EntryRow>) = Unit

        val rows get() = snapshot().items
    }

    private fun CoroutineScope.watch(flow: Flow<PagingData<EntryRow>>): Pair<Rows, Job> {
        val rows = Rows(coroutineContext)
        return rows to launch { flow.collectLatest(rows::collectFrom) }
    }

    private suspend fun waitUntil(check: suspend () -> Boolean) = withTimeout(WAIT) {
        while (!check()) delay(SHORT / 5)
    }

    @Test
    fun detail_shows_a_received_photo() = runTest {
        db.seed(entry("a"))
        db.mediaDao().insertAll(listOf(waiting("p", "a")))

        withContext(Dispatchers.Default) {
            val shown = launch {
                val photo = withTimeout(WAIT) { entries.entry("a").first { it?.photos?.single()?.pending == false } }
                assertEquals("$MEDIA_DIR/p.jpg", photo?.photos?.single()?.path)
            }
            assertEquals(true, entries.entry("a").first()?.photos?.single()?.pending)
            received("p")
            shown.join()
        }
    }

    /** 사진 하나가 다른 일기의 것이면 상세를 다시 그릴 일이 없음 */
    @Test
    fun detail_ignores_photos_of_other_entries() = runTest {
        db.seed(entry("a"), entry("b"))
        db.mediaDao().insertAll(listOf(waiting("mine", "a"), waiting("other", "b")))

        withContext(Dispatchers.Default) {
            val seen = mutableListOf<Boolean?>()
            val watching = launch { entries.entry("a").collect { seen += it?.photos?.single()?.pending } }
            waitUntil { seen.isNotEmpty() }
            received("other")
            received("mine")
            waitUntil { seen.last() == false }
            watching.cancelAndJoin()
            assertEquals(listOf<Boolean?>(true, false), seen)
        }
    }

    @Test
    fun timeline_shows_a_received_photo() = runTest {
        db.seed(entry("a"))
        db.mediaDao().insertAll(listOf(waiting("p", "a")))

        withContext(Dispatchers.Default) {
            val (shown, job) = watch(entries.timeline(favoritesOnly = false))
            waitUntil { shown.rows.size == 1 }
            assertEquals(listOf<String?>(EntryRow.PHOTO_PENDING), shown.rows.single().photos)

            received("p")

            waitUntil { shown.rows.single().photos.single() != EntryRow.PHOTO_PENDING }
            job.cancel()
        }
    }

    @Test
    fun favorites_and_search_show_a_received_photo() = runTest {
        db.seed(entry("a", favorite = true, text = "바다"))
        db.mediaDao().insertAll(listOf(waiting("p", "a")))

        withContext(Dispatchers.Default) {
            val (favorites, first) = watch(entries.timeline(favoritesOnly = true))
            val (found, second) = watch(entries.search(SearchFilter(query = "바다")))
            waitUntil { favorites.rows.size == 1 && found.rows.size == 1 }

            received("p")

            waitUntil {
                favorites.rows.single().photos.single() != EntryRow.PHOTO_PENDING &&
                    found.rows.single().photos.single() != EntryRow.PHOTO_PENDING
            }
            first.cancel()
            second.cancel()
        }
    }

    @Test
    fun day_and_trash_show_a_received_photo() = runTest {
        db.seed(entry("a"), entry("b", deletedAt = T0))
        db.mediaDao().insertAll(listOf(waiting("p", "a"), waiting("q", "b")))

        withContext(Dispatchers.Default) {
            val day = launch {
                withTimeout(WAIT) { entries.day(LocalDate(2026, 10, 31)).first { it.single().photos.single() != EntryRow.PHOTO_PENDING } }
            }
            val trashed = launch { withTimeout(WAIT) { trash.items().first { it.single().row.photos.single() != EntryRow.PHOTO_PENDING } } }
            assertEquals(listOf<String?>(EntryRow.PHOTO_PENDING), entries.day(LocalDate(2026, 10, 31)).first().single().photos)
            received("p")
            received("q")
            day.join()
            trashed.join()
        }
    }

    /** 수백 장을 받아도 목록을 다시 읽으라는 알림은 시간 간격 수만큼만 옴 */
    @Test
    fun many_photos_refresh_only_at_intervals() = runTest {
        db.seed(entry("a"))
        db.mediaDao().insertAll(List(BURST) { waiting("p$it", "a", it) })
        val every = 200.milliseconds

        withContext(Dispatchers.Default) {
            var refreshes = 0
            val watching = launch { db.mediaDao().photoStatusChanges(every).collect { refreshes++ } }
            delay(SHORT) // 처음 값을 읽고 나서 바꿈
            val elapsed = timed { repeat(BURST) { received("p$it") } }
            waitUntil { db.backupDao().pendingFiles().isEmpty() }
            delay(every * 3) // 묶어 둔 마지막 알림까지 기다림
            watching.cancelAndJoin()

            assertTrue(refreshes >= 1, "받은 사진을 알리지 않았다")
            val most = (elapsed / every).toInt() + 2
            assertTrue(refreshes <= most, "$elapsed 동안 $refreshes 번, 많아야 $most 번이어야 한다")
            assertTrue(refreshes < BURST / 10, "사진마다 알렸다: $refreshes 번")
        }
    }

    /** 다른 까닭으로 사진 표가 바뀌어도 상태 수가 그대로면 목록을 다시 읽지 않음 */
    @Test
    fun saved_photos_do_not_refresh_the_list() = runTest {
        db.seed(entry("a"))

        withContext(Dispatchers.Default) {
            var refreshes = 0
            val watching = launch { db.mediaDao().photoStatusChanges(SHORT).collect { refreshes++ } }
            delay(SHORT)
            db.seedPhotos(photo("new", "a", 0))
            delay(SHORT * 4)
            watching.cancelAndJoin()

            assertEquals(0, refreshes)
        }
    }

    private suspend fun timed(block: suspend () -> Unit): Duration {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow()
    }
}
