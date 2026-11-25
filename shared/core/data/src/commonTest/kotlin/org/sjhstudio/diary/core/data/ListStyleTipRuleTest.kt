package org.sjhstudio.diary.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.testing.FakeContextTips
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 목록 모양을 직접 바꿨거나 복원으로 바뀌었으면 목록 보기 팁을 다시 띄우지 않음
class ListStyleTipRuleTest {
    @Test
    fun starting_with_the_saved_style_keeps_the_tip() = runTest {
        val settings = FakeSettingsRepository(AppSettings(list = ListPreferences(listStyle = ListStyle.CARD)))
        val tips = FakeContextTips()
        backgroundScope.launch { ListStyleTipRule(settings, tips).watch() }
        runCurrent()

        assertEquals(emptyList(), tips.done)
    }

    /** 복원으로 카드형이 된 뒤 설정에서 피드로 바꾼 경우. 지금 피드여도 팁이 다시 뜨지 않음 */
    @Test
    fun changing_the_style_ends_the_tip_even_back_to_feed() = runTest {
        val settings = FakeSettingsRepository()
        val tips = FakeContextTips()
        backgroundScope.launch { ListStyleTipRule(settings, tips).watch() }
        runCurrent()

        settings.setListStyle(ListStyle.CARD)
        runCurrent()
        settings.setListStyle(ListStyle.FEED)
        runCurrent()

        assertEquals(ListStyle.FEED, settings.settings.value.list.listStyle)
        assertEquals(true, ContextTip.HOME_LIST_STYLE in tips.done)
    }

    /** 피드를 담은 백업을 피드인 기기에 복원하면 아무것도 바뀌지 않아 팁이 남음 */
    @Test
    fun restoring_the_same_style_keeps_the_tip() = runTest {
        val settings = FakeSettingsRepository()
        val tips = FakeContextTips()
        backgroundScope.launch { ListStyleTipRule(settings, tips).watch() }
        runCurrent()

        settings.setListStyle(ListStyle.FEED)
        runCurrent()

        assertEquals(emptyList(), tips.done)
    }
}
