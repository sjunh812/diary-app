package org.sjhstudio.diary.feature.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 기분 테마 화면(스펙 013 US3·US4)
@OptIn(ExperimentalCoroutinesApi::class)
class MoodThemeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun picking_a_theme_and_clay_look_is_saved() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = MoodThemeViewModel(settings, FakePlanRepository(Plan.PLUS))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.setTheme(MoodTheme.CLAY)
        vm.setHair(ClayHair.CURLY)
        vm.setSkin(ClaySkin.SKIN5)
        vm.setGlasses(true)
        advanceUntilIdle()

        assertEquals(
            MoodLook(themeId = "clay", clayHair = "curly", claySkin = "skin5", clayGlasses = true),
            settings.settings.value.moodLook
        )
        assertEquals(true, vm.state.value?.canPlus)
    }

    /** 요금제를 모르면 잠금 표시를 하지 않게 null. 무료면 거짓 */
    @Test
    fun plus_is_unknown_until_the_plan_is_known() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.FREE, known = false)
        val vm = MoodThemeViewModel(FakeSettingsRepository(), plans)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        dispatcher.scheduler.runCurrent()
        assertEquals(null, vm.state.value?.canPlus)

        plans.known.value = true
        advanceUntilIdle()
        assertEquals(false, vm.state.value?.canPlus)
    }

    /** 모르는 테마 id는 기본으로 보이고, 다른 것을 바꿔도 지우지 않음 */
    @Test
    fun an_unknown_theme_survives_other_changes() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(AppSettings(moodLook = MoodLook(themeId = "galaxy")))
        val vm = MoodThemeViewModel(settings, FakePlanRepository(Plan.PLUS))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.setSticker(true)
        advanceUntilIdle()

        assertEquals("galaxy", settings.settings.value.moodLook.themeId)
        assertEquals(MoodTheme.BASIC, vm.state.value?.look?.theme)
    }
}
