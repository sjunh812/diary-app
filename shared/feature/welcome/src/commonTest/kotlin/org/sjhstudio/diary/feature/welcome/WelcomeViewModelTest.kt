package org.sjhstudio.diary.feature.welcome

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.DefaultWelcomeRepository
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/** 온보딩의 백업 연결. 실패해도 막지 않고, 말없이 넘어가지도 않는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val backup = FakeBackupRepository()

    private fun viewModel() = WelcomeViewModel(
        DefaultWelcomeRepository(WelcomeStore(InMemoryDataStore(emptyPreferences()))) { null },
        backup,
        FakeSettingsRepository()
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun a_failed_connection_moves_on_and_is_told() = runTest(dispatcher) {
        backup.connectResult = ConnectResult.Failed(BackupFailure.OFFLINE)
        val viewModel = viewModel()
        var moved = false

        viewModel.connect { moved = true }
        advanceUntilIdle()

        assertEquals(true, moved)
        assertEquals(false, viewModel.connecting.value)
        assertEquals(BackupFailure.OFFLINE, backup.connectFailure.value)
    }

    @Test
    fun cancelling_moves_on_without_a_word() = runTest(dispatcher) {
        backup.connectResult = ConnectResult.Cancelled
        val viewModel = viewModel()
        var moved = false

        viewModel.connect { moved = true }
        advanceUntilIdle()

        assertEquals(true, moved)
        assertNull(backup.connectFailure.value)
    }

    /** 연결이 예외를 던져도 온보딩이 멈추거나 꺼지지 않는다. */
    @Test
    fun a_crash_while_connecting_still_moves_on() = runTest(dispatcher) {
        backup.connectCrash = true
        val viewModel = viewModel()
        var moved = false

        viewModel.connect { moved = true }
        advanceUntilIdle()

        assertEquals(true, moved)
        assertEquals(false, viewModel.connecting.value)
    }
}
