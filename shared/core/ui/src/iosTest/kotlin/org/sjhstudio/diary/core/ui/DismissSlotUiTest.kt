package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 카드를 닫으면 흐려지는 동안 자리를 지키고, 그 뒤 접히며 아래 줄이 따라 올라옴
@OptIn(ExperimentalTestApi::class)
class DismissSlotUiTest {
    @Test
    fun a_closed_card_fades_then_folds() = runComposeUiTest {
        var shown by mutableStateOf(true)
        setContent {
            DiaryTheme(darkTheme = false) {
                Column {
                    DiaryDismissSlot(shown) { if (it) Box(Modifier.size(100.dp).testTag(CARD)) }
                    Box(Modifier.size(10.dp).testTag(BELOW))
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        // 처음 그릴 때는 움직이지 않음
        assertEquals(100.dp, onNodeWithTag(BELOW).getBoundsInRoot().top, "처음")

        shown = false
        mainClock.advanceTimeBy(100)
        // 흐려지는 동안 아래 줄은 그대로
        assertEquals(100.dp, onNodeWithTag(BELOW).getBoundsInRoot().top, "흐려지는 중")

        mainClock.advanceTimeBy(150)
        val folding = onNodeWithTag(BELOW).getBoundsInRoot().top
        assertTrue(folding < 100.dp && folding > 0.dp, "접히는 중 $folding")

        mainClock.advanceTimeBy(400)
        assertEquals(0.dp, onNodeWithTag(BELOW).getBoundsInRoot().top)
    }

    /** 같은 카드의 글만 바뀌면 흐려지거나 접히지 않고 바로 바뀜 */
    @Test
    fun the_same_card_updates_in_place() = runComposeUiTest {
        var height by mutableIntStateOf(100)
        setContent {
            DiaryTheme(darkTheme = false) {
                Column {
                    DiaryDismissSlot(height, key = { "card" }) { Box(Modifier.size(it.dp)) }
                    Box(Modifier.size(10.dp).testTag(BELOW))
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false

        height = 60
        mainClock.advanceTimeBy(50)

        assertEquals(60.dp, onNodeWithTag(BELOW).getBoundsInRoot().top)
    }

    /** 읽는 중에서 처음 정해진 카드는 화면과 함께 바로 보임 */
    @Test
    fun the_first_card_after_reading_shows_at_once() = runComposeUiTest {
        var reading by mutableStateOf(true)
        setContent {
            DiaryTheme(darkTheme = false) {
                Column {
                    DiaryDismissSlot(reading, known = { !it }) { if (!it) Box(Modifier.size(100.dp)) }
                    Box(Modifier.size(10.dp).testTag(BELOW))
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false

        reading = false
        mainClock.advanceTimeBy(50)

        assertEquals(100.dp, onNodeWithTag(BELOW).getBoundsInRoot().top)
    }

    private companion object {
        const val CARD = "card"
        const val BELOW = "below"
    }
}
