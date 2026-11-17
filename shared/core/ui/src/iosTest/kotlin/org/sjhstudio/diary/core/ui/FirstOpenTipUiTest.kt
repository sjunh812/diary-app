package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.model.ContextTip

// 켜 둔 채 날이 바뀌면 같은 자리 팁을 다시 고름(스펙 008 FR-011)
@OptIn(ExperimentalTestApi::class)
class FirstOpenTipUiTest {
    /** 어제 다른 팁이 보여 오늘만 열리는 팁 */
    private class DayTips(var day: LocalDate) : ContextTips {
        override fun isOpen(tip: ContextTip): Boolean = day > LocalDate(2026, 11, 17)

        override suspend fun load() = Unit

        override fun done(tip: ContextTip) = Unit
    }

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun a_new_day_picks_the_tip_again() = runComposeUiTest {
        val tips = DayTips(LocalDate(2026, 11, 17))
        startKoin { modules(module { single<ContextTips> { tips } }) }
        var today by mutableStateOf(LocalDate(2026, 11, 17))
        var picked: ContextTip? = ContextTip.INSIGHT_OPEN
        setContent { picked = rememberFirstOpenTip(listOf(ContextTip.HOME_LIST_STYLE), today) }
        waitForIdle()
        assertEquals(null, picked)

        tips.day = LocalDate(2026, 11, 18)
        today = LocalDate(2026, 11, 18)
        waitForIdle()

        assertEquals(ContextTip.HOME_LIST_STYLE, picked)
    }
}
