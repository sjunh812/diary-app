package org.sjhstudio.diary.feature.backup

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.model.AccountChange
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

// 백업 설정의 구글 계정 줄(스펙 011)
@OptIn(ExperimentalCoroutinesApi::class)
class BackupAccountChangeTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 계정 선택 창이 떠 있는 동안 화면을 돌려 받는 쪽이 잠깐 없어도 결과가 남음 */
    @Test
    fun a_result_waits_for_the_screen() = runTest(dispatcher) {
        val backup = FakeBackupRepository().apply { accountChange = AccountChange.CHANGED }
        val viewModel = BackupViewModel(backup, FakeRestoreRepository())

        viewModel.changeAccount()
        advanceUntilIdle()
        val seen = mutableListOf<AccountChange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.accountChanges.collect { seen += it } }

        assertEquals(listOf(AccountChange.CHANGED), seen)
    }

    @Test
    fun the_result_reaches_the_screen_once() = runTest(dispatcher) {
        val backup = FakeBackupRepository().apply { accountChange = AccountChange.BUSY }
        val viewModel = BackupViewModel(backup, FakeRestoreRepository())
        val seen = mutableListOf<AccountChange>()
        // 바로 구독해야 한 번 오는 결과를 놓치지 않음
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.accountChanges.collect { seen += it } }

        viewModel.changeAccount()
        advanceUntilIdle()

        assertEquals(listOf(AccountChange.BUSY), seen)
        assertFalse(viewModel.connecting.value)
    }

    /** 계정 선택 창이 떠 있는 동안 또 눌러도 창을 두 번 띄우지 않음 */
    @Test
    fun a_second_tap_while_picking_asks_once() = runTest(dispatcher) {
        val picked = CompletableDeferred<AccountChange>()
        val fake = FakeBackupRepository()
        var asked = 0
        val backup =
            object : BackupRepository by fake {
                override suspend fun changeAccount(): AccountChange {
                    asked++
                    return picked.await()
                }
            }
        val viewModel = BackupViewModel(backup, FakeRestoreRepository())

        viewModel.changeAccount()
        advanceUntilIdle()
        viewModel.changeAccount()
        advanceUntilIdle()
        picked.complete(AccountChange.CHANGED)
        advanceUntilIdle()

        assertEquals(1, asked)
        assertFalse(viewModel.connecting.value)
    }
}
