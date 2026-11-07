package org.sjhstudio.diary.feature.calendar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat

/** 달력의 그날 일기 줄은 누르면 열리고 길게 누르면 메뉴를 부른다. */
@OptIn(ExperimentalTestApi::class)
class DayPanelUiTest {
    private val day = LocalDate(2026, 11, 1)
    private val text = "달력 아래 일기"

    @Test
    fun long_press_asks_for_the_menu_and_does_not_open() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val menu = mutableListOf<String>()
        setContent {
            DiaryTheme(darkTheme = false) {
                DayPanel(
                    day,
                    listOf(fakeEntryRow("a", day, text, false)),
                    rememberDiaryDateFormat(),
                    DiaryTheme.typography.body,
                    false,
                    { opened += it },
                    { menu += it },
                    {}
                )
            }
        }

        onNode(hasText(text, substring = true)).performTouchInput { longClick() }
        waitForIdle()

        assertEquals(listOf("a"), menu)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun tap_opens_and_does_not_ask_for_the_menu() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val menu = mutableListOf<String>()
        setContent {
            DiaryTheme(darkTheme = false) {
                DayPanel(
                    day,
                    listOf(fakeEntryRow("a", day, text, false)),
                    rememberDiaryDateFormat(),
                    DiaryTheme.typography.body,
                    false,
                    { opened += it },
                    { menu += it },
                    {}
                )
            }
        }

        onNode(hasText(text, substring = true)).performClick()
        waitForIdle()

        assertEquals(listOf("a"), opened)
        assertTrue(menu.isEmpty())
    }
}
