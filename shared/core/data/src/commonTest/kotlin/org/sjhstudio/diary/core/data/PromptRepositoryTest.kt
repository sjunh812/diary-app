package org.sjhstudio.diary.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.IOException
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class PromptRepositoryTest {
    private val start = Instant.parse("2026-11-15T01:00:00Z")
    private var now = start
    private val clock = object : Clock {
        override fun now() = now
    }
    private val written = MutableStateFlow(0)
    private var since: Instant? = null
    private var requests = 0

    /** 스토어가 정보를 줬는지. 거짓이면 받다 거둬진 것과 같음 */
    private var fetched = true
    private var dataStore: DataStore<Preferences> = InMemoryDataStore(emptyPreferences())
    private val store get() = PromptStore(dataStore)

    private fun repo() = PromptRepository(
        store = store,
        writtenSince = {
            since = it
            written
        },
        review = { stillDue ->
            (fetched && stillDue()).also { if (it) requests++ }
        },
        clock = clock,
        timeZone = { TimeZone.UTC }
    )

    @Test
    fun feedback_card_after_three_days_and_three_entries() = runTest {
        val repo = repo()
        repo.start()
        written.value = 3
        now = start + 2.days
        assertFalse(repo.feedbackCard.first())
        now = start + 3.days
        assertTrue(repo.feedbackCard.first())
        assertEquals(start, since)
    }

    @Test
    fun feedback_card_needs_three_entries() = runTest {
        val repo = repo()
        repo.start()
        now = start + 10.days
        written.value = 2
        assertFalse(repo.feedbackCard.first())
        written.value = 3
        assertTrue(repo.feedbackCard.first())
    }

    @Test
    fun first_use_is_written_once() = runTest {
        val repo = repo()
        repo.start()
        now = start + 5.days
        repo.start()
        assertEquals(start, store.current().firstUseAt)
    }

    @Test
    fun no_card_before_first_use_is_known() = runTest {
        written.value = 10
        now = start + 30.days
        assertFalse(repo().feedbackCard.first())
    }

    @Test
    fun closing_or_opening_survey_hides_card_for_good() = runTest {
        val repo = repo()
        repo.start()
        written.value = 3
        now = start + 3.days
        repo.feedbackClosed()
        assertFalse(repo.feedbackCard.first())

        dataStore = InMemoryDataStore(emptyPreferences())
        val other = repo()
        other.start()
        other.surveyOpened()
        now = start + 6.days
        assertFalse(other.feedbackCard.first())
    }

    @Test
    fun clock_moved_back_hides_card() = runTest {
        val repo = repo()
        repo.start()
        written.value = 3
        now = start - 1.days
        assertFalse(repo.feedbackCard.first())
    }

    @Test
    fun review_after_seven_days_and_five_entries_once() = runTest {
        val repo = repo()
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 6.days
        repo.askReviewIfDue()
        assertEquals(0, requests)
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals(1, requests)
        now = start + 200.days
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun review_needs_five_entries() = runTest {
        val repo = repo()
        repo.start()
        written.value = 4
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals(0, requests)
    }

    @Test
    fun no_review_on_a_day_the_card_showed() = runTest {
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        repo.feedbackShown()
        repo.feedbackClosed()
        repo.askReviewIfDue()
        assertEquals(0, requests)
        now = start + 8.days
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun no_review_while_the_card_is_open() = runTest {
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        repo.feedbackShown()
        now = start + 8.days
        repo.askReviewIfDue()
        assertEquals(0, requests)
        repo.surveyOpened()
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun card_never_shown_does_not_block_the_review() = runTest {
        // 백업 카드가 맨 위를 차지해 의견 카드가 한 번도 안 보임
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals(1, requests)
        // 리뷰를 물은 날은 카드를 미루고 다음 날 보임
        assertFalse(repo.feedbackCard.first())
        now = start + 8.days
        assertTrue(repo.feedbackCard.first())
    }

    @Test
    fun review_withdrawn_while_fetching_is_asked_again_later() = runTest {
        val repo = repo()
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        fetched = false
        repo.askReviewIfDue()
        assertEquals(0, requests)
        assertEquals(null, store.current().reviewAskedDay)
        fetched = true
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun review_day_is_written_after_showing() = runTest {
        var dayWhileShowing: String? = "아직"
        val repo =
            PromptRepository(store, { written }, { stillDue ->
                stillDue().also { if (it) dayWhileShowing = store.current().reviewAskedDay }
            }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals(null, dayWhileShowing)
        assertEquals("2026-11-22", store.current().reviewAskedDay)
    }

    @Test
    fun withdrawn_right_after_showing_still_records_the_day() = runTest {
        val repo =
            PromptRepository(store, { written }, { stillDue ->
                stillDue().also { if (it) currentCoroutineContext().cancel() }
            }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        val asking = backgroundScope.launch { repo.askReviewIfDue() }
        asking.join()
        assertEquals("2026-11-22", store.current().reviewAskedDay)
    }

    @Test
    fun broken_store_asks_nothing() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = kotlinx.coroutines.flow.flow { throw IOException("공간 부족") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("공간 부족")
        }
        dataStore = broken
        val repo = repo()
        repo.start()
        written.value = 10
        now = start + 30.days
        assertFalse(repo.feedbackCard.first())
        repo.askReviewIfDue()
        assertEquals(0, requests)
    }

    @Test
    fun card_appears_when_an_entry_is_written_after_the_days_passed() = runTest {
        val repo = repo()
        repo.start()
        written.value = 2
        now = start + 1.days
        val seen = mutableListOf<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.feedbackCard.collect { seen += it } }
        now = start + 4.days
        written.value = 3
        assertEquals(listOf(false, true), seen)
        job.cancel()
    }

    @Test
    fun card_shown_while_review_is_fetching_blocks_the_review() = runTest {
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        // 스토어 정보를 받는 사이 카드가 나옴
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow =
            PromptRepository(store, { written }, { stillDue ->
                gate.await()
                stillDue().also { if (it) requests++ }
            }, clock, { TimeZone.UTC })
        val asking = backgroundScope.launch { slow.askReviewIfDue() }
        testScheduler.advanceUntilIdle()
        repo.feedbackShown()
        gate.complete(Unit)
        asking.join()
        // 띄우기 직전 다시 보아 카드가 보인 날은 묻지 않고 기록도 남기지 않음
        assertEquals(0, requests)
        assertEquals(null, store.current().reviewAskedDay)
    }

    @Test
    fun failing_to_write_the_review_day_asks_once_in_the_process() = runTest {
        val full = InMemoryDataStore(emptyPreferences())
        dataStore = full
        val repo = repo()
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        // 읽기는 되고 요청일 쓰기만 실패. 이 프로세스 안에서는 다시 묻지 않음
        full.failWrites = true
        repo.askReviewIfDue()
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    /** 공간이 차 카드를 보인 날을 못 적어도 이 프로세스 안에서는 그날 리뷰를 묻지 않음 */
    @Test
    fun a_shown_card_that_could_not_be_written_still_blocks_the_review() = runTest {
        val full = InMemoryDataStore(emptyPreferences())
        dataStore = full
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        full.failWrites = true
        repo.feedbackShown()
        repo.askReviewIfDue()
        assertEquals(0, requests)
    }

    @Test
    fun no_card_on_the_day_the_review_shows_even_when_the_day_is_not_written() = runTest {
        // 백업 카드가 맨 위라 의견 카드를 못 본 채 리뷰가 뜨고, 공간이 차 요청일을 못 적음
        val full = InMemoryDataStore(emptyPreferences())
        dataStore = full
        val repo = repo()
        repo.start()
        written.value = 5
        now = start + 7.days
        full.failWrites = true
        repo.askReviewIfDue()
        assertEquals(1, requests)
        assertFalse(repo.feedbackCard.first())
    }

    @Test
    fun no_card_while_the_review_is_showing() = runTest {
        val showing = kotlinx.coroutines.CompletableDeferred<Unit>()
        val closed = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repo =
            PromptRepository(store, { written }, { stillDue ->
                stillDue().also {
                    showing.complete(Unit)
                    closed.await()
                }
            }, clock, { TimeZone.UTC })
        repo.start()
        written.value = 5
        now = start + 7.days
        val asking = backgroundScope.launch { repo.askReviewIfDue() }
        showing.await()
        assertFalse(repo.feedbackCard.first())
        closed.complete(Unit)
        asking.join()
    }

    @Test
    fun not_shown_after_claiming_is_asked_again_and_keeps_the_card() = runTest {
        // 판단 뒤 화면이 없어 못 띄움(Android 화면이 뒤로 감, iOS 앞 창 없음)
        var showable = false
        val repo =
            PromptRepository(store, { written }, { stillDue ->
                (stillDue() && showable).also { if (it) requests++ }
            }, clock, { TimeZone.UTC })
        repo.start()
        written.value = 5
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals(0, requests)
        assertEquals(null, store.current().reviewAskedDay)
        assertTrue(repo.feedbackCard.first())
        showable = true
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun overlapping_asks_show_once() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repo =
            PromptRepository(store, { written }, { stillDue ->
                gate.await()
                stillDue().also { if (it) requests++ }
            }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        val first = backgroundScope.launch { repo.askReviewIfDue() }
        val second = backgroundScope.launch { repo.askReviewIfDue() }
        testScheduler.advanceUntilIdle()
        gate.complete(Unit)
        first.join()
        second.join()
        assertEquals(1, requests)
    }

    @Test
    fun closing_hides_the_card_even_when_storage_is_full() = runTest {
        val full = InMemoryDataStore(emptyPreferences())
        dataStore = full
        val repo = repo()
        repo.start()
        written.value = 3
        now = start + 3.days
        assertTrue(repo.feedbackCard.first())
        full.failWrites = true
        repo.feedbackClosed()
        assertFalse(repo.feedbackCard.first())
    }

    @Test
    fun review_day_is_read_after_counting_entries() = runTest {
        val repo = PromptRepository(store, {
            kotlinx.coroutines.flow.flow {
                // 편 수를 세는 사이 자정이 지남
                now = start + 8.days
                emit(5)
            }
        }, { stillDue -> stillDue() }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        now = start + 7.days + 23.hours
        repo.askReviewIfDue()
        assertEquals("2026-11-23", store.current().reviewAskedDay)
    }
}
