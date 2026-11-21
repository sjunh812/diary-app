package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.model.AppVersion
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.ReleaseNote
import org.sjhstudio.diary.core.model.StoreCheck
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.testing.FakeStoreUpdates
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.TestClock

class AppUpdateRepositoryTest {
    private val clock = TestClock()
    private val remote = mutableMapOf<String, String>()
    private val dataStore = InMemoryDataStore(emptyPreferences())

    private fun TestScope.repo(store: FakeStoreUpdates, scope: CoroutineScope = backgroundScope) = AppUpdateRepository(
        store = store,
        remote = { remote[it].orEmpty() },
        prompts = PromptStore(dataStore),
        versionName = "2.0.1",
        scope = CoroutineScope(scope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        clock = clock
    )

    @Test
    fun versions_compare_by_number_parts() {
        assertEquals(true, AppVersion.isNewer("2.0.10", "2.0.9"))
        assertEquals(true, AppVersion.isNewer("2.1", "2.0.9"))
        assertEquals(false, AppVersion.isNewer("2.0.1", "2.0.1"))
        assertEquals(false, AppVersion.isNewer("2.0", "2.0.0"))
        assertEquals(true, AppVersion.isNewer("45", "43"))
        assertEquals(false, AppVersion.isNewer("", "2.0.1"))
    }

    @Test
    fun card_shows_when_store_has_newer_version() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.homeCard.first())
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    @Test
    fun nothing_when_store_has_no_update_or_fails_first_time() = runTest {
        val store = FakeStoreUpdates()
        val repo = repo(store)
        repo.refresh()
        assertNull(repo.homeCard.first())
        store.answer = null
        repo.refresh()
        assertNull(repo.homeCard.first())
        assertEquals(UpdateNotice.None, repo.notice.value)
    }

    @Test
    fun failed_check_keeps_last_known_state() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.answer = null
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    @Test
    fun later_hides_that_version_until_a_newer_one() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        assertNull(repo.homeCard.first())
        // 설정 줄은 그대로
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
        store.answer = StoreCheck.Available("46")
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.homeCard.first())
    }

    @Test
    fun required_version_cannot_be_hidden() = runTest {
        remote[AppUpdateRepository.KEY_MIN_VERSION] = "2.0.2"
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        assertEquals(UpdateNotice.Available(required = true, note = null), repo.homeCard.first())
    }

    @Test
    fun minimum_at_or_below_this_version_is_not_required() = runTest {
        remote[AppUpdateRepository.KEY_MIN_VERSION] = "2.0.1"
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    @Test
    fun note_only_for_a_version_newer_than_this_app() = runTest {
        remote[AppUpdateRepository.KEY_NOTE_VERSION] = "2.0.2"
        remote["update_note_ko"] = "결제 뒤에도 광고가 보이던 문제를 고쳤어요."
        remote["update_note_en"] = "Fixed ads showing after purchase."
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        val note = (repo.notice.value as UpdateNotice.Available).note
        assertEquals("결제 뒤에도 광고가 보이던 문제를 고쳤어요.", note?.textFor("ko"))
        assertEquals("Fixed ads showing after purchase.", note?.textFor("ja"))
        remote[AppUpdateRepository.KEY_NOTE_VERSION] = "2.0.1"
        repo.refresh()
        assertNull((repo.notice.value as UpdateNotice.Available).note)
    }

    @Test
    fun note_without_any_text_is_no_note() {
        assertNull(ReleaseNote(emptyMap()).textFor("ko"))
    }

    @Test
    fun download_progress_and_done_show_even_after_later() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        store.installs.emit(InstallStep.Downloading(0.42f))
        assertEquals(UpdateNotice.Downloading(0.42f), repo.homeCard.first())
        store.installs.emit(InstallStep.Downloaded)
        assertEquals(UpdateNotice.Downloaded, repo.homeCard.first())
        repo.restart()
        assertEquals(1, store.restarts)
    }

    @Test
    fun later_after_download_hides_only_this_run() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        assertNull(repo.homeCard.first())
        assertEquals(UpdateNotice.Downloaded, repo.notice.value)
        val reopened = repo(store)
        reopened.refresh()
        assertEquals(UpdateNotice.Downloaded, reopened.homeCard.first())
    }

    @Test
    fun cancel_keeps_the_card_as_it_was() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.start()
        store.installs.emit(InstallStep.Canceled)
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.homeCard.first())
    }

    @Test
    fun failure_shows_until_retry_and_survives_refresh() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Downloading(0.1f))
        store.installs.emit(InstallStep.Failed)
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
        repo.start()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
        assertEquals(1, store.starts)
    }

    @Test
    fun reopening_while_downloaded_shows_restart() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Downloaded, repo.homeCard.first())
    }

    @Test
    fun ios_asks_the_store_at_most_once_a_day() = runTest {
        val store = FakeStoreUpdates(installed = "2.0.1", checkEvery = 1.days).apply { answer = StoreCheck.Available("2.0.2") }
        val repo = repo(store)
        repo.refresh()
        clock.advance(5.hours)
        store.answer = StoreCheck.None
        repo.refresh()
        assertEquals(1, store.checks)
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
        clock.advance(20.hours)
        repo.refresh()
        assertEquals(2, store.checks)
        assertEquals(UpdateNotice.None, repo.notice.value)
    }

    @Test
    fun ios_offline_uses_the_last_known_version_after_a_day() = runTest {
        val store = FakeStoreUpdates(installed = "2.0.1", checkEvery = 1.days).apply { answer = StoreCheck.Available("2.0.2") }
        repo(store).refresh()
        clock.advance(2.days)
        store.answer = null
        val reopened = repo(store)
        reopened.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), reopened.notice.value)
    }

    @Test
    fun ios_cached_version_is_forgotten_after_updating() = runTest {
        val before = FakeStoreUpdates(installed = "2.0.1", checkEvery = 1.days).apply { answer = StoreCheck.Available("2.0.2") }
        repo(before).refresh()
        val updated = FakeStoreUpdates(installed = "2.0.2", checkEvery = 1.days)
        val repo = repo(updated)
        repo.refresh()
        assertEquals(0, updated.checks)
        assertEquals(UpdateNotice.None, repo.notice.value)
    }

    @Test
    fun store_page_and_start_failures_are_quiet() = runTest {
        val store = FakeStoreUpdates().apply { accepts = false }
        val repo = repo(store)
        assertEquals(false, repo.start())
        repo.openStorePage()
        assertEquals(1, store.storePages)
    }
}
