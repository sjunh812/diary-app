package org.sjhstudio.diary.feature.trash

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.feature.trash.resources.Res
import org.sjhstudio.diary.feature.trash.resources.trash_empty

// 휴지통 화면에 시험이 하나도 없었다. 잘못 누르면 일기를 영영 잃는 화면이다.
@OptIn(ExperimentalTestApi::class)
class TrashScreenUiTest {
    private val day = LocalDate(2026, 11, 1)

    private fun item(id: String, text: String) =
        TrashedEntry(fakeEntryRow(id, day, text, false), Instant.fromEpochSeconds(1_800_000_000), 12)

    @Test
    fun picking_a_line_only_opens_its_choices() = runComposeUiTest {
        val picked = mutableListOf<String>()
        var emptied = 0
        setContent {
            DiaryTheme(darkTheme = false) {
                TrashScreen(listOf(item("a", "지운 일기")), {}, { emptied++ }, { picked += it })
            }
        }

        onNode(hasText("지운 일기", substring = true)).performClick()
        waitForIdle()

        assertEquals(listOf("a"), picked)
        assertEquals(0, emptied)
    }

    @Test
    fun emptying_asks_through_the_callback_and_deletes_nothing_itself() = runComposeUiTest {
        var emptied = 0
        var action = ""
        setContent {
            DiaryTheme(darkTheme = false) {
                action = stringResource(Res.string.trash_empty)
                TrashScreen(listOf(item("a", "지운 일기")), {}, { emptied++ }, {})
            }
        }

        onNode(hasText(action)).performClick()
        waitForIdle()

        assertEquals(1, emptied)
    }

    @Test
    fun an_empty_trash_offers_no_empty_action() = runComposeUiTest {
        var action = ""
        setContent {
            DiaryTheme(darkTheme = false) {
                action = stringResource(Res.string.trash_empty)
                TrashScreen(emptyList(), {}, {}, {})
            }
        }
        waitForIdle()

        assertEquals(0, onAllNodes(hasText(action)).fetchSemanticsNodes().size)
    }
}
