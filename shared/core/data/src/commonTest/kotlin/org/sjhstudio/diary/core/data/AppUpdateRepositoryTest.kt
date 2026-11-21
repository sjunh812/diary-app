package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    /** 앱 범위는 실제로 한 박자 늦게 돌아 상태가 늦게 따라와도, 카드는 처음부터 새 버전을 알림. 빈 칸이 먼저 그려지면 팁이 스침 */
    @Test
    fun the_home_card_never_reads_none_before_the_new_version() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = AppUpdateRepository(store, { "" }, PromptStore(dataStore), "2.0.1", backgroundScope, clock)
        val seen = mutableListOf<UpdateNotice?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.homeCard.collect { seen += it } }
        repo.refresh()
        advanceUntilIdle()
        assertEquals(listOf<UpdateNotice?>(UpdateNotice.Available(required = false, note = null)), seen)
    }

    /** 앱을 다시 열어 "받는 중"으로 안 뒤 Play 알림에서 받기를 취소하면 남은 진행을 보이지 않음 */
    @Test
    fun canceling_a_download_found_on_reopen_does_not_leave_it_downloading() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.4f) }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Downloading(0.4f), repo.notice.value)

        store.answer = StoreCheck.Available("45")
        store.installs.emit(InstallStep.Canceled)
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    /** 받다 실패하면 앱을 다시 열어 안 "받는 중"보다 실패가 보임 */
    @Test
    fun a_failed_download_found_on_reopen_shows_the_failure() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.4f) }
        val repo = repo(store)
        repo.refresh()

        store.answer = StoreCheck.Available("45")
        store.installs.emit(InstallStep.Failed)
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
    }

    /** Play가 알림 없이 받기를 멈춰도 앱으로 돌아와 다시 물으면 "받는 중"이 풀림 */
    @Test
    fun a_download_that_stops_silently_recovers_on_return() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Downloading(0.3f))
        assertEquals(UpdateNotice.Downloading(0.3f), repo.notice.value)

        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    /** 마지막 "받음" 알림을 놓쳐도 돌아와 다시 물으면 [다시 시작하기]가 나옴 */
    @Test
    fun a_missed_downloaded_event_is_found_on_return() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Downloading(0.9f))

        store.answer = StoreCheck.Downloaded("45")
        repo.refresh()
        assertEquals(UpdateNotice.Downloaded(), repo.notice.value)
    }

    /** 받은 뒤 설치가 실패하면 다시 받을 수 있는 카드가 되고 [나중에]로 닫힘 */
    @Test
    fun a_failed_install_after_download_can_be_dismissed() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded("45") }
        val repo = repo(store)
        repo.refresh()

        store.installs.emit(InstallStep.Failed)
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
        repo.dismiss()
        assertNull(repo.homeCard.first())
    }

    /** 새 버전이 없는데 실패 알림이 와도 카드가 생기지 않음 */
    @Test
    fun a_stray_failure_without_an_update_shows_nothing() = runTest {
        val store = FakeStoreUpdates()
        val repo = repo(store)
        repo.refresh()

        store.installs.emit(InstallStep.Failed)
        assertEquals(UpdateNotice.None, repo.notice.value)
        assertNull(repo.homeCard.first())
    }

    /** 취소한 뒤 다시 묻기까지 실패해도(오프라인) 물어 둔 "받는 중"이 남지 않음 */
    @Test
    fun canceling_offline_does_not_leave_it_downloading() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.4f) }
        val repo = repo(store)
        repo.refresh()

        store.answer = null
        store.installs.emit(InstallStep.Canceled)
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    /** 실패한 뒤 다시 묻기까지 실패해도 [나중에]로 닫을 수 있음 */
    @Test
    fun a_failed_download_offline_can_still_be_dismissed() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.4f) }
        val repo = repo(store)
        repo.refresh()

        store.answer = null
        store.installs.emit(InstallStep.Failed)
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
        repo.dismiss()
        assertNull(repo.homeCard.first())
    }

    /** 닫은 버전을 설정에서 받다가 실패해도 홈 카드로 실패를 알림. 사용자가 시작한 일 */
    @Test
    fun a_failed_download_of_a_dismissed_version_still_shows() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        assertNull(repo.homeCard.first())

        store.installs.emit(InstallStep.Downloading(0.4f))
        store.installs.emit(InstallStep.Failed)
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.homeCard.first())
    }

    /** 묻는 사이 "받음" 알림이 먼저 오면 늦게 온 "받는 중" 답으로 되돌리지 않음 */
    @Test
    fun a_stale_downloading_answer_does_not_undo_downloaded() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()

        store.answer = StoreCheck.Downloading("45", 0.95f)
        store.duringCheck = { store.installs.emit(InstallStep.Downloaded) }
        repo.refresh()
        assertEquals(UpdateNotice.Downloaded(), repo.notice.value)
    }

    /** 묻는 사이 실패 알림이 먼저 오면 늦게 온 "받는 중" 답으로 실패를 지우지 않음 */
    @Test
    fun a_stale_downloading_answer_does_not_hide_a_failure() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()

        store.answer = StoreCheck.Downloading("45", 0.4f)
        store.duringCheck = { store.installs.emit(InstallStep.Failed) }
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
    }

    /** 스토어가 끝내 답하지 않아도 처음 확인은 끝나 홈 맨 위 칸이 계속 기다리지 않음 */
    @Test
    fun a_store_that_never_answers_still_finishes_the_first_check() = runTest {
        val store = FakeStoreUpdates().apply { hangs = true }
        val repo = repo(store)
        repo.refresh()
        // 처음 확인을 마쳐야 카드 값이 나옴
        assertNull(repo.homeCard.first())
    }

    /** 받은 뒤 [나중에]로 숨겨도 같은 실행에서 다시 받기를 시작해 끝내면 [다시 시작하기]를 알림 */
    @Test
    fun a_new_download_after_later_shows_downloaded_again() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Downloaded)
        repo.dismiss()
        assertNull(repo.homeCard.first())

        store.installs.emit(InstallStep.Failed)
        repo.start()
        store.installs.emit(InstallStep.Downloaded)
        assertEquals(UpdateNotice.Downloaded(), repo.homeCard.first())
    }

    /** 지난 버전의 받기 실패가 새로 나온 버전에 옮겨 붙지 않음 */
    @Test
    fun an_old_failure_does_not_stick_to_a_newer_version() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Failed)

        store.answer = StoreCheck.Available("46")
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    /** 꼭 필요한 업데이트는 받은 뒤에도 [나중에]로 숨길 수 없음 */
    @Test
    fun a_required_update_stays_after_download() = runTest {
        remote[AppUpdateRepository.KEY_MIN_VERSION] = "9.0"
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded("45") }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Downloaded(required = true), repo.homeCard.first())

        repo.dismiss()
        assertEquals(UpdateNotice.Downloaded(required = true), repo.homeCard.first())
    }

    /** 실패 카드를 [나중에]로 닫을 때 한순간도 일반 새 버전 카드로 바뀌지 않음 */
    @Test
    fun dismissing_a_failed_card_goes_straight_to_nothing() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        store.installs.emit(InstallStep.Failed)
        val seen = mutableListOf<UpdateNotice?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.homeCard.collect { seen += it } }

        repo.dismiss()
        assertEquals(listOf<UpdateNotice?>(UpdateNotice.Available(required = false, note = null, failed = true), null), seen)
    }

    /** 닫은 버전의 실패 카드에서 [업데이트]를 눌러도 받기가 시작될 때까지 카드가 사라지지 않음 */
    @Test
    fun retrying_a_failed_dismissed_version_keeps_the_card() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Available("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        store.installs.emit(InstallStep.Downloading(0.1f))
        store.installs.emit(InstallStep.Failed)

        repo.start()
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.homeCard.first())
        store.installs.emit(InstallStep.Downloading(0.2f))
        assertEquals(UpdateNotice.Downloading(0.2f), repo.homeCard.first())
    }

    /** 묻는 사이 취소 알림이 먼저 오면 늦게 온 "받는 중" 답으로 되돌리지 않음 */
    @Test
    fun a_stale_downloading_answer_does_not_undo_a_cancel() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.4f) }
        val repo = repo(store)
        repo.refresh()

        store.duringCheck = { store.installs.emit(InstallStep.Canceled) }
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null), repo.notice.value)
    }

    /** 묻는 사이 실패 알림이 오면 늦게 온 "받음" 답으로 실패를 지우지 않음 */
    @Test
    fun a_stale_downloaded_answer_does_not_hide_a_failure() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded("45") }
        val repo = repo(store)
        repo.refresh()

        store.duringCheck = { store.installs.emit(InstallStep.Failed) }
        repo.refresh()
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
    }

    /** 앱을 다시 열어 안 "받는 중"에서 실패 알림이 와도 다른 카드가 한 번도 나가지 않음 */
    @Test
    fun a_failure_after_reopen_goes_straight_to_the_failure_card() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.6f) }
        val repo = repo(store)
        repo.refresh()
        val seen = mutableListOf<UpdateNotice?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.homeCard.collect { seen += it } }

        store.installs.emit(InstallStep.Failed)
        assertEquals(
            listOf<UpdateNotice?>(UpdateNotice.Downloading(0.6f), UpdateNotice.Available(required = false, note = null, failed = true)),
            seen
        )
    }

    /** 앱을 새로 켠 첫 확인 사이 알림이 와도 그 답이 알려 준 버전은 배워, 뒤이은 실패를 카드로 알림 */
    @Test
    fun the_first_check_still_learns_the_version_when_an_event_arrives() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloading("45", 0.5f) }
        val repo = repo(store)
        store.duringCheck = { store.installs.emit(InstallStep.Downloading(0.55f)) }
        repo.refresh()
        assertEquals(UpdateNotice.Downloading(0.55f), repo.notice.value)

        store.installs.emit(InstallStep.Failed)
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.homeCard.first())
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
        assertEquals(UpdateNotice.Downloaded(), repo.homeCard.first())
        repo.restart()
        assertEquals(1, store.restarts)
    }

    @Test
    fun later_after_download_hides_only_this_run() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded("45") }
        val repo = repo(store)
        repo.refresh()
        repo.dismiss()
        assertNull(repo.homeCard.first())
        assertEquals(UpdateNotice.Downloaded(), repo.notice.value)
        val reopened = repo(store)
        reopened.refresh()
        assertEquals(UpdateNotice.Downloaded(), reopened.homeCard.first())
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
        // 받기가 실제로 시작될 때 실패 표시가 바뀜
        assertEquals(UpdateNotice.Available(required = false, note = null, failed = true), repo.notice.value)
        store.installs.emit(InstallStep.Downloading(0.05f))
        assertEquals(UpdateNotice.Downloading(0.05f), repo.notice.value)
        assertEquals(1, store.starts)
    }

    @Test
    fun reopening_while_downloaded_shows_restart() = runTest {
        val store = FakeStoreUpdates().apply { answer = StoreCheck.Downloaded("45") }
        val repo = repo(store)
        repo.refresh()
        assertEquals(UpdateNotice.Downloaded(), repo.homeCard.first())
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
