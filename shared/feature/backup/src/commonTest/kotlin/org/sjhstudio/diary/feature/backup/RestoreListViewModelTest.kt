package org.sjhstudio.diary.feature.backup

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

// 복원 목록은 지난번 것을 먼저 보이고 드라이브 답으로 바꿈
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun entry(name: String) = BackupEntry(
        snapshotName = name,
        deviceId = "d",
        deviceName = "폰",
        platform = "android",
        createdAt = Instant.fromEpochSeconds(1_800_000_000),
        entryCount = 1,
        photoCount = 0,
        photosNotInBackup = 0,
        appVersion = "2.0.0",
        state = BackupEntryState.Ready
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun the_saved_list_shows_before_the_drive_answers() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        val gate = CompletableDeferred<Unit>()
        repository.saved = listOf(entry("지난번"))
        repository.entries = listOf(entry("지금"))
        repository.backupsGate = gate

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()
        assertEquals(RestoreListUi.Loaded(listOf(entry("지난번"))), vm.state.value)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(RestoreListUi.Loaded(listOf(entry("지금"))), vm.state.value)
    }

    /** 드라이브에 닿지 못하면 지난 목록을 그대로 두지 않고 사실대로 알림 */
    @Test
    fun a_failed_answer_replaces_the_saved_list() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.saved = listOf(entry("지난번"))
        repository.entries = null

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        assertEquals(RestoreListUi.Failed(BackupFailure.OFFLINE), vm.state.value)
    }

    @Test
    fun no_saved_list_waits_as_before() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.backupsGate = CompletableDeferred()

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        assertEquals(RestoreListUi.Loading, vm.state.value)
    }

    /** 계정 때문에 실패하면 인터넷 탓이 아니라 그 까닭을 화면에 넘김 */
    @Test
    fun the_failure_kind_reaches_the_screen() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.entries = null
        repository.failure = BackupFailure.AUTH

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        assertEquals(RestoreListUi.Failed(BackupFailure.AUTH), vm.state.value)
    }

    /** 다시 시도하는 동안 실패 화면을 둔 채 버튼만 기다림. 로딩 화면으로 바뀌면 빨리 실패할 때 깜빡임 */
    @Test
    fun retrying_keeps_the_failed_screen_until_the_answer() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.saved = listOf(entry("지난번"))
        repository.entries = null
        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.backupsGate = gate
        vm.load()
        advanceUntilIdle()
        // 지난 목록으로 잠깐 바뀌지도 않음
        assertEquals(RestoreListUi.Failed(BackupFailure.OFFLINE, retrying = true), vm.state.value)

        repository.entries = listOf(entry("지금"))
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(RestoreListUi.Loaded(listOf(entry("지금"))), vm.state.value)
    }
}
