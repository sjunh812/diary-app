package org.sjhstudio.diary.feature.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.reminder.ReminderRepository
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.ReminderScheduler
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 매일 알림(R1): 켜고 끄기, 시각 바꾸기, 알림 허용 안내(스펙 001 FR-022).
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsRepository()
    private val scheduler = RecordingScheduler()
    private var granted = false
    private val permission = object : NotificationPermission {
        override suspend fun isGranted() = granted
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ReminderSettingsViewModel(settings, ReminderRepository(settings, scheduler), permission)

    @Test
    fun turning_on_and_off_saves_and_reschedules() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.setEnabled(true)
        advanceUntilIdle()
        assertTrue(settings.settings.first().reminderEnabled)
        assertEquals(1, scheduler.scheduled.size)

        vm.setEnabled(false)
        advanceUntilIdle()
        assertFalse(settings.settings.first().reminderEnabled)
        assertEquals(1, scheduler.canceled)
    }

    @Test
    fun saving_a_time_keeps_it_on_and_reschedules() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.setTime(7 * 60 + 5)
        advanceUntilIdle()
        assertEquals(7 * 60 + 5, vm.state.value.minuteOfDay)
        assertTrue(vm.state.value.enabled)
        assertEquals(1, scheduler.scheduled.size)
    }

    /** 허용 여부를 읽기 전에는 안내를 보이지 않고, 켜 두었는데 허용되지 않았을 때만 보임. */
    @Test
    fun permission_notice_shows_only_when_on_and_not_allowed() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.setEnabled(true)
        advanceUntilIdle()
        assertNull(vm.state.value.notificationsAllowed)
        assertFalse(vm.state.value.showsPermissionNotice)

        vm.refreshPermission()
        advanceUntilIdle()
        assertTrue(vm.state.value.showsPermissionNotice)

        granted = true
        vm.refreshPermission()
        advanceUntilIdle()
        assertFalse(vm.state.value.showsPermissionNotice)
    }

    private class RecordingScheduler : ReminderScheduler {
        val scheduled = mutableListOf<Instant>()
        var canceled = 0

        override fun schedule(at: Instant) {
            scheduled += at
        }

        override fun cancel() {
            canceled++
        }
    }
}
