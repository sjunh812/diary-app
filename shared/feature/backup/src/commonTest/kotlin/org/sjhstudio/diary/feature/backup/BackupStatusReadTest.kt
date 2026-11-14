package org.sjhstudio.diary.feature.backup

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.IOException
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

// 홈 줄과 백업 화면은 저장해 둔 상태를 읽지 못해도 꺼짐이라고 하지 않고 다시 시도를 보임
@OptIn(ExperimentalCoroutinesApi::class)
class BackupStatusReadTest {
    private val dispatcher = StandardTestDispatcher()
    private val on = BackupStatus(BackupConnection.Connected("diary@gmail.com"), autoEnabled = true)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 처음 [failures]번은 읽다 실패하고 그다음부터 [on]을 줌 */
    private class Flaky(private val fake: FakeBackupRepository, var failures: Int, private val on: BackupStatus) :
        BackupRepository by fake {
        override val status: Flow<BackupStatus> = flow {
            if (failures > 0) {
                failures--
                throw IOException("읽을 수 없음")
            }
            emit(on)
        }
    }

    private fun viewModel(repository: BackupRepository) = BackupViewModel(repository, FakeRestoreRepository())

    @Test
    fun an_unreadable_status_is_told_and_never_shown_as_off() = runTest(dispatcher) {
        val viewModel = viewModel(Flaky(FakeBackupRepository(), failures = 1, on))
        backgroundScope.launch { viewModel.loadedStatus.collect {} }
        backgroundScope.launch { viewModel.statusFailed.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.statusFailed.value)
        assertNull(viewModel.loadedStatus.value)
    }

    @Test
    fun trying_again_reads_the_status() = runTest(dispatcher) {
        val viewModel = viewModel(Flaky(FakeBackupRepository(), failures = 1, on))
        backgroundScope.launch { viewModel.loadedStatus.collect {} }
        backgroundScope.launch { viewModel.statusFailed.collect {} }
        advanceUntilIdle()

        viewModel.retryStatus()
        advanceUntilIdle()

        assertFalse(viewModel.statusFailed.value)
        assertEquals(on, viewModel.loadedStatus.value)
    }

    @Test
    fun before_reading_nothing_is_claimed() = runTest(dispatcher) {
        val viewModel = viewModel(FakeBackupRepository(on))

        // 아직 읽지 않음
        assertNull(viewModel.loadedStatus.value)
        assertFalse(viewModel.statusFailed.value)

        backgroundScope.launch { viewModel.loadedStatus.collect {} }
        advanceUntilIdle()
        assertEquals(on, viewModel.loadedStatus.value)
    }
}
