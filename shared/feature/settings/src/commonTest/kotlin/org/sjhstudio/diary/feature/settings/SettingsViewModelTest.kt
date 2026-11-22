package org.sjhstudio.diary.feature.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.reminder.NotificationAllowance
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.platform.NotificationPermission
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink
import org.sjhstudio.diary.core.testing.FakeFontInstaller
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.FakePlanRepository
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

    private var granted = true
    private val notifications =
        NotificationAllowance(
            object : NotificationPermission {
                override suspend fun isGranted() = granted
            }
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
        val viewModel =
            SettingsViewModel(settings, lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = fonts, notifications = notifications)
        advanceUntilIdle()
        assertEquals(null, viewModel.look.value.fontPath)

        val path = fonts.finish(DiaryFont.BUSAN_DANDI_MYEONGJO.id)
        advanceUntilIdle()

        assertEquals(path, viewModel.look.value.fontPath)
    }

    @Test
    fun the_report_row_follows_the_migration_state() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                true
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertTrue(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun an_unreadable_migration_state_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                error("읽을 수 없음")
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertFalse(viewModel.state.value.showsMigrationReport)
    }

    @Test
    fun the_last_backup_time_comes_with_the_state() = runTest(dispatcher) {
        val at = Instant.parse("2026-11-13T10:48:00Z")
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications, lastBackupAt = flowOf(at))
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(at, viewModel.state.value.lastBackupAt)
    }

    @Test
    fun the_trash_count_and_plan_come_with_the_state() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(3), FakePlanRepository(Plan.PLUS), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.trashCount)
        assertEquals(Plan.PLUS, viewModel.state.value.plan)
    }

    /** 재설치 직후 요금제를 모르면 null. 그동안 무료 문구를 먼저 그리지 않음 */
    @Test
    fun an_unconfirmed_plan_stays_unknown() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.FREE, known = false), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertNull(viewModel.state.value.plan)
    }

    /** 저장소는 개수를 읽지 못하면 값 없이 끝남. 그래도 설정은 열려야 함 */
    @Test
    fun an_unreadable_trash_count_leaves_settings_usable() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, emptyFlow(), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertEquals(0, viewModel.state.value.trashCount)
    }

    /** 쓰기 알림을 켰어도 기기 설정에서 막으면 목록 줄이 시각 대신 꺼짐을 보임 */
    @Test
    fun the_reminder_row_says_off_when_notifications_are_blocked() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(AppSettings(reminderEnabled = true))
        val viewModel =
            SettingsViewModel(settings, lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        granted = false
        viewModel.refreshNotifications()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.reminderBlocked)

        granted = true
        viewModel.refreshNotifications()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.reminderBlocked)
    }

    /** 허용을 읽기 전에는 시각을 그리지 않음. 막아 둔 사람에게 시각이 보였다가 꺼짐으로 바뀌지 않게 */
    @Test
    fun the_reminder_row_waits_for_the_notification_setting() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(AppSettings(reminderEnabled = true))
        val viewModel =
            SettingsViewModel(settings, lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertNull(viewModel.state.value.reminderBlocked)
    }

    @Test
    fun formatting_waits_for_the_plan_then_settles() = runTest {
        val plans = FakePlanRepository(Plan.FREE, known = false)
        val viewModel =
            SettingsViewModel(
                FakeSettingsRepository(),
                lock,
                flowOf(0),
                plans,
                { false },
                fonts = FakeFontInstaller(),
                notifications = notifications
            )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        // 끝내 모르면 지금 값(무료)으로 정해 Plus 안내를 볼 수 있음
        assertEquals(false, viewModel.state.value.canFormat)

        plans.plan.value = Plan.PLUS
        plans.known.value = true
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.canFormat)
    }

    /** 기기에 남긴 요금제는 설정을 다 읽기 전 첫 화면부터 씀. Plus 카드가 늦게 끼어들어 줄이 밀리지 않게 */
    @Test
    fun the_saved_plan_is_there_from_the_first_frame() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)

        assertFalse(viewModel.state.value.loaded)
        assertEquals(Plan.FREE, viewModel.state.value.plan)
    }

    /** 스펙 010: 테마 색 저장과 통계(허용한 이름만, 직접 고른 색 값은 보내지 않음) */
    @Test
    fun theme_colors_are_saved_and_custom_follows_the_plan() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val sent = mutableListOf<Map<String, String>>()
        val sink =
            object : TelemetrySink {
                override fun log(event: String, params: Map<String, String>) {
                    sent += params
                }

                override fun setCustomKey(key: String, value: String) = Unit
            }
        val viewModel =
            SettingsViewModel(settings, lock, flowOf(0), FakePlanRepository(Plan.PLUS), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications, telemetry = Telemetry(sink, strict = true))
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.canCustomTheme)

        viewModel.setThemeColor(ThemeColor.SEA)
        advanceUntilIdle()
        assertEquals(ThemeColor.SEA, settings.settings.value.theme.color)

        viewModel.setCustomThemeColor(0xC2185B)
        advanceUntilIdle()
        assertEquals(ThemeColor.CUSTOM, settings.settings.value.theme.color)
        assertEquals(0xC2185B, settings.settings.value.theme.customRgb)
        // 고른 색 값은 어떤 꼴로도 보내지 않고 이름만
        assertEquals(
            listOf(mapOf("name" to "theme_color", "value" to "sea"), mapOf("name" to "theme_color", "value" to "custom")),
            sent.filter { it["name"] == "theme_color" }
        )
    }

    @Test
    fun free_plan_cannot_pick_a_custom_theme_color() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.AD_FREE), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.canCustomTheme)
    }

    /** 테마 화면은 휴지통 개수·백업 상태처럼 늦게 오는 값을 기다리지 않음 */
    @Test
    fun theme_screen_does_not_wait_for_unrelated_reads() = runTest(dispatcher) {
        val never = flow<Int> { awaitCancellation() }
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, never, FakePlanRepository(Plan.PLUS), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        backgroundScope.launch { viewModel.theme.collect {} }
        advanceUntilIdle()

        assertEquals(true, viewModel.theme.value?.canCustom)
    }

    /** 이미 아는 요금제를 들어올 때 모름으로 되돌리지 않음. 직접 고른 색이 잠깐 라벤더로 보이지 않게 */
    @Test
    fun a_known_plan_is_never_shown_as_unknown() = runTest(dispatcher) {
        val viewModel =
            SettingsViewModel(FakeSettingsRepository(), lock, flowOf(0), FakePlanRepository(Plan.PLUS), {
                false
            }, fonts = FakeFontInstaller(), notifications = notifications)
        val seen = mutableListOf<Boolean?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.theme.collect { state -> state?.let { seen += it.canCustom } }
        }
        advanceUntilIdle()

        assertEquals(listOf<Boolean?>(true), seen.distinct())
    }
}
