package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// Plus가 끝나면 직접 고른 색을 지우고(스펙 010 US6) Plus 기분 테마를 기본으로(스펙 013 US4)
class PlusLookResetTest {
    private val custom = ThemeChoice(colorId = "custom", customHex = "#C2185B")

    @Test
    fun ending_plus_clears_the_custom_color_and_goes_back_to_lavender() = runTest {
        val settings = FakeSettingsRepository(AppSettings(theme = custom))
        val plans = FakePlanRepository(Plan.PLUS)
        backgroundScope.launch { PlusLookReset(settings, plans).watch() }
        runCurrent()
        assertEquals(custom, settings.settings.value.theme)

        plans.plan.value = Plan.FREE
        runCurrent()

        assertEquals(ThemeChoice(colorId = "lavender"), settings.settings.value.theme)
    }

    /** 저장에 한 번 실패해도 감시는 이어 가 다음 설정 변경에 다시 지움 */
    @Test
    fun a_failed_write_is_tried_again_on_the_next_change() = runTest {
        val real = FakeSettingsRepository(AppSettings(theme = custom))
        var fails = 1
        val settings =
            object : SettingsRepository by real {
                override suspend fun updateTheme(transform: (ThemeChoice) -> ThemeChoice) {
                    if (fails-- > 0) error("공간 부족")
                    real.updateTheme(transform)
                }
            }
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.FREE)).watch() }
        runCurrent()
        assertEquals(custom, real.settings.value.theme)

        real.setThemeMode(ThemeMode.DARK)
        runCurrent()

        assertEquals(ThemeChoice(colorId = "lavender"), real.settings.value.theme)
    }

    @Test
    fun a_preset_stays_but_the_remembered_custom_color_goes() = runTest {
        val settings = FakeSettingsRepository(AppSettings(theme = ThemeChoice(colorId = "sea", customHex = "#C2185B")))
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.AD_FREE)).watch() }
        runCurrent()

        assertEquals(ThemeChoice(colorId = "sea"), settings.settings.value.theme)
    }

    /** 무료일 때 직접 고른 색이 든 백업을 복원해도 남지 않음 */
    @Test
    fun custom_color_restored_while_free_is_cleared() = runTest {
        val settings = FakeSettingsRepository(AppSettings(theme = ThemeChoice(colorId = "rose")))
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.FREE)).watch() }
        runCurrent()

        settings.updateTheme { custom }
        runCurrent()

        assertEquals(ThemeChoice(colorId = "lavender"), settings.settings.value.theme)
    }

    /** 모르는 이름은 다시 고를 때까지 그대로 */
    @Test
    fun unknown_color_name_stays_while_free() = runTest {
        val unknown = ThemeChoice(colorId = "mauve")
        val settings = FakeSettingsRepository(AppSettings(theme = unknown))
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.FREE)).watch() }
        runCurrent()

        assertEquals(unknown, settings.settings.value.theme)
    }

    /** 재설치 직후 요금제를 모를 때는 산 사람의 색을 지우지 않음 */
    @Test
    fun unknown_plan_keeps_the_custom_color() = runTest {
        val settings = FakeSettingsRepository(AppSettings(theme = custom))
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.FREE, known = false)).watch() }
        runCurrent()

        assertEquals(custom, settings.settings.value.theme)
    }

    @Test
    fun ending_plus_turns_a_plus_mood_theme_back_to_basic_and_keeps_the_clay_look() = runTest {
        val clay = MoodLook().withTheme(MoodTheme.CLAY).withClay(ClayHair.CURLY, ClaySkin.SKIN4, glasses = true)
        val settings = FakeSettingsRepository(AppSettings(moodLook = clay))
        val plans = FakePlanRepository(Plan.PLUS)
        backgroundScope.launch { PlusLookReset(settings, plans).watch() }
        runCurrent()
        assertEquals(clay, settings.settings.value.moodLook)

        plans.plan.value = Plan.AD_FREE
        runCurrent()

        assertEquals(clay.copy(themeId = "basic"), settings.settings.value.moodLook)
    }

    /** 무료 테마는 그대로. 무료 기기에 Plus 테마가 든 백업을 복원하면 기본으로 */
    @Test
    fun free_mood_themes_stay_and_restored_plus_themes_go_back() = runTest {
        val settings = FakeSettingsRepository(AppSettings(moodLook = MoodLook(themeId = "dot")))
        backgroundScope.launch { PlusLookReset(settings, FakePlanRepository(Plan.FREE)).watch() }
        runCurrent()
        assertEquals("dot", settings.settings.value.moodLook.themeId)

        settings.updateMoodLook { it.withTheme(MoodTheme.VEGGIE) }
        runCurrent()

        assertEquals("basic", settings.settings.value.moodLook.themeId)
    }
}
