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
    private var dataStore: DataStore<Preferences> = InMemoryDataStore(emptyPreferences())
    private val store get() = PromptStore(dataStore)

    private fun repo() = PromptRepository(
        store = store,
        writtenSince = {
            since = it
            written
        },
        review = { requests++ },
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
        repo.askReviewIfDue()
        assertEquals(0, requests)
        assertTrue(repo.feedbackCard.first())
        repo.surveyOpened()
        now = start + 8.days
        repo.askReviewIfDue()
        assertEquals(1, requests)
    }

    @Test
    fun review_day_is_written_before_asking() = runTest {
        var dayWhileAsking: String? = null
        val repo = PromptRepository(store, { written }, { dayWhileAsking = store.current().reviewAskedDay }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        written.value = 5
        now = start + 7.days
        repo.askReviewIfDue()
        assertEquals("2026-11-22", dayWhileAsking)
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
    fun card_shown_while_review_is_checking_still_blocks_the_review() = runTest {
        val repo = repo()
        repo.start()
        now = start + 7.days
        // 리뷰 판단이 편 수를 기다리는 사이 카드가 나옴
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow =
            PromptRepository(store, {
                kotlinx.coroutines.flow.flow {
                    gate.await()
                    emit(5)
                }
            }, { requests++ }, clock, { TimeZone.UTC })
        slow.feedbackClosed()
        val asking = backgroundScope.launch { slow.askReviewIfDue() }
        testScheduler.advanceUntilIdle()
        val showing = backgroundScope.launch { slow.feedbackShown() }
        testScheduler.advanceUntilIdle()
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
        asking.join()
        showing.join()
        // 잠금 안에서 리뷰를 먼저 적어 한 번만 요청
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
        var dayWhileAsking: String? = null
        val repo = PromptRepository(store, {
            kotlinx.coroutines.flow.flow {
                // 편 수를 세는 사이 자정이 지남
                now = start + 8.days
                emit(5)
            }
        }, { dayWhileAsking = store.current().reviewAskedDay }, clock, { TimeZone.UTC })
        repo.start()
        repo.feedbackClosed()
        now = start + 7.days + 23.hours
        repo.askReviewIfDue()
        assertEquals("2026-11-23", dayWhileAsking)
    }
}
