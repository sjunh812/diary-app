package org.sjhstudio.diary.feature.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
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
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakeFontInstaller
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

    /** 앱을 켜며 받기가 끝나면 설정이 그대로여도 본문 글꼴이 받은 파일을 씀 */
    @Test
    fun the_body_font_follows_a_download_that_finishes_later() = runTest(dispatcher) {
        val fonts = FakeFontInstaller()
        val settings = FakeSettingsRepository(AppSettings(font = DiaryFont.BUSAN_DANDI_MYEONGJO))
        val viewModel = SettingsViewModel(settings, lock, flowOf(0), flowOf(Plan.FREE), { false }, fonts = fonts)
        advanceUntilIdle()
        assertEquals(null, viewModel.look.value.fontPath)

        val path = fonts.finish(DiaryFont.BUSAN_DANDI_MYEONGJO.id)
        advanceUntilIdle()

        assertEquals(path, viewModel.look.value.fontPath)
    }

    @Test
    fun the_report_row_follows_the_migration_state() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), flowOf(Plan.FREE), { true }, fonts = FakeFontInstaller())
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertTrue(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun an_unreadable_migration_state_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), flowOf(Plan.FREE), {
                error("읽을 수 없음")
            }, fonts = FakeFontInstaller())
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertFalse(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun the_last_backup_time_comes_with_the_state() = runTest(dispatcher) {
        val at = Instant.parse("2026-11-13T10:48:00Z")
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), flowOf(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), lastBackupAt = flowOf(at))
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(at, viewModel.state.value.lastBackupAt)
    }

    @Test
    fun the_trash_count_and_plan_come_with_the_state() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(3), flowOf(Plan.PLUS), { false }, fonts = FakeFontInstaller())
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.trashCount)
        assertEquals(Plan.PLUS, viewModel.state.value.plan)
    }

    /** 저장소는 개수를 읽지 못하면 값 없이 끝남. 그래도 설정은 열려야 함 */
    @Test
    fun an_unreadable_trash_count_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, emptyFlow(), flowOf(Plan.FREE), { false }, fonts = FakeFontInstaller())
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertEquals(0, viewModel.state.value.trashCount)
    }
}
