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
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

// 복원 목록은 지난번 것을 먼저 보이고 드라이브 답으로 바꾼다.
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

    /** 드라이브에 닿지 못하면 지난 목록을 그대로 두지 않고 사실대로 알린다. */
    @Test
    fun a_failed_answer_replaces_the_saved_list() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.saved = listOf(entry("지난번"))
        repository.entries = null

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        assertEquals(RestoreListUi.Failed, vm.state.value)
    }

    @Test
    fun no_saved_list_waits_as_before() = runTest(dispatcher) {
        val repository = FakeRestoreRepository()
        repository.backupsGate = CompletableDeferred()

        val vm = RestoreListViewModel(repository)
        advanceUntilIdle()

        assertEquals(RestoreListUi.Loading, vm.state.value)
    }
}
