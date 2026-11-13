package org.sjhstudio.diary.feature.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 이전 상태는 파일을 읽어서 확인
// 화면을 만들 때 막고 읽지 않고, 읽지 못해도 설정은 그대로 씀
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val lock =
        DefaultLockRepository(
            LockStore(InMemoryDataStore<Preferences>(emptyPreferences())),
            FakePinKeyStore(),
            keyWork = Dispatchers.Unconfined
        )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun the_report_row_follows_the_migration_state() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), flowOf(Plan.FREE), { true })
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertTrue(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun an_unreadable_migration_state_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), flowOf(Plan.FREE), { error("읽을 수 없음") })
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertFalse(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun the_trash_count_and_plan_come_with_the_state() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), lock, flowOf(3), flowOf(Plan.PLUS), { false })
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.trashCount)
        assertEquals(Plan.PLUS, viewModel.state.value.plan)
    }

    /** 저장소는 개수를 읽지 못하면 값 없이 끝남. 그래도 설정은 열려야 함 */
    @Test
    fun an_unreadable_trash_count_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), lock, emptyFlow(), flowOf(Plan.FREE), { false })
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertEquals(0, viewModel.state.value.trashCount)
    }
}
